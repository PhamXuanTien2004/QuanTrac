import { useEffect, useState } from 'react';
import { useStationStore } from '../store/useStationStore';
import { useAuthStore } from '../store/useAuthStore';
import { Plus, Trash2, MapPin, Activity, Eye, Edit2 } from 'lucide-react';

export default function StationsPage() {
  const { stations, isLoading, error, fetchStations, addStation, updateStation, removeStation } = useStationStore();
  const { user } = useAuthStore();
  const userRole = user?.role || 'ROLE_STAFF';
  const isStaff = userRole === 'ROLE_STAFF';

  const [isModalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState({ 
    id: '', stationCode: '', name: '', description: '', address: '', latitude: 0, longitude: 0, installationDate: '', status: 'ONLINE' as const 
  });

  useEffect(() => {
    fetchStations();
  }, [fetchStations]);

  const handleCreateOrUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload = {
        id: formData.id,
        stationCode: formData.stationCode,
        name: formData.name,
        description: formData.description,
        address: formData.address,
        latitude: formData.latitude,
        longitude: formData.longitude,
        installationDate: formData.installationDate,
        status: formData.status
      };

      if (editingId) {
        await updateStation(payload);
      } else {
        await addStation(payload);
      }
      setModalOpen(false);
      resetForm();
    } catch (err) {
      // Error handled in store
    }
  };

  const handleEdit = (station: any) => {
    setEditingId(station.id);
    setFormData({ ...formData, ...station });
    setModalOpen(true);
  };

  const resetForm = () => {
    setEditingId(null);
    setFormData({ id: '', stationCode: '', name: '', description: '', address: '', latitude: 0, longitude: 0, installationDate: '', status: 'ONLINE' });
  };


  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header Actions */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Quản lý Trạm Quan trắc</h2>
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
            <Plus size={18} /> Thêm Trạm mới
          </button>
        )}
      </div>

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
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Tên Trạm</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Vị trí</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Tọa độ (Lat, Lng)</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Trạng thái</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500, width: '100px' }}>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {isLoading && stations.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Đang tải dữ liệu từ Backend...
                </td>
              </tr>
            ) : stations.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Chưa có dữ liệu Trạm Quan trắc nào.
                </td>
              </tr>
            ) : (
              stations.map((station) => (
                <tr key={station.id} style={{ borderBottom: '1px solid var(--border-glass)', transition: 'background-color 0.2s' }} className="hover-row">
                  <td style={{ padding: '16px 24px', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <div style={{ padding: '8px', backgroundColor: 'rgba(56, 189, 248, 0.1)', borderRadius: '8px', color: '#38bdf8' }}>
                      <MapPin size={18} />
                    </div>
                    {station.name}
                  </td>
                  <td style={{ padding: '16px 24px', color: 'var(--text-secondary)' }}>
                    {station.address}
                  </td>
                  <td style={{ padding: '16px 24px', color: 'var(--text-secondary)' }}>
                    {station.latitude}, {station.longitude}
                  </td>
                  <td style={{ padding: '16px 24px' }}>
                    <span style={{ 
                      padding: '4px 12px', 
                      borderRadius: '99px', 
                      fontSize: '0.75rem', 
                      fontWeight: 600,
                      backgroundColor: station.status === 'ONLINE' ? 'rgba(34, 197, 94, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                      color: station.status === 'ONLINE' ? 'var(--success)' : 'var(--danger)',
                      border: `1px solid ${station.status === 'ONLINE' ? 'rgba(34, 197, 94, 0.2)' : 'rgba(239, 68, 68, 0.2)'}`
                    }}>
                      {station.status === 'ONLINE' ? 'Hoạt động' : 'Bảo trì'}
                    </span>
                  </td>
                  <td style={{ padding: '16px 24px' }}>
                    {!isStaff && (
                      <div style={{ display: 'flex', gap: '12px' }}>
                        <button 
                          onClick={() => handleEdit(station)}
                          style={{ color: 'var(--text-muted)', transition: 'color 0.2s' }}
                          onMouseEnter={(e) => e.currentTarget.style.color = 'var(--primary-color)'}
                          onMouseLeave={(e) => e.currentTarget.style.color = 'var(--text-muted)'}
                          title="Chỉnh sửa"
                        >
                          <Edit2 size={18} />
                        </button>
                        <button 
                          onClick={() => removeStation(station.id)}
                          style={{ color: 'var(--text-muted)', transition: 'color 0.2s' }}
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
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Create / Edit Modal */}
      {isModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh',
          backgroundColor: 'rgba(0, 0, 0, 0.5)', backdropFilter: 'blur(4px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50
        }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: '600px', padding: '32px', backgroundColor: 'var(--bg-surface)', maxHeight: '90vh', overflowY: 'auto' }}>
            <h3 style={{ fontSize: '1.25rem', marginBottom: '24px' }}>
              {editingId ? 'Cập nhật Trạm Quan trắc' : 'Thêm Trạm Quan trắc'}
            </h3>
            <form onSubmit={handleCreateOrUpdate} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              
              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Mã Trạm (Code)</label>
                <input required value={formData.stationCode} onChange={e => setFormData({...formData, stationCode: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                  placeholder="VD: STATION_HN_01" 
                  disabled={!!editingId} />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Tên Trạm</label>
                <input required value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                  placeholder="VD: Trạm Khí Thải A" />
              </div>
              
              <div style={{ gridColumn: '1 / -1' }}>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Địa chỉ (Address)</label>
                <input required value={formData.address} onChange={e => setFormData({...formData, address: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                  placeholder="VD: Khu công nghiệp Bắc Thăng Long" />
              </div>

              <div style={{ gridColumn: '1 / -1' }}>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Mô tả thêm (Description)</label>
                <textarea value={formData.description} onChange={e => setFormData({...formData, description: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                  placeholder="Thông tin thêm về trạm..." rows={3} />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Vĩ độ (Lat)</label>
                <input type="number" step="any" required value={formData.latitude} onChange={e => setFormData({...formData, latitude: parseFloat(e.target.value)})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} />
              </div>
              
              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Kinh độ (Lng)</label>
                <input type="number" step="any" required value={formData.longitude} onChange={e => setFormData({...formData, longitude: parseFloat(e.target.value)})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Ngày lắp đặt</label>
                <input type="date" value={formData.installationDate ? formData.installationDate.substring(0, 10) : ''} onChange={e => setFormData({...formData, installationDate: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Trạng thái</label>
                <select required value={formData.status} onChange={e => setFormData({...formData, status: e.target.value as 'ONLINE'|'OFFLINE'})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                >
                  <option value="ONLINE">ONLINE (Hoạt động)</option>
                  <option value="OFFLINE">OFFLINE (Bảo trì)</option>
                </select>
              </div>
              
              <div style={{ gridColumn: '1 / -1', display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
                <button type="button" onClick={() => { setModalOpen(false); resetForm(); }} style={{ padding: '10px 20px', color: 'var(--text-secondary)', fontWeight: 500 }}>
                  Hủy
                </button>
                <button type="submit" disabled={isLoading} style={{ padding: '10px 20px', backgroundColor: 'var(--primary-color)', color: 'white', borderRadius: 'var(--radius-md)', fontWeight: 500 }}>
                  {isLoading ? 'Đang lưu...' : (editingId ? 'Cập nhật' : 'Thêm mới')}
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
