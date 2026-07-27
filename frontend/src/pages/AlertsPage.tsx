import { useState, useEffect, useMemo } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { useStationStore } from '../store/useStationStore';
import api from '../services/api';
import { AlertTriangle, Clock, Activity, Loader2, Download } from 'lucide-react';

interface AlertHistory {
  id: number;
  stationId: string;
  sensorId: string;
  sensorType: string;
  value: number;
  unit: string;
  message: string;
  timestamp: string;
}

export default function AlertsPage() {
  const { user } = useAuthStore();
  const { stations, fetchStations } = useStationStore();

  const userRole = user?.role || 'ROLE_STAFF';
  const isAdmin = userRole === 'ROLE_ADMIN' || userRole === 'Admin';
  const userStationId = user?.stationId;

  const [selectedStation, setSelectedStation] = useState<string>(isAdmin ? (stations[0]?.id || '') : (userStationId || ''));
  const [alerts, setAlerts] = useState<AlertHistory[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Export State
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [exportData, setExportData] = useState({
    startDate: new Date(new Date().setDate(new Date().getDate() - 7)).toISOString().split('T')[0], // Mặc định 7 ngày trước
    endDate: new Date().toISOString().split('T')[0], // Mặc định hôm nay
    format: 'EXCEL',
    emailTo: user?.email || ''
  });
  const [exportMessage, setExportMessage] = useState<{ text: string, type: 'success' | 'error' } | null>(null);

  useEffect(() => {
    fetchStations();
  }, [fetchStations]);

  useEffect(() => {
    if (isAdmin && !selectedStation && stations.length > 0) {
      setSelectedStation(stations[0].id);
    }
  }, [isAdmin, stations, selectedStation]);

  const loadAlerts = async () => {
    if (!selectedStation) return;
    setIsLoading(true);
    setError(null);
    try {
      const response = await api.get(`/notifications/alerts/station/${selectedStation}`);
      setAlerts(response.data?.data || []);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Lỗi khi tải lịch sử cảnh báo');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAlerts();
  }, [selectedStation]);

  const handleExport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedStation) {
      setExportMessage({ text: 'Vui lòng chọn trạm trước khi xuất báo cáo!', type: 'error' });
      return;
    }
    setIsExporting(true);
    setExportMessage(null);
    try {
      const selectedStationName = displayedStations.find(s => s.id === selectedStation)?.name || selectedStation;
      const payload = {
        stationId: selectedStation,
        stationName: selectedStationName,
        startDate: new Date(exportData.startDate + 'T00:00:00Z').toISOString(),
        endDate: new Date(exportData.endDate + 'T23:59:59Z').toISOString(),
        format: exportData.format,
        emailTo: exportData.emailTo
      };
      const response = await api.post('/report/export', payload);
      setExportMessage({ text: response.data?.message || 'Yêu cầu xuất báo cáo thành công!', type: 'success' });
      setTimeout(() => {
        setIsExportModalOpen(false);
        setExportMessage(null);
      }, 3000);
    } catch (err: any) {
      setExportMessage({ text: err.response?.data?.message || 'Có lỗi xảy ra khi xuất báo cáo', type: 'error' });
    } finally {
      setIsExporting(false);
    }
  };

  const displayedStations = useMemo(() => {
    if (isAdmin) return stations;
    return stations.filter(s => s.id === userStationId);
  }, [isAdmin, stations, userStationId]);

  return (
    <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '24px' }}>
      
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertTriangle className="text-danger" size={24} />
            Lịch sử Cảnh báo
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '4px' }}>
            Hiển thị 20 cảnh báo gần nhất của trạm quan trắc
          </p>
        </div>

        <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
          {isAdmin && displayedStations.length > 0 && (
            <div style={{ minWidth: '250px' }}>
              <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '4px', color: 'var(--text-secondary)' }}>
                Chọn Trạm
              </label>
              <select
                value={selectedStation}
                onChange={(e) => setSelectedStation(e.target.value)}
                className="glass-input"
                style={{ width: '100%', padding: '10px 16px', borderRadius: '8px' }}
              >
                {displayedStations.map(station => (
                  <option key={station.id} value={station.id} style={{ background: 'var(--surface-color)', color: 'var(--text-primary)' }}>
                    {station.name}
                  </option>
                ))}
              </select>
            </div>
          )}
          
          <button
            onClick={() => setIsExportModalOpen(true)}
            className="glass-button primary"
            style={{ padding: '10px 16px', display: 'flex', alignItems: 'center', gap: '8px', borderRadius: '8px', height: '42px' }}
          >
            <Download size={18} />
            Xuất Báo cáo
          </button>
        </div>
      </div>

      {/* Content */}
      <div style={{ background: 'var(--surface-color)', borderRadius: '12px', padding: '20px', border: '1px solid var(--border-color)' }}>
        {isLoading ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: '40px 0', color: 'var(--text-secondary)' }}>
            <Loader2 className="animate-spin" size={32} />
          </div>
        ) : error ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#ef4444' }}>
            <AlertTriangle size={48} style={{ margin: '0 auto 16px', opacity: 0.5 }} />
            <p>{error}</p>
            <button 
              onClick={loadAlerts}
              className="glass-button primary" 
              style={{ marginTop: '16px', padding: '8px 16px' }}
            >
              Thử lại
            </button>
          </div>
        ) : alerts.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-secondary)' }}>
            <Activity size={48} style={{ margin: '0 auto 16px', opacity: 0.3 }} />
            <p>Trạm này hiện chưa có cảnh báo nào.</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                  <th style={{ padding: '12px 16px', fontWeight: 500 }}>Thời gian</th>
                  <th style={{ padding: '12px 16px', fontWeight: 500 }}>Loại Cảm biến</th>
                  <th style={{ padding: '12px 16px', fontWeight: 500 }}>Giá trị</th>
                  <th style={{ padding: '12px 16px', fontWeight: 500 }}>Nội dung cảnh báo</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map((alert) => (
                  <tr key={alert.id} style={{ borderBottom: '1px solid var(--border-color)', transition: 'background 0.2s' }}>
                    <td style={{ padding: '16px', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Clock size={16} className="text-secondary" />
                        <span>{new Date(alert.timestamp).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })}</span>
                      </div>
                    </td>
                    <td style={{ padding: '16px', fontWeight: 500 }}>{alert.sensorType || 'N/A'}</td>
                    <td style={{ padding: '16px', color: '#ef4444', fontWeight: 600 }}>
                      {alert.value} {alert.unit}
                    </td>
                    <td style={{ padding: '16px' }}>
                      <div style={{ 
                        display: 'inline-block', 
                        padding: '6px 12px', 
                        background: 'rgba(239, 68, 68, 0.1)', 
                        color: '#ef4444', 
                        borderRadius: '6px',
                        fontSize: '0.875rem'
                      }}>
                        {alert.message}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Export Modal */}
      {isExportModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999
        }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: '400px', padding: '24px', background: 'var(--bg-surface)' }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Download size={20} className="text-primary" />
              Xuất Báo cáo Cảnh báo
            </h3>

            {exportMessage && (
              <div style={{
                padding: '12px', borderRadius: '8px', marginBottom: '16px',
                background: exportMessage.type === 'success' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                color: exportMessage.type === 'success' ? 'var(--success)' : 'var(--danger)',
                border: `1px solid ${exportMessage.type === 'success' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)'}`
              }}>
                {exportMessage.text}
              </div>
            )}

            <form onSubmit={handleExport} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Từ ngày (Start Date)</label>
                <input 
                  type="date" 
                  required 
                  value={exportData.startDate} 
                  onChange={e => setExportData({...exportData, startDate: e.target.value})}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'rgba(0,0,0,0.2)', color: 'white' }} 
                />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Đến ngày (End Date)</label>
                <input 
                  type="date" 
                  required 
                  value={exportData.endDate} 
                  onChange={e => setExportData({...exportData, endDate: e.target.value})}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'rgba(0,0,0,0.2)', color: 'white' }} 
                />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Định dạng Xuất (Format)</label>
                <select 
                  value={exportData.format} 
                  onChange={e => setExportData({...exportData, format: e.target.value})}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'rgba(0,0,0,0.2)', color: 'white' }}
                >
                  <option value="EXCEL" style={{ background: '#1f2937' }}>Microsoft Excel (.xlsx)</option>
                  <option value="PDF" style={{ background: '#1f2937' }}>PDF Document (.pdf)</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Gửi đến Email</label>
                <input 
                  type="email" 
                  required 
                  value={exportData.emailTo} 
                  onChange={e => setExportData({...exportData, emailTo: e.target.value})}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'rgba(0,0,0,0.2)', color: 'white' }} 
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '8px' }}>
                <button type="button" onClick={() => setIsExportModalOpen(false)} style={{ padding: '10px 20px', background: 'transparent', border: '1px solid var(--border-color)', borderRadius: '8px', color: 'var(--text-secondary)' }}>
                  Hủy
                </button>
                <button type="submit" disabled={isExporting} style={{ padding: '10px 20px', background: 'var(--primary-color)', color: 'white', border: 'none', borderRadius: '8px', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  {isExporting ? <Loader2 size={18} className="animate-spin" /> : <Download size={18} />}
                  {isExporting ? 'Đang xuất...' : 'Xác nhận Xuất'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
