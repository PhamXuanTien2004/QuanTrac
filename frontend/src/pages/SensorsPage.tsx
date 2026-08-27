import { useEffect, useState, useMemo } from 'react';
import { useSensorStore } from '../store/useSensorStore';
import { useGatewayStore } from '../store/useGatewayStore';
import { useSensorTypeStore } from '../store/useSensorTypeStore';
import { useStationStore } from '../store/useStationStore';
import { useAuthStore } from '../store/useAuthStore';
import { Plus, Trash2, Cpu, Edit2, Search, Filter, Activity } from 'lucide-react';

export default function SensorsPage() {
  const { sensors, isLoading: isLoadingSensors, error, fetchSensors, addSensor, updateSensor, removeSensor } = useSensorStore();
  const { gateways, fetchGateways } = useGatewayStore();
  const { sensorTypes, fetchSensorTypes } = useSensorTypeStore();
  const { stations, fetchStations } = useStationStore();
  const { user } = useAuthStore();
  
  const userRole = user?.role || 'ROLE_STAFF';
  const isAdmin = userRole === 'ROLE_ADMIN' || userRole === 'Admin';
  const isStaff = userRole === 'ROLE_STAFF';

  // State tìm kiếm & lọc
  const [searchKeyword, setSearchKeyword] = useState('');
  const [stationFilter, setStationFilter] = useState(''); // Admin only
  const [gatewayFilter, setGatewayFilter] = useState(''); // Chọn gateway

  // Phân trang
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  useEffect(() => {
    // Reset về trang 1 khi các bộ lọc thay đổi
    setCurrentPage(1);
  }, [searchKeyword, stationFilter, gatewayFilter]);

  const [isModalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState({ 
    id: '', sensorCode: '', name: '', gatewayCode: '', sensorTypeName: '', model: '', manufacturer: '', installationDate: '', calibrationDate: '', minValue: 0, maxValue: 100, status: 'ONLINE' as const 
  });

  useEffect(() => {
    fetchSensors();
    fetchGateways();
    fetchSensorTypes();
    if (isAdmin) {
      fetchStations();
    }
  }, [fetchSensors, fetchGateways, fetchSensorTypes, fetchStations, isAdmin]);

  const handleCreateOrUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload = {
        id: formData.id,
        sensorCode: formData.sensorCode,
        name: formData.name,
        gatewayCode: formData.gatewayCode,
        sensorTypeName: formData.sensorTypeName,
        model: formData.model,
        manufacturer: formData.manufacturer,
        installationDate: formData.installationDate ? new Date(formData.installationDate).toISOString() : null,
        calibrationDate: formData.calibrationDate ? new Date(formData.calibrationDate).toISOString() : null,
        minValue: formData.minValue,
        maxValue: formData.maxValue,
        status: formData.status
      };

      if (editingId) {
        await updateSensor(payload);
      } else {
        await addSensor(payload);
      }
      setModalOpen(false);
      resetForm();
    } catch (err) {
      // Error handled in store
    }
  };

  const handleEdit = (sensor: any) => {
    setEditingId(sensor.id);
    const gateway = gateways.find(g => g.id === sensor.gateway?.id || g.id === sensor.gatewayId);
    const type = sensorTypes.find(t => t.id === sensor.sensorType?.id || t.id === sensor.sensorTypeId);
    
    const formatDateTime = (isoString?: string) => isoString ? isoString.substring(0, 16) : '';
    
    setFormData({ 
      ...formData, 
      ...sensor, 
      gatewayCode: gateway ? gateway.code : '',
      sensorTypeName: type ? type.name : '',
      installationDate: formatDateTime(sensor.installationDate),
      calibrationDate: formatDateTime(sensor.calibrationDate)
    });
    setModalOpen(true);
  };

  const resetForm = () => {
    setEditingId(null);
    setFormData({ id: '', sensorCode: '', name: '', gatewayCode: '', sensorTypeName: '', model: '', manufacturer: '', installationDate: '', calibrationDate: '', minValue: 0, maxValue: 100, status: 'ONLINE' });
  };

  // Helper getters
  const getGateway = (sensor: any) => {
    const gwId = sensor.gateway?.id || sensor.gatewayId;
    return gateways.find(g => g.id === gwId);
  };

  const getStation = (sensor: any) => {
    const gw = getGateway(sensor);
    if (!gw) return null;
    const stId = gw.station?.id || gw.stationId;
    return stations.find(s => s.id === stId);
  };

  const getSensorTypeName = (id: string) => {
    const type = sensorTypes.find(s => s.id === id);
    return type ? type.name : id;
  };

  // Tính toán danh sách Gateways khả dụng cho người dùng hiện tại
  const availableGateways = useMemo(() => {
    if (isAdmin) {
      if (stationFilter) {
        return gateways.filter(g => (g.station?.id || g.stationId) === stationFilter);
      }
      return gateways;
    } else {
      // BẢO MẬT: Chỉ lấy Gateway thuộc về Trạm của Manager/Staff
      return gateways.filter(g => (g.station?.id || g.stationId) === user?.stationId);
    }
  }, [isAdmin, gateways, stationFilter, user?.stationId]);

  // Lọc dữ liệu hiển thị (Bảng)
  const filteredSensors = useMemo(() => {
    let result = sensors;

    // 1. Lọc cơ bản theo quyền hạn (Bảo mật cốt lõi)
    if (!isAdmin) {
      const allowedGwIds = availableGateways.map(g => g.id);
      result = result.filter(sensor => {
        const gwId = sensor.gateway?.id || sensor.gatewayId;
        return allowedGwIds.includes(gwId);
      });
    }

    // 2. Tìm kiếm bằng chữ
    if (searchKeyword) {
      const lowerKw = searchKeyword.toLowerCase();
      result = result.filter(sensor => 
        (sensor.name && sensor.name.toLowerCase().includes(lowerKw)) || 
        (sensor.sensorCode && sensor.sensorCode.toLowerCase().includes(lowerKw))
      );
    }

    // 3. Lọc theo Dropdown Trạm (Chỉ Admin)
    if (isAdmin && stationFilter) {
      result = result.filter(sensor => {
        const st = getStation(sensor);
        return st && st.id === stationFilter;
      });
    }

    // 4. Lọc theo Dropdown Gateway
    if (gatewayFilter) {
      result = result.filter(sensor => {
        const gw = getGateway(sensor);
        return gw && gw.id === gatewayFilter;
      });
    }

    return result;
  }, [sensors, isAdmin, availableGateways, searchKeyword, stationFilter, gatewayFilter, getGateway, getStation]);

  // Logic phân trang hiển thị
  const totalPages = Math.ceil(filteredSensors.length / itemsPerPage);
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentSensors = filteredSensors.slice(indexOfFirstItem, indexOfLastItem);

  // Logic hiển thị biểu đồ: Khi chọn 1 gateway cụ thể
  const selectedGateway = gateways.find(g => g.id === gatewayFilter);
  const showChart = selectedGateway && selectedGateway.status === 'ONLINE';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header Actions */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Quản lý Cảm biến (Sensors)</h2>
        {!isStaff && (
          <button 
            onClick={() => setModalOpen(true)}
            className="hover-lift"
            style={{ 
              display: 'flex', alignItems: 'center', gap: '8px', 
              backgroundColor: 'var(--primary-color)', color: 'white', 
              padding: '10px 20px', borderRadius: 'var(--radius-md)', fontWeight: 500 
            }}
          >
            <Plus size={18} /> Thêm Sensor mới
          </button>
        )}
      </div>

      {/* Toolbar: Tìm kiếm & Lọc */}
      <div className="glass-panel" style={{ padding: '16px', display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
        <div style={{ flex: 1, minWidth: '250px', position: 'relative' }}>
          <Search size={18} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input 
            type="text" 
            placeholder="Tìm kiếm theo Tên hoặc Mã cảm biến..." 
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            style={{ 
              width: '100%', padding: '10px 16px 10px 44px', 
              borderRadius: 'var(--radius-md)', border: '1px solid var(--border-glass)', 
              backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' 
            }} 
          />
        </div>
        
        {isAdmin && (
          <div style={{ minWidth: '200px', position: 'relative' }}>
            <Filter size={18} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <select 
              value={stationFilter} 
              onChange={(e) => { setStationFilter(e.target.value); setGatewayFilter(''); }}
              style={{ 
                width: '100%', padding: '10px 16px 10px 44px', 
                borderRadius: 'var(--radius-md)', border: '1px solid var(--border-glass)', 
                backgroundColor: 'rgba(0,0,0,0.2)', color: 'white',
                appearance: 'none'
              }}
            >
              <option value="">-- Tất cả Trạm --</option>
              {stations.map(st => (
                <option key={st.id} value={st.id}>{st.name}</option>
              ))}
            </select>
          </div>
        )}

        <div style={{ minWidth: '200px', position: 'relative' }}>
          <Filter size={18} style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <select 
            value={gatewayFilter} 
            onChange={(e) => setGatewayFilter(e.target.value)}
            style={{ 
              width: '100%', padding: '10px 16px 10px 44px', 
              borderRadius: 'var(--radius-md)', border: '1px solid var(--border-glass)', 
              backgroundColor: 'rgba(0,0,0,0.2)', color: 'white',
              appearance: 'none'
            }}
          >
            <option value="">-- Tất cả Gateway --</option>
            {availableGateways.map(g => (
              <option key={g.id} value={g.id}>{g.code}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Biểu đồ Động (Grafana Line Chart) - Chỉ hiển thị khi chọn 1 Gateway cụ thể */}
      {gatewayFilter && (
        <div className="glass-panel" style={{ overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border-glass)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Activity size={20} color="var(--primary-color)" />
            <h3 style={{ fontSize: '1.1rem' }}>
              Dữ liệu trực tuyến: Gateway <span style={{ color: 'var(--primary-color)' }}>{selectedGateway?.code}</span>
            </h3>
          </div>
          <div style={{ height: '350px', backgroundColor: 'rgba(0,0,0,0.2)', position: 'relative' }}>
            {showChart ? (
              <iframe 
                src={`http://localhost:3000/d-solo/adj5lfz/test?orgId=1&from=now-1h&to=now&timezone=browser&var-datasource0=ffsqx9mqb79q8a&refresh=5s&panelId=panel-1&theme=dark&kiosk=tv&var-gatewayId=${selectedGateway.id}`}
                width="100%" 
                height="100%" 
                style={{ border: 'none' }}
                title="Grafana Chart"
              ></iframe>
            ) : (
               <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--danger)' }}>
                 Thiết bị Gateway này đang Mất Kết Nối. Không có dữ liệu thời gian thực.
               </div>
            )}
          </div>
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger)', padding: '16px', borderRadius: 'var(--radius-md)', border: '1px solid rgba(239, 68, 68, 0.2)' }}>
          {error}
        </div>
      )}

      {/* Data Table */}
      <div className="glass-panel" style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-glass)', backgroundColor: 'rgba(255, 255, 255, 0.02)' }}>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Thông tin Sensor</th>
              {isAdmin && (
                <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Vị trí (Trạm)</th>
              )}
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Gateway</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Loại</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Trạng thái</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500, width: '100px' }}>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {isLoadingSensors && filteredSensors.length === 0 ? (
              <tr>
                <td colSpan={isAdmin ? 6 : 5} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Đang tải dữ liệu từ Backend...
                </td>
              </tr>
            ) : filteredSensors.length === 0 ? (
              <tr>
                <td colSpan={isAdmin ? 6 : 5} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Không tìm thấy Cảm biến nào phù hợp.
                </td>
              </tr>
            ) : (
              currentSensors.map((sensor) => {
                const gw = getGateway(sensor);
                const st = getStation(sensor);
                
                return (
                  <tr key={sensor.id} style={{ borderBottom: '1px solid var(--border-glass)', transition: 'background-color 0.2s' }} className="hover-row">
                    <td style={{ padding: '16px 24px', display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div style={{ padding: '10px', backgroundColor: 'rgba(139, 92, 246, 0.1)', borderRadius: '10px', color: '#8b5cf6' }}>
                        <Cpu size={20} />
                      </div>
                      <div>
                        <div style={{ fontWeight: 600, fontSize: '1rem', color: 'var(--text-primary)' }}>{sensor.name}</div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '4px' }}>Mã: <span style={{ fontWeight: 600, color: 'var(--primary-color)' }}>{sensor.sensorCode}</span></div>
                      </div>
                    </td>
                    
                    {isAdmin && (
                      <td style={{ padding: '16px 24px' }}>
                        {st ? (
                          <>
                            <div style={{ fontWeight: 500 }}>{st.name}</div>
                            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Mã: {st.stationCode}</div>
                          </>
                        ) : (
                          <span style={{ color: 'var(--text-muted)' }}>Chưa rõ</span>
                        )}
                      </td>
                    )}
                    
                    <td style={{ padding: '16px 24px' }}>
                      <span style={{ 
                        padding: '4px 10px', 
                        backgroundColor: 'rgba(255,255,255,0.05)', 
                        border: '1px solid var(--border-glass)',
                        borderRadius: '6px', 
                        fontSize: '0.85rem' 
                      }}>
                        {gw ? gw.code : (sensor.gateway?.id || sensor.gatewayId || 'Trống')}
                      </span>
                    </td>
                    
                    <td style={{ padding: '16px 24px', color: 'var(--text-secondary)' }}>
                      {getSensorTypeName(sensor.sensorType?.id || sensor.sensorTypeId)}
                    </td>
                    
                    <td style={{ padding: '16px 24px' }}>
                      <span style={{ 
                        padding: '4px 12px', borderRadius: '20px', fontSize: '0.75rem', fontWeight: 600,
                        backgroundColor: sensor.status === 'ONLINE' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                        color: sensor.status === 'ONLINE' ? 'var(--success)' : 'var(--danger)'
                      }}>
                        {sensor.status === 'ONLINE' ? 'Kết nối' : 'Mất kết nối'}
                      </span>
                    </td>
                    
                    <td style={{ padding: '16px 24px' }}>
                      {!isStaff && (
                        <div style={{ display: 'flex', gap: '12px' }}>
                          <button 
                            onClick={() => handleEdit(sensor)}
                            style={{ color: 'var(--text-muted)', transition: 'color 0.2s', padding: '6px' }}
                            onMouseEnter={(e) => e.currentTarget.style.color = 'var(--primary-color)'}
                            onMouseLeave={(e) => e.currentTarget.style.color = 'var(--text-muted)'}
                            title="Chỉnh sửa"
                          >
                            <Edit2 size={18} />
                          </button>
                          <button 
                            onClick={() => removeSensor(sensor.id)}
                            style={{ color: 'var(--text-muted)', transition: 'color 0.2s', padding: '6px' }}
                            onMouseEnter={(e) => e.currentTarget.style.color = 'var(--danger)'}
                            onMouseLeave={(e) => e.currentTarget.style.color = 'var(--text-muted)'}
                            title="Xóa mềm"
                          >
                            <Trash2 size={18} />
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>

        {/* Phân trang */}
        {totalPages > 0 && (
          <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', padding: '16px 24px', gap: '16px', borderTop: '1px solid var(--border-glass)' }}>
            <span style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
              Trang {currentPage} / {totalPages}
            </span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                disabled={currentPage === 1}
                style={{
                  padding: '6px 12px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: currentPage === 1 ? 'rgba(255,255,255,0.05)' : 'var(--primary-color)',
                  color: currentPage === 1 ? 'var(--text-muted)' : 'white',
                  border: 'none',
                  cursor: currentPage === 1 ? 'not-allowed' : 'pointer'
                }}
              >
                Trước
              </button>
              <button
                onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                disabled={currentPage === totalPages}
                style={{
                  padding: '6px 12px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: currentPage === totalPages ? 'rgba(255,255,255,0.05)' : 'var(--primary-color)',
                  color: currentPage === totalPages ? 'var(--text-muted)' : 'white',
                  border: 'none',
                  cursor: currentPage === totalPages ? 'not-allowed' : 'pointer'
                }}
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Create / Edit Modal (Khong thay doi logic) */}
      {isModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh',
          backgroundColor: 'rgba(0, 0, 0, 0.5)', backdropFilter: 'blur(4px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50
        }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: '700px', padding: '32px', backgroundColor: 'var(--bg-surface)', maxHeight: '90vh', overflowY: 'auto' }}>
            <h3 style={{ fontSize: '1.25rem', marginBottom: '24px' }}>
              {editingId ? 'Cập nhật Sensor' : 'Thêm Sensor mới'}
            </h3>
            <form onSubmit={handleCreateOrUpdate} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              
              <div style={{ gridColumn: '1 / -1', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Mã Sensor (Bắt buộc)</label>
                  <input required value={formData.sensorCode} onChange={e => setFormData({...formData, sensorCode: e.target.value})} 
                    style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                    placeholder="VD: SN-01" disabled={!!editingId} />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Tên Sensor (Bắt buộc)</label>
                  <input required value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} 
                    style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                    placeholder="VD: Cảm biến bụi PM2.5" />
                </div>
              </div>
              
              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Thuộc Gateway (Mã)</label>
                <select required value={formData.gatewayCode} onChange={e => setFormData({...formData, gatewayCode: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                >
                  <option value="">-- Chọn Gateway --</option>
                  {gateways.map(g => (
                    <option key={g.id} value={g.code}>{g.code} ({g.model || 'Unknown'})</option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Loại Sensor (Tên)</label>
                <select required value={formData.sensorTypeName} onChange={e => setFormData({...formData, sensorTypeName: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                >
                  <option value="">-- Chọn Loại Sensor --</option>
                  {sensorTypes.map(st => (
                    <option key={st.id} value={st.name}>{st.name} ({st.unit})</option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Model</label>
                <input value={formData.model} onChange={e => setFormData({...formData, model: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                  placeholder="VD: DHT22" />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Nhà sản xuất</label>
                <input value={formData.manufacturer} onChange={e => setFormData({...formData, manufacturer: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                  placeholder="VD: Adafruit" />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Ngày lắp đặt</label>
                <input type="datetime-local" value={formData.installationDate} onChange={e => setFormData({...formData, installationDate: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Ngày hiệu chuẩn</label>
                <input type="datetime-local" value={formData.calibrationDate} onChange={e => setFormData({...formData, calibrationDate: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} />
              </div>



              <div style={{ gridColumn: '1 / -1' }}>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Trạng thái</label>
                <select required value={formData.status} onChange={e => setFormData({...formData, status: e.target.value as 'ONLINE'|'OFFLINE'})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                >
                  <option value="ONLINE">ONLINE (Kết nối)</option>
                  <option value="OFFLINE">OFFLINE (Mất kết nối)</option>
                </select>
              </div>

              <div style={{ gridColumn: '1 / -1', display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
                <button type="button" onClick={() => { setModalOpen(false); resetForm(); }} style={{ padding: '10px 20px', color: 'var(--text-secondary)', fontWeight: 500 }}>
                  Hủy
                </button>
                <button type="submit" disabled={isLoadingSensors} style={{ padding: '10px 20px', backgroundColor: 'var(--primary-color)', color: 'white', borderRadius: 'var(--radius-md)', fontWeight: 500 }}>
                  {isLoadingSensors ? 'Đang lưu...' : (editingId ? 'Cập nhật' : 'Thêm mới')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
      
      <style>{`
        .hover-row:hover { background-color: rgba(255,255,255,0.02); }
      `}</style>
    </div>
  );
}
