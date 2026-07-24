import { useEffect, useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useStationStore } from '../store/useStationStore';
import { useSensorStore } from '../store/useSensorStore';
import { useGatewayStore } from '../store/useGatewayStore';
import { useTelemetryStore } from '../store/useTelemetryStore';
import { ArrowLeft, Activity, Thermometer, Droplets, Wind, Search } from 'lucide-react';

export default function StationDetailPage({ stationIdProp, hideBackButton }: { stationIdProp?: string, hideBackButton?: boolean }) {
  const params = useParams<{ id: string }>();
  const id = stationIdProp || params.id;
  const navigate = useNavigate();

  const { stations, fetchStations } = useStationStore();
  const { sensors, fetchSensors } = useSensorStore();
  const { gateways, fetchGateways } = useGatewayStore();
  const { realtimeData, historicalData, isLoadingRealtime, isLoadingHistory, fetchRealtimeData, fetchHistoricalData, connectWebSocket, disconnectWebSocket } = useTelemetryStore();

  const [startTime, setStartTime] = useState<string>('');
  const [endTime, setEndTime] = useState<string>('');
  const [currentPage, setCurrentPage] = useState<number>(1);
  const itemsPerPage = 10;

  useEffect(() => {
    if (stations.length === 0) fetchStations();
    if (sensors.length === 0) fetchSensors();
    if (gateways.length === 0) fetchGateways();
    
    if (id) {
      fetchRealtimeData(id); // Fetch initial state
      connectWebSocket(id); // Subscribe to realtime stream

      return () => {
        disconnectWebSocket();
      };
    }
  }, [id, fetchStations, fetchSensors, fetchGateways, fetchRealtimeData, connectWebSocket, disconnectWebSocket]);

  const station = stations.find(s => s.id === id);
  const stationGateways = gateways.filter(g => g.station?.id === id || g.stationId === id);
  const stationGatewayIds = stationGateways.map(g => g.id);
  const stationSensors = sensors.filter(s => stationGatewayIds.includes(s.gatewayId || ''));

  const handleSearchHistory = () => {
    if (id && startTime && endTime) {
      const startIso = new Date(startTime).toISOString();
      const endIso = new Date(endTime).toISOString();
      fetchHistoricalData(id, startIso, endIso);
      setCurrentPage(1);
    } else {
      alert('Vui lòng chọn khoảng thời gian hợp lệ');
    }
  };

  const getSensorIcon = (type: string) => {
    switch (type.toUpperCase()) {
      case 'TEMP': return <Thermometer size={24} />;
      case 'HUMIDITY': return <Droplets size={24} />;
      case 'PM25': return <Wind size={24} />;
      default: return <Activity size={24} />;
    }
  };

  const sensorStats = useMemo(() => {
    const stats: Record<string, { total: number; outOfBounds: number }> = {};
    stationSensors.forEach(s => {
      stats[s.id] = { total: 0, outOfBounds: 0 };
    });

    historicalData.forEach(data => {
      const sensor = stationSensors.find(s => s.id === data.sensorId);
      if (sensor && stats[sensor.id]) {
        stats[sensor.id].total += 1;
        
        let isOut = false;
        if (sensor.minValue !== null && sensor.minValue !== undefined && data.value < sensor.minValue) isOut = true;
        if (sensor.maxValue !== null && sensor.maxValue !== undefined && data.value > sensor.maxValue) isOut = true;
        
        if (isOut) {
          stats[sensor.id].outOfBounds += 1;
        }
      }
    });
    return stats;
  }, [historicalData, stationSensors]);

  if (!station) {
    return <div style={{ padding: '24px', color: 'var(--text-muted)' }}>Đang tải thông tin trạm...</div>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {!hideBackButton && (
          <button 
            onClick={() => navigate('/stations')}
            style={{ padding: '8px', borderRadius: '50%', backgroundColor: 'rgba(255,255,255,0.05)', color: 'var(--text-secondary)' }}
          >
            <ArrowLeft size={20} />
          </button>
        )}
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 600, color: 'white' }}>{station.name}</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>{station.address}</p>
        </div>
      </div>

      {/* Realtime Data Section */}
      <div>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '16px', color: 'var(--primary-color)' }}>Dữ liệu Thời gian thực & Biểu đồ</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '24px' }}>
          {isLoadingRealtime && realtimeData.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>Đang tải dữ liệu...</p>
          ) : stationSensors.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>Trạm này chưa có cảm biến nào.</p>
          ) : (
            stationSensors.map(sensor => {
              const telemetry = realtimeData.find(t => t.sensorId === sensor.id);
              const isValueAvailable = telemetry !== undefined;
              const currentValue = telemetry?.value;
              let isOutOfRange = false;
              
              if (isValueAvailable && currentValue !== undefined) {
                  if (sensor.minValue !== undefined && sensor.minValue !== null && currentValue < sensor.minValue) isOutOfRange = true;
                  if (sensor.maxValue !== undefined && sensor.maxValue !== null && currentValue > sensor.maxValue) isOutOfRange = true;
              }
              
              const valueColor = isOutOfRange ? '#ef4444' : 'white';

              return (
                <div key={sensor.id} style={{ display: 'flex', flexDirection: 'column', backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: 'var(--radius-lg)', overflow: 'hidden', border: isOutOfRange ? '1px solid rgba(239, 68, 68, 0.4)' : '1px solid rgba(255,255,255,0.05)' }}>
                  
                  {/* Cảm biến Header */}
                  <div style={{ padding: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', backgroundColor: isOutOfRange ? 'rgba(239, 68, 68, 0.05)' : 'rgba(255,255,255,0.02)', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div style={{ padding: '12px', backgroundColor: isOutOfRange ? 'rgba(239, 68, 68, 0.1)' : 'rgba(56, 189, 248, 0.1)', borderRadius: '12px', color: isOutOfRange ? '#ef4444' : '#38bdf8' }}>
                        {getSensorIcon(sensor.sensorTypeName || '')}
                      </div>
                      <div>
                        <p style={{ fontWeight: 600, color: 'white', fontSize: '1rem', marginBottom: '2px' }}>{sensor.name}</p>
                        <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                          Ngưỡng: {sensor.minValue ?? '-'} ~ {sensor.maxValue ?? '-'}
                        </p>
                      </div>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', justifyContent: 'flex-end' }}>
                        <span style={{ fontSize: '1.75rem', fontWeight: 700, color: valueColor }}>
                          {telemetry ? telemetry.value.toFixed(2) : '--'}
                        </span>
                        {isOutOfRange && (
                          <span style={{ fontSize: '0.75rem', color: '#ef4444', fontWeight: 600, padding: '2px 4px', backgroundColor: 'rgba(239, 68, 68, 0.1)', borderRadius: '4px' }}>Cảnh báo</span>
                        )}
                      </div>
                      {telemetry && (
                        <p style={{ color: 'var(--text-muted)', fontSize: '0.7rem', marginTop: '4px' }}>
                          Cập nhật: {new Date(telemetry.timestamp).toLocaleTimeString()}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Grafana Iframe */}
                  <div style={{ height: '300px', width: '100%', backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <iframe 
                      src={`http://localhost:3000/d-solo/ad5x4sl/new-dashboard?orgId=1&timezone=browser&panelId=panel-1&theme=dark&kiosk=tv&var-stationId=${station.id}&var-sensorId=${sensor.id}&var-min=${sensor.minValue ?? 0}&var-max=${sensor.maxValue ?? 100}`}
                      width="100%" 
                      height="100%" 
                      style={{ border: 'none' }}
                      title={`Grafana Chart ${sensor.name}`}
                    ></iframe>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>


      {/* Historical Data Table */}
      <div>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '16px', color: 'var(--primary-color)' }}>Tra cứu Lịch sử</h3>
        <div className="glass-panel" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', gap: '16px', marginBottom: '24px', alignItems: 'flex-end' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '8px' }}>Từ thời điểm</label>
              <input 
                type="datetime-local" 
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                style={{ padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '8px' }}>Đến thời điểm</label>
              <input 
                type="datetime-local" 
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                style={{ padding: '10px 16px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }}
              />
            </div>
            <button 
              onClick={handleSearchHistory}
              disabled={isLoadingHistory}
              style={{ padding: '10px 24px', backgroundColor: 'var(--primary-color)', color: 'white', borderRadius: 'var(--radius-md)', fontWeight: 500, display: 'flex', alignItems: 'center', gap: '8px' }}
            >
              <Search size={18} /> {isLoadingHistory ? 'Đang tải...' : 'Tìm kiếm'}
            </button>
          </div>

          {/* Báo cáo số lần vượt ngưỡng */}
          {historicalData.length > 0 && (
            <div style={{ marginBottom: '32px' }}>
              <h4 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '16px', color: 'var(--text-primary)' }}>Báo cáo Cảm biến (Trong thời gian tra cứu)</h4>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '16px' }}>
                {stationSensors.map(sensor => {
                  const stat = sensorStats[sensor.id];
                  if (!stat) return null;
                  
                  const isDanger = stat.outOfBounds > 0;
                  
                  return (
                    <div key={sensor.id} style={{ 
                      padding: '16px', 
                      borderRadius: 'var(--radius-md)', 
                      backgroundColor: 'rgba(0,0,0,0.2)',
                      borderLeft: `4px solid ${isDanger ? 'var(--danger-color, #ef4444)' : 'var(--success-color, #22c55e)'}`
                    }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                        <div>
                          <h5 style={{ fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>{sensor.name}</h5>
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>{sensor.type}</span>
                        </div>
                      </div>
                      
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem' }}>
                        <span style={{ color: 'var(--text-secondary)' }}>Tổng số lần đo:</span>
                        <span style={{ fontWeight: 500 }}>{stat.total}</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem', marginTop: '8px' }}>
                        <span style={{ color: 'var(--text-secondary)' }}>Số lần vượt ngưỡng:</span>
                        <span style={{ fontWeight: 600, color: isDanger ? 'var(--danger-color, #ef4444)' : 'var(--text-primary)' }}>
                          {stat.outOfBounds}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-glass)' }}>
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 500, minWidth: '180px' }}>Thời gian</th>
                  {stationSensors.map(sensor => (
                    <th key={sensor.id} style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 500 }}>
                      {sensor.name} <br/>
                      <span style={{ fontSize: '0.75rem', fontWeight: 'normal', opacity: 0.7 }}>({sensor.type})</span>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {(() => {
                  if (historicalData.length === 0) {
                    return (
                      <tr>
                        <td colSpan={stationSensors.length + 1} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                          Không có dữ liệu lịch sử trong khoảng thời gian này.
                        </td>
                      </tr>
                    );
                  }

                  const pivotedData: Record<number, { timeStr: string; values: Record<string, number> }> = {};
                  
                  historicalData.forEach(data => {
                    const timeNum = new Date(data.timestamp).getTime();
                    const timeRounded = Math.floor(timeNum / 1000) * 1000;
                    
                    if (!pivotedData[timeRounded]) {
                      pivotedData[timeRounded] = {
                        timeStr: new Date(data.timestamp).toLocaleString(),
                        values: {}
                      };
                    }
                    pivotedData[timeRounded].values[data.sensorId] = data.value;
                  });

                  const sortedTimes = Object.keys(pivotedData).map(Number).sort((a, b) => b - a);
                  
                  const itemsPerPage = 20;
                  const totalPages = Math.ceil(sortedTimes.length / itemsPerPage);
                  const startIndex = (currentPage - 1) * itemsPerPage;
                  const currentTimes = sortedTimes.slice(startIndex, startIndex + itemsPerPage);

                  return (
                    <>
                      {currentTimes.map(timeNum => {
                        const row = pivotedData[timeNum];
                        return (
                          <tr key={timeNum} style={{ borderBottom: '1px solid var(--border-glass)' }}>
                            <td style={{ padding: '12px 16px' }}>{row.timeStr}</td>
                            {stationSensors.map(sensor => {
                              const val = row.values[sensor.id];
                              let isOutOfRange = false;
                              if (val !== undefined) {
                                if (sensor.minValue !== null && sensor.minValue !== undefined && val < sensor.minValue) isOutOfRange = true;
                                if (sensor.maxValue !== null && sensor.maxValue !== undefined && val > sensor.maxValue) isOutOfRange = true;
                              }
                              
                              return (
                                <td key={sensor.id} style={{ 
                                  padding: '12px 16px', 
                                  fontWeight: 600,
                                  color: isOutOfRange ? 'var(--danger-color, #ef4444)' : 'inherit'
                                }}>
                                  {val !== undefined ? val.toFixed(2) : '-'}
                                </td>
                              );
                            })}
                          </tr>
                        );
                      })}
                      {totalPages > 1 && (
                        <tr>
                          <td colSpan={stationSensors.length + 1} style={{ padding: '16px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                              <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                                Trang {currentPage} / {totalPages}
                              </span>
                              <div style={{ display: 'flex', gap: '8px' }}>
                                <button
                                  onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                                  disabled={currentPage === 1}
                                  style={{
                                    padding: '6px 12px',
                                    borderRadius: 'var(--radius-sm)',
                                    border: '1px solid var(--border-color)',
                                    backgroundColor: currentPage === 1 ? 'rgba(255,255,255,0.05)' : 'rgba(255,255,255,0.1)',
                                    color: currentPage === 1 ? 'var(--text-muted)' : 'var(--text-primary)',
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
                                    borderRadius: 'var(--radius-sm)',
                                    border: '1px solid var(--border-color)',
                                    backgroundColor: currentPage === totalPages ? 'rgba(255,255,255,0.05)' : 'rgba(255,255,255,0.1)',
                                    color: currentPage === totalPages ? 'var(--text-muted)' : 'var(--text-primary)',
                                    cursor: currentPage === totalPages ? 'not-allowed' : 'pointer'
                                  }}
                                >
                                  Sau
                                </button>
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </>
                  );
                })()}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
