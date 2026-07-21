import { useEffect, useState } from 'react';
import { useSensorTypeStore } from '../store/useSensorTypeStore';
import { useAuthStore } from '../store/useAuthStore';
import { Plus, Trash2, Settings, Edit2 } from 'lucide-react';

export default function SensorTypesPage() {
  const { sensorTypes, isLoading, error, fetchSensorTypes, addSensorType, updateSensorType, removeSensorType } = useSensorTypeStore();
  const { user } = useAuthStore();
  
  const userRole = user?.role || 'ROLE_STAFF';
  const isAdminOrManager = userRole === 'ROLE_ADMIN' || userRole === 'ROLE_MANAGER';

  const [isModalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState({ 
    id: '', code: '', name: '', unit: '', minRange: 0, maxRange: 100, description: '' 
  });

  useEffect(() => {
    fetchSensorTypes();
  }, [fetchSensorTypes]);

  const handleCreateOrUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const payload = {
        id: formData.id,
        code: formData.code,
        name: formData.name,
        unit: formData.unit,
        minRange: formData.minRange,
        maxRange: formData.maxRange,
        description: formData.description
      };

      if (editingId) {
        await updateSensorType(payload);
      } else {
        await addSensorType(payload);
      }
      setModalOpen(false);
      resetForm();
    } catch (err) {
      // Error handled in store
    }
  };

  const handleEdit = (type: any) => {
    setEditingId(type.id);
    setFormData({ ...formData, ...type });
    setModalOpen(true);
  };

  const resetForm = () => {
    setEditingId(null);
    setFormData({ id: '', code: '', name: '', unit: '', minRange: 0, maxRange: 100, description: '' });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header Actions */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Quản lý Loại Sensor (Catalog)</h2>
        {isAdminOrManager && (
          <button 
            onClick={() => setModalOpen(true)}
            className="hover-lift"
            style={{ 
              display: 'flex', alignItems: 'center', gap: '8px', 
              backgroundColor: 'var(--primary-color)', color: 'white', 
              padding: '10px 20px', borderRadius: 'var(--radius-md)', fontWeight: 500 
            }}
          >
            <Plus size={18} /> Thêm Loại Sensor
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
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Mã (Code)</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Tên Loại Sensor</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Đơn vị</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Ngưỡng an toàn (Min - Max)</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500, width: '100px' }}>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {isLoading && sensorTypes.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Đang tải dữ liệu từ Backend...
                </td>
              </tr>
            ) : sensorTypes.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Chưa có dữ liệu Loại Sensor nào.
                </td>
              </tr>
            ) : (
              sensorTypes.map((type) => (
                <tr key={type.id} style={{ borderBottom: '1px solid var(--border-glass)', transition: 'background-color 0.2s' }} className="hover-row">
                  <td style={{ padding: '16px 24px', fontWeight: 500, color: 'var(--text-secondary)' }}>
                    {type.code}
                  </td>
                  <td style={{ padding: '16px 24px', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <div style={{ padding: '8px', backgroundColor: 'rgba(16, 185, 129, 0.1)', borderRadius: '8px', color: '#10b981' }}>
                      <Settings size={18} />
                    </div>
                    {type.name}
                  </td>
                  <td style={{ padding: '16px 24px', color: 'var(--text-secondary)' }}>
                    <span style={{ padding: '4px 8px', backgroundColor: 'rgba(255,255,255,0.05)', borderRadius: '4px', fontFamily: 'monospace' }}>
                      {type.unit}
                    </span>
                  </td>
                  <td style={{ padding: '16px 24px', color: 'var(--text-secondary)' }}>
                    {type.minRange} ~ {type.maxRange} {type.unit}
                  </td>
                  <td style={{ padding: '16px 24px' }}>
                    {isAdminOrManager && (
                      <div style={{ display: 'flex', gap: '12px' }}>
                        <button 
                          onClick={() => handleEdit(type)}
                          style={{ color: 'var(--text-muted)', transition: 'color 0.2s' }}
                          onMouseEnter={(e) => e.currentTarget.style.color = 'var(--primary-color)'}
                          onMouseLeave={(e) => e.currentTarget.style.color = 'var(--text-muted)'}
                          title="Chỉnh sửa"
                        >
                          <Edit2 size={18} />
                        </button>
                        <button 
                          onClick={() => removeSensorType(type.id)}
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
          <div className="glass-panel" style={{ width: '100%', maxWidth: '500px', padding: '32px', backgroundColor: 'var(--bg-surface)' }}>
            <h3 style={{ fontSize: '1.25rem', marginBottom: '24px' }}>
              {editingId ? 'Cập nhật Loại Sensor' : 'Thêm Loại Sensor'}
            </h3>
            <form onSubmit={handleCreateOrUpdate} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Mã (Code)</label>
                <input required value={formData.code} onChange={e => setFormData({...formData, code: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                  placeholder="VD: ST-PM25" disabled={!!editingId} />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Tên Loại (Name)</label>
                <input required value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                  placeholder="VD: Cảm biến Bụi PM2.5 v1" />
              </div>
              
              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Đơn vị đo</label>
                <input required value={formData.unit} onChange={e => setFormData({...formData, unit: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} 
                  placeholder="VD: µg/m³, °C, %" />
              </div>

              <div style={{ display: 'flex', gap: '16px' }}>
                <div style={{ flex: 1 }}>
                  <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Khoảng đo MIN (minRange)</label>
                  <input type="number" step="any" required value={formData.minRange} onChange={e => setFormData({...formData, minRange: parseFloat(e.target.value)})} 
                    style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} />
                </div>
                <div style={{ flex: 1 }}>
                  <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Khoảng đo MAX (maxRange)</label>
                  <input type="number" step="any" required value={formData.maxRange} onChange={e => setFormData({...formData, maxRange: parseFloat(e.target.value)})} 
                    style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }} />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '8px', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Mô tả thêm</label>
                <textarea value={formData.description} onChange={e => setFormData({...formData, description: e.target.value})} 
                  style={{ width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white', minHeight: '80px' }} 
                  placeholder="Mô tả về loại cảm biến..." />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
                <button type="button" onClick={() => { setModalOpen(false); resetForm(); }} style={{ padding: '10px 20px', color: 'var(--text-secondary)', fontWeight: 500 }}>
                  Hủy
                </button>
                <button type="submit" disabled={isLoading} style={{ padding: '10px 20px', backgroundColor: 'var(--primary-color)', color: 'white', borderRadius: 'var(--radius-md)', fontWeight: 500 }}>
                  {isLoading ? 'Đang lưu...' : (editingId ? 'Cập nhật' : 'Lưu Loại Sensor')}
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
