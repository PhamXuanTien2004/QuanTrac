import { useState, useEffect, useMemo } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { useStationStore } from '../store/useStationStore';
import api from '../services/api';
import { AlertTriangle, Clock, Activity, Loader2, ChevronLeft, ChevronRight } from 'lucide-react';

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

  // Pagination & Filter State
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [startDate, setStartDate] = useState(new Date(new Date().setDate(new Date().getDate() - 30)).toISOString().split('T')[0]);
  const [endDate, setEndDate] = useState(new Date().toISOString().split('T')[0]);

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
      const payload = {
        stationId: selectedStation,
        startDate: new Date(startDate + 'T00:00:00Z').toISOString(),
        endDate: new Date(endDate + 'T23:59:59Z').toISOString(),
        page: currentPage,
        size: 20
      };
      const response = await api.post(`/notifications/alerts/filter`, payload);
      const data = response.data?.data;
      setAlerts(data?.content || []);
      setTotalPages(data?.totalPages || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Lỗi khi tải lịch sử cảnh báo');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAlerts();
  }, [selectedStation, currentPage, startDate, endDate]);

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
            Hiển thị 20 cảnh báo mỗi trang
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
                onChange={(e) => { setSelectedStation(e.target.value); setCurrentPage(0); }}
                style={{ width: '100%', padding: '10px 16px', borderRadius: '8px' }}
              >
                {displayedStations.map(station => (
                  <option key={station.id} value={station.id}>
                    {station.name}
                  </option>
                ))}
              </select>
            </div>
          )}
          
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '4px', color: 'var(--text-secondary)' }}>
              Từ ngày
            </label>
            <input 
              type="date" 
              value={startDate} 
              onChange={e => { setStartDate(e.target.value); setCurrentPage(0); }}
              style={{ padding: '9px 12px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'rgba(0,0,0,0.2)', color: 'white' }} 
            />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '4px', color: 'var(--text-secondary)' }}>
              Đến ngày
            </label>
            <input 
              type="date" 
              value={endDate} 
              onChange={e => { setEndDate(e.target.value); setCurrentPage(0); }}
              style={{ padding: '9px 12px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'rgba(0,0,0,0.2)', color: 'white' }} 
            />
          </div>
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
            <p>Trạm này hiện chưa có cảnh báo nào trong khoảng thời gian này.</p>
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

        {/* Pagination */}
        {totalPages > 0 && (
          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '12px', marginTop: '24px' }}>
            <button
              onClick={() => setCurrentPage(p => Math.max(0, p - 1))}
              disabled={currentPage === 0 || isLoading}
              className="glass-button"
              style={{ padding: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            >
              <ChevronLeft size={20} />
            </button>
            <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
              Trang {currentPage + 1} / {totalPages}
            </span>
            <button
              onClick={() => setCurrentPage(p => Math.min(totalPages - 1, p + 1))}
              disabled={currentPage === totalPages - 1 || isLoading}
              className="glass-button"
              style={{ padding: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            >
              <ChevronRight size={20} />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
