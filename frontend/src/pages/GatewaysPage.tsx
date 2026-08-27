import { useEffect, useState } from 'react';
import { useGatewayStore } from '../store/useGatewayStore';
import { useStationStore } from '../store/useStationStore';
import { useAuthStore } from '../store/useAuthStore';
import { Plus, Trash2, Server, Edit2 } from 'lucide-react';

export default function GatewaysPage() {
  const { gateways, isLoading: isLoadingGateways, error, fetchGateways, addGateway, updateGateway, removeGateway } = useGatewayStore();
  const { stations, fetchStations } = useStationStore();
  const { user } = useAuthStore();
  
  const userRole = user?.role || 'ROLE_STAFF';
  const isStaff = userRole === 'ROLE_STAFF';

  // Phân trang
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;
  
  // Logic phân trang hiển thị
  const totalPages = Math.ceil(gateways.length / itemsPerPage);
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentGateways = gateways.slice(indexOfFirstItem, indexOfLastItem);

  const [isModalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState({ 
    id: '', code: '', stationName: '', status: 'ONLINE' as const 
  });

  useEffect(() => {
    fetchGateways();
    fetchStations();
  }, [fetchGateways, fetchStations]);

  const handleCreateOrUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload = {
        id: formData.id,
        code: formData.code,
        stationName: formData.stationName,
        status: formData.status
      };
      
      if (editingId) {
        await updateGateway(payload);
      } else {
        await addGateway(payload);
      }
      setModalOpen(false);
      resetForm();
    } catch (err) {
      // Error handled in store
    }
  };

  const handleEdit = (gateway: any) => {
    setEditingId(gateway.id);
    const station = stations.find(s => s.id === gateway.station?.id || s.id === gateway.stationId);
    setFormData({ ...formData, ...gateway, stationName: station ? station.name : '' });
    setModalOpen(true);
  };

  const resetForm = () => {
    setEditingId(null);
    setFormData({ id: '', code: '', stationName: '', status: 'ONLINE' });
  };

  const getStationName = (id: string) => {
    const station = stations.find(s => s.id === id);
    return station ? station.name : id;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header Actions */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Quản lý Gateway (Bộ thu thập)</h2>
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
            <Plus size={18} /> Thêm Gateway mới
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
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Mã Gateway</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Thuộc Trạm</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Trạng thái</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500, width: '100px' }}>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {isLoadingGateways && gateways.length === 0 ? (
              <tr>
                <td colSpan={4} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Đang tải dữ liệu từ Backend...
                </td>
              </tr>
            ) : gateways.length === 0 ? (
              <tr>
                <td colSpan={4} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Chưa có dữ liệu Gateway nào.
                </td>
              </tr>
            ) : (
              currentGateways.map((gateway) => (
                <tr key={gateway.id} style={{ borderBottom: '1px solid var(--border-glass)', transition: 'background-color 0.2s' }} className="hover-row">
                  <td style={{ padding: '16px 24px', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <div style={{ padding: '8px', backgroundColor: 'rgba(245, 158, 11, 0.1)', borderRadius: '8px', color: '#f59e0b' }}>
                      <Server size={18} />
                    </div>
                    {gateway.code}
                  </td>
                  <td style={{ padding: '16px 24px', color: 'var(--text-secondary)' }}>
                    {getStationName(gateway.station?.id || gateway.stationId)}
                  </td>
                  <td style={{ padding: '16px 24px' }}>
                    <span style={{ 
                      padding: '4px 12px', 
                      borderRadius: '99px', 
                      fontSize: '0.75rem', 
                      fontWeight: 600,
                      backgroundColor: gateway.status === 'ONLINE' ? 'rgba(34, 197, 94, 0.1)' : gateway.status === 'WARNING' ? 'rgba(245, 158, 11, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                      color: gateway.status === 'ONLINE' ? 'var(--success)' : gateway.status === 'WARNING' ? 'var(--warning)' : 'var(--danger)',
                      border: `1px solid ${gateway.status === 'ONLINE' ? 'rgba(34, 197, 94, 0.2)' : gateway.status === 'WARNING' ? 'rgba(245, 158, 11, 0.2)' : 'rgba(239, 68, 68, 0.2)'}`
                    }}>
                      {gateway.status === 'ONLINE' ? 'Kết nối' : gateway.status === 'WARNING' ? 'Cảnh báo' : 'Mất kết nối'}
                    </span>
                  </td>
                  <td style={{ padding: '16px 24px' }}>
                    {!isStaff && (
                      <div style={{ display: 'flex', gap: '12px' }}>
                        <button 
                          onClick={() => handleEdit(gateway)}
                          style={{ color: 'var(--text-muted)', transition: 'color 0.2s' }}
                          onMouseEnter={(e) => e.currentTarget.style.color = 'var(--primary-color)'}
                          onMouseLeave={(e) => e.currentTarget.style.color = 'var(--text-muted)'}
                          title="Chỉnh sửa"
                        >
                          <Edit2 size={18} />
                        </button>
                        <button 
                          onClick={() => removeGateway(gateway.id)}
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

      {/* Create / Edit Modal */}
      {isModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh',
          backgroundColor: 'rgba(0, 0, 0, 0.5)', backdropFilter: 'blur(4px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50
        }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: '600px', padding: '32px', backgroundColor: 'var(--bg-surface)', maxHeight: '90vh', overflowY: 'auto' }}>
            <h3 style={{ fontSize: '1.25rem', marginBottom: '24px' }}>
              {editingId ? 'Cập nhật Gateway' : 'Thêm Gateway mới'}
            </h3>
            <form onSubmit={handleCreateOrUpdate} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              
              <div style={{ gridColumn: '1 / -1' }}>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Mã Gateway (Unique)</label>
                <input required value={formData.code} onChange={e => setFormData({...formData, code: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                  placeholder="VD: GW-HN-001" 
                  disabled={!!editingId} />
              </div>
              
              <div style={{ gridColumn: '1 / -1' }}>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Thuộc Trạm (Bắt buộc)</label>
                <select required value={formData.stationName} onChange={e => setFormData({...formData, stationName: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                >
                  <option value="">-- Chọn Trạm --</option>
                  {stations.map(st => (
                    <option key={st.id} value={st.name}>{st.name}</option>
                  ))}
                </select>
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
                <button type="submit" disabled={isLoadingGateways} style={{ padding: '10px 20px', backgroundColor: 'var(--primary-color)', color: 'white', borderRadius: 'var(--radius-md)', fontWeight: 500 }}>
                  {isLoadingGateways ? 'Đang lưu...' : (editingId ? 'Cập nhật' : 'Thêm mới')}
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
