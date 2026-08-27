import React, { useState, useEffect, useMemo } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { useStationStore } from '../store/useStationStore';
import api from '../services/api';
import { Download, Loader2, FileText } from 'lucide-react';

export default function ReportsPage() {
  const { user } = useAuthStore();
  const { stations, fetchStations } = useStationStore();

  const userRole = user?.role || 'ROLE_STAFF';
  const isAdmin = userRole === 'ROLE_ADMIN' || userRole === 'Admin';
  const userStationId = user?.stationId;

  const [selectedStation, setSelectedStation] = useState<string>('');
  const [isExporting, setIsExporting] = useState(false);
  const [exportData, setExportData] = useState({
    startDate: new Date(new Date().setDate(new Date().getDate() - 30)).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0],
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
    } else if (!isAdmin && userStationId) {
      setSelectedStation(userStationId);
    }
  }, [isAdmin, stations, selectedStation, userStationId]);

  const displayedStations = useMemo(() => {
    if (isAdmin) return stations;
    return stations.filter(s => s.id === userStationId);
  }, [isAdmin, stations, userStationId]);

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
      setExportMessage({ text: response.data?.message || 'Yêu cầu xuất báo cáo thành công! Vui lòng kiểm tra email.', type: 'success' });
    } catch (err: any) {
      setExportMessage({ text: err.response?.data?.message || 'Có lỗi xảy ra khi xuất báo cáo', type: 'error' });
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '24px', maxWidth: '600px', margin: '0 auto' }}>
      <h2 style={{ fontSize: '1.25rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
        <FileText className="text-primary" size={24} />
        Xuất Báo cáo Chất lượng Không Khí
      </h2>

      {exportMessage && (
        <div style={{
          padding: '16px', borderRadius: '8px', 
          background: exportMessage.type === 'success' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)',
          color: exportMessage.type === 'success' ? 'var(--success)' : 'var(--danger)',
          border: `1px solid ${exportMessage.type === 'success' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)'}`
        }}>
          {exportMessage.text}
        </div>
      )}

      <form onSubmit={handleExport} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        {isAdmin && displayedStations.length > 0 && (
          <div>
            <label style={{ display: 'block', marginBottom: '8px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Chọn Trạm</label>
            <select
              value={selectedStation}
              onChange={(e) => setSelectedStation(e.target.value)}
              style={{ width: '100%', padding: '12px 16px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'rgba(0,0,0,0.2)', color: 'white' }}
            >
              {displayedStations.map(station => (
                <option key={station.id} value={station.id} style={{ background: '#1f2937' }}>
                  {station.name}
                </option>
              ))}
            </select>
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '8px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Từ ngày</label>
            <input 
              type="date" 
              required 
              value={exportData.startDate} 
              onChange={e => setExportData({...exportData, startDate: e.target.value})}
              style={{ width: '100%', padding: '12px 16px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'rgba(0,0,0,0.2)', color: 'white' }} 
            />
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: '8px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Đến ngày</label>
            <input 
              type="date" 
              required 
              value={exportData.endDate} 
              onChange={e => setExportData({...exportData, endDate: e.target.value})}
              style={{ width: '100%', padding: '12px 16px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'rgba(0,0,0,0.2)', color: 'white' }} 
            />
          </div>
        </div>

        <div>
          <label style={{ display: 'block', marginBottom: '8px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Định dạng Xuất</label>
          <select 
            value={exportData.format} 
            onChange={e => setExportData({...exportData, format: e.target.value})}
            style={{ width: '100%', padding: '12px 16px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'rgba(0,0,0,0.2)', color: 'white' }}
          >
            <option value="EXCEL" style={{ background: '#1f2937' }}>Microsoft Excel (.xlsx)</option>
            <option value="PDF" style={{ background: '#1f2937' }}>PDF Document (.pdf)</option>
          </select>
        </div>

        <div>
          <label style={{ display: 'block', marginBottom: '8px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Email Nhận Báo Cáo</label>
          <input 
            type="email" 
            required 
            value={exportData.emailTo} 
            onChange={e => setExportData({...exportData, emailTo: e.target.value})}
            placeholder="Nhập địa chỉ email của bạn"
            style={{ width: '100%', padding: '12px 16px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'rgba(0,0,0,0.2)', color: 'white' }} 
          />
        </div>

        <button 
          type="submit" 
          disabled={isExporting} 
          style={{ 
            marginTop: '12px',
            padding: '14px 20px', 
            background: 'var(--primary-color)', 
            color: 'white', 
            border: 'none', 
            borderRadius: '8px', 
            fontWeight: 500, 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            gap: '8px' 
          }}
        >
          {isExporting ? <Loader2 size={20} className="animate-spin" /> : <Download size={20} />}
          {isExporting ? 'Đang xử lý...' : 'Xác nhận Xuất Báo cáo'}
        </button>
      </form>
    </div>
  );
}
