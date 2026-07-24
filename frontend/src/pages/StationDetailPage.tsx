import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useStationStore } from '../store/useStationStore';
import { useSensorStore } from '../store/useSensorStore';
import { useGatewayStore } from '../store/useGatewayStore';
import { useTelemetryStore } from '../store/useTelemetryStore';
import { ArrowLeft, Activity, Thermometer, Droplets, Wind, Search } from 'lucide-react';

export default function StationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { stations, fetchStations } = useStationStore();
  const { sensors, fetchSensors } = useSensorStore();
  const { gateways, fetchGateways } = useGatewayStore();
  const { realtimeData, historicalData, isLoadingRealtime, isLoadingHistory, fetchRealtimeData, fetchHistoricalData, connectWebSocket, disconnectWebSocket } = useTelemetryStore();

  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');

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

  if (!station) {
    return <div style={{ padding: '24px', color: 'var(--text-muted)' }}>Đang tải thông tin trạm...</div>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <button 
          onClick={() => navigate('/stations')}
          style={{ padding: '8px', borderRadius: '50%', backgroundColor: 'rgba(255,255,255,0.05)', color: 'var(--text-secondary)' }}
        >
          <ArrowLeft size={20} />
        </button>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 600, color: 'white' }}>{station.name}</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>{station.address}</p>
        </div>
      </div>

      {/* Realtime Data Section */}
      <div>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '16px', color: 'var(--primary-color)' }}>Dữ liệu Thời gian thực</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '16px' }}>
          {isLoadingRealtime && realtimeData.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>Đang tải dữ liệu...</p>
          ) : stationSensors.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>Trạm này chưa có cảm biến nào.</p>
          ) : (
            stationSensors.map(sensor => {
              // Map by sensorId or type. The backend returns sensorId.
              const telemetry = realtimeData.find(t => t.sensorId === sensor.id);
              
              const isValueAvailable = telemetry !== undefined;
              const currentValue = telemetry?.value;
              let isOutOfRange = false;
              
              if (isValueAvailable && currentValue !== undefined) {
                  if (sensor.minValue !== undefined && sensor.minValue !== null && currentValue < sensor.minValue) {
                      isOutOfRange = true;
                  }
                  if (sensor.maxValue !== undefined && sensor.maxValue !== null && currentValue > sensor.maxValue) {
                      isOutOfRange = true;
                  }
              }
              
              const valueColor = isOutOfRange ? '#ef4444' : 'white'; // Red if out of bounds

              return (
                <div key={sensor.id} className="glass-panel hover-lift" style={{ padding: '20px', display: 'flex', alignItems: 'center', gap: '16px', border: isOutOfRange ? '1px solid rgba(239, 68, 68, 0.3)' : undefined }}>
                  <div style={{ padding: '12px', backgroundColor: isOutOfRange ? 'rgba(239, 68, 68, 0.1)' : 'rgba(56, 189, 248, 0.1)', borderRadius: '12px', color: isOutOfRange ? '#ef4444' : '#38bdf8', transition: 'all 0.3s ease' }}>
                    {getSensorIcon(sensor.sensorTypeName || '')}
                  </div>
                  <div>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '4px' }}>
                      {sensor.name}
                      {(sensor.minValue !== undefined && sensor.minValue !== null && sensor.maxValue !== undefined && sensor.maxValue !== null) && 
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: '8px' }}>
                          (Ngưỡng: {sensor.minValue} - {sensor.maxValue})
                        </span>
                      }
                    </p>
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px' }}>
                      <span style={{ fontSize: '1.5rem', fontWeight: 700, color: valueColor, transition: 'color 0.3s ease' }}>
                        {telemetry ? telemetry.value.toFixed(2) : '--'}
                      </span>
                      {isOutOfRange && (
                        <span style={{ fontSize: '0.75rem', color: '#ef4444', fontWeight: 600, padding: '2px 6px', backgroundColor: 'rgba(239, 68, 68, 0.1)', borderRadius: '4px' }}>Cảnh báo</span>
                      )}
                    </div>
                    {telemetry && (
                      <p style={{ color: 'var(--text-muted)', fontSize: '0.75rem', marginTop: '4px' }}>
                        Cập nhật: {new Date(telemetry.timestamp).toLocaleTimeString()}
                      </p>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Grafana Graph Section */}
      <div>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '16px', color: 'var(--primary-color)' }}>Đồ thị Trực quan</h3>
        <div className="glass-panel" style={{ padding: '0', overflow: 'hidden', height: '400px' }}>
          {/* Replace this URL with actual Grafana dashboard URL */}
          <iframe 
            src={`http://localhost:3000/d-solo/ad7cfqj/station-dashboard?orgId=1&timezone=browser&var-stationId=${station.id}&panelId=1&theme=dark`} 
            width="100%" 
            height="100%" 
            frameBorder="0"
            title="Grafana Dashboard"
          ></iframe>
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

          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-glass)' }}>
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 500 }}>Thời gian</th>
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 500 }}>Cảm biến</th>
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 500 }}>Loại</th>
                  <th style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontWeight: 500 }}>Giá trị</th>
                </tr>
              </thead>
              <tbody>
                {historicalData.length === 0 ? (
                  <tr>
                    <td colSpan={4} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                      Không có dữ liệu lịch sử trong khoảng thời gian này.
                    </td>
                  </tr>
                ) : (
                  historicalData.map((data, index) => {
                    const sensor = stationSensors.find(s => s.id === data.sensorId);
                    return (
                      <tr key={index} style={{ borderBottom: '1px solid var(--border-glass)' }}>
                        <td style={{ padding: '12px 16px' }}>{new Date(data.timestamp).toLocaleString()}</td>
                        <td style={{ padding: '12px 16px' }}>{sensor ? sensor.name : data.sensorId}</td>
                        <td style={{ padding: '12px 16px' }}>{data.sensorType}</td>
                        <td style={{ padding: '12px 16px', fontWeight: 600 }}>{data.value.toFixed(2)}</td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
