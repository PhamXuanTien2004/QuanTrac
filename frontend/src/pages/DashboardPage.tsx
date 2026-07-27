import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { useEffect, useState, useMemo } from 'react';
import { useStationStore } from '../store/useStationStore';
import { useAuthStore } from '../store/useAuthStore';
import { useGatewayStore } from '../store/useGatewayStore';
import { useSensorStore } from '../store/useSensorStore';
import { useTelemetryStore } from '../store/useTelemetryStore';
import { Activity, Thermometer, Droplets, Wind } from 'lucide-react';
import StationDetailPage from './StationDetailPage';

// Fix Leaflet icon issue in React
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

export default function DashboardPage() {
  const { user } = useAuthStore();
  const { stations, fetchStations } = useStationStore();
  const { gateways, fetchGateways } = useGatewayStore();
  const { sensors, fetchSensors } = useSensorStore();
  const { realtimeData, fetchRealtimeData, connectWebSocket, disconnectWebSocket } = useTelemetryStore();

  const userRole = user?.role || 'ROLE_STAFF';
  const isAdmin = userRole === 'ROLE_ADMIN' || userRole === 'Admin';
  const userStationId = user?.stationId;

  const [mapCenter, setMapCenter] = useState<[number, number]>([21.0285, 105.8542]);
  const [mapZoom, setMapZoom] = useState(13);
  
  // Trạng thái cho Accordion (Tự động mở sẵn cho Staff/Manager)
  const [expandedStationId, setExpandedStationId] = useState<string | null>(!isAdmin ? (userStationId || null) : null);

  useEffect(() => {
    fetchStations();
    // Admin cần load cả gateway và sensor để hiển thị trong list danh sách trạm
    fetchGateways();
    fetchSensors();
  }, [fetchStations, fetchGateways, fetchSensors]);

  useEffect(() => {
    window.dispatchEvent(new Event('resize'));
  }, [stations]);

  // Connect to WebSocket for Real-time Data
  useEffect(() => {
    const targetStationId = isAdmin ? expandedStationId : userStationId;
    if (targetStationId) {
      fetchRealtimeData(targetStationId);
      connectWebSocket(targetStationId);
      return () => disconnectWebSocket();
    }
  }, [isAdmin, expandedStationId, userStationId, fetchRealtimeData, connectWebSocket, disconnectWebSocket]);

  // Logic lọc dữ liệu và bản đồ theo Role
  const displayedStations = useMemo(() => {
    return isAdmin ? stations : stations.filter((s) => s.id === userStationId);
  }, [isAdmin, stations, userStationId]);

  useEffect(() => {
    if (!isAdmin && displayedStations.length === 1) {
      const st = displayedStations[0];
      if (st.latitude && st.longitude) {
        setMapCenter([st.latitude, st.longitude]);
        setMapZoom(16); // Zoom sát vào trạm
      }
    } else if (isAdmin) {
      setMapCenter([16.047079, 108.206230]); // Tọa độ trung tâm VN
      setMapZoom(6);
    }
  }, [displayedStations, isAdmin]);

  // Tính toán số liệu thống kê
  let stats = [];
  if (isAdmin) {
    const activeStations = stations.filter((s) => s.status === 'ONLINE').length;
    const offlineStations = stations.filter((s) => s.status === 'OFFLINE' || s.status === 'ERROR').length;
    stats = [
      { label: 'Tổng số Trạm (Toàn quốc)', value: stations.length.toString(), color: 'var(--primary-color)' },
      { label: 'Trạm đang hoạt động', value: activeStations.toString(), color: 'var(--success)' },
      { label: 'Trạm mất kết nối/Lỗi', value: offlineStations.toString(), color: 'var(--danger)' },
    ];
  } else {
    const stationGateways = gateways.filter((g) => g.station?.id === userStationId || g.stationId === userStationId);
    const activeGateways = stationGateways.filter((g) => g.status === 'ONLINE').length;
    
    const gatewayIds = stationGateways.map(g => g.id);
    const stationSensors = sensors.filter(s => gatewayIds.includes(s.gateway?.id || s.gatewayId));

    stats = [
      { label: 'Tổng số Gateway', value: stationGateways.length.toString(), color: 'var(--primary-color)' },
      { label: 'Gateway hoạt động', value: activeGateways.toString(), color: 'var(--success)' },
      { label: 'Tổng số Sensor', value: stationSensors.length.toString(), color: 'var(--warning)' },
    ];
  }

  // Phân loại danh sách Trạm (cho Admin)
  const onlineStationsList = displayedStations.filter(s => s.status === 'ONLINE');
  const offlineStationsList = displayedStations.filter(s => s.status !== 'ONLINE');

  // Helper function lấy Sensors thuộc về 1 Station
  const getSensorsByStation = (stationId: string) => {
    const stGateways = gateways.filter(g => (g.station?.id || g.stationId) === stationId);
    const gwIds = stGateways.map(g => g.id);
    return sensors.filter(s => gwIds.includes(s.gateway?.id || s.gatewayId));
  };

  // Logic kiểm tra để hiển thị Grafana
  // Tìm Gateway đang ONLINE thuộc về các trạm đang hiển thị
  const validGateways = gateways.filter(g => 
    g.status === 'ONLINE' && 
    displayedStations.some(s => s.id === (g.station?.id || g.stationId))
  );
  
  // Tìm Sensor đang ONLINE thuộc về các Gateway hợp lệ bên trên
  const validSensors = sensors.filter(s => 
    s.status === 'ONLINE' && 
    validGateways.some(g => g.id === (s.gateway?.id || s.gatewayId))
  );

  const hasActiveDevices = validGateways.length > 0 && validSensors.length > 0;
  
  // Tùy biến link Grafana (Truyền tham số lọc nếu có thiết bị active)
  const activeGatewayId = validGateways.length > 0 ? validGateways[0].id : '';
  const activeStationId = displayedStations.length > 0 ? displayedStations[0].id : '';
  
  let grafanaUrl = "http://localhost:3000/d-solo/ad5x4sl/new-dashboard?orgId=1&timezone=browser&panelId=panel-1&theme=dark&kiosk=tv";
  if (!isAdmin && hasActiveDevices) {
    grafanaUrl += `&var-stationId=${activeStationId}`;
  }

  const toggleStation = (stId: string) => {
    setExpandedStationId(prev => prev === stId ? null : stId);
  };

  const getSensorIcon = (type: string) => {
    switch (type.toUpperCase()) {
      case 'TEMP': return <Thermometer size={24} />;
      case 'HUMIDITY': return <Droplets size={24} />;
      case 'PM25': return <Wind size={24} />;
      default: return <Activity size={24} />;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Thống kê nhanh */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '24px' }}>
        {stats.map((stat, idx) => (
          <div key={idx} className="glass-panel hover-lift" style={{ padding: '24px' }}>
            <h3 style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '8px' }}>
              {stat.label}
            </h3>
            <div style={{ fontSize: '2rem', fontWeight: 'bold', color: stat.color }}>
              {stat.value}
            </div>
          </div>
        ))}
      </div>

      {isAdmin ? (
        <>
          {/* BẢN ĐỒ (CHỈ DÀNH CHO ADMIN) */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '24px' }}>
            <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column' }}>
              <h2 style={{ fontSize: '1.25rem', marginBottom: '16px' }}>Bản đồ Trạm Quan trắc</h2>
              <div style={{ flex: 1, minHeight: '400px', borderRadius: 'var(--radius-md)', overflow: 'hidden' }}>
                <MapContainer 
                  key={`${mapCenter[0]}-${mapZoom}`}
                  center={mapCenter} 
                  zoom={mapZoom} 
                  style={{ height: '100%', width: '100%' }}
                >
                  <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                  />
                  {displayedStations.filter(s => s.latitude && s.longitude).map(station => (
                    <Marker key={station.id} position={[station.latitude, station.longitude]}>
                      <Popup>
                        <strong>{station.name}</strong><br />
                        Trạng thái: <span style={{ color: station.status === 'ONLINE' ? 'var(--success)' : 'var(--danger)' }}>{station.status}</span>
                      </Popup>
                    </Marker>
                  ))}
                </MapContainer>
              </div>
            </div>
          </div>
        )}

        {/* DANH SÁCH TRẠM QUAN TRẮC (CHUNG CHO CẢ ADMIN VÀ STAFF) */}
        <div className="glass-panel" style={{ padding: '24px' }}>
          <h2 style={{ fontSize: '1.25rem', marginBottom: '24px' }}>Danh sách Trạm Quan trắc</h2>
          
          {displayedStations.length > 0 ? (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
              {displayedStations.map(st => {
                const stSensors = getSensorsByStation(st.id);
                return (
                  <div key={st.id} className="hover-lift" style={{ backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: 'var(--radius-md)', padding: '24px', border: '1px solid rgba(255,255,255,0.05)', transition: 'all 0.2s' }}>
                    
                    {/* Header thông tin trạm */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '16px' }}>
                      <div>
                        <h3 style={{ fontSize: '1.25rem', fontWeight: 600, color: 'white', marginBottom: '8px' }}>{st.name}</h3>
                        <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                          <span>Mã trạm: <strong style={{ color: 'var(--primary-color)' }}>{st.stationCode}</strong></span>
                          <span>📍 {st.address}</span>
                        </div>
                      </div>
                      <div style={{ padding: '6px 12px', borderRadius: '20px', backgroundColor: st.status === 'ONLINE' ? 'rgba(34, 197, 94, 0.1)' : 'rgba(239, 68, 68, 0.1)', color: st.status === 'ONLINE' ? 'var(--success)' : 'var(--danger)', fontWeight: 'bold', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <div style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: st.status === 'ONLINE' ? 'var(--success)' : 'var(--danger)' }}></div>
                        {st.status}
                      </div>
                    </div>
                    
                    {/* Danh sách Sensor */}
                    {st.status === 'ONLINE' ? (
                      <div>
                        <h4 style={{ fontSize: '0.95rem', color: 'var(--text-secondary)', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <Activity size={16} /> Thông số Cảm biến Real-time
                        </h4>
                        
                        {stSensors.length > 0 ? (
                          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '16px' }}>
                            {stSensors.map(sensor => {
                              const telemetry = realtimeData.find(t => t.sensorId === sensor.id);
                              const currentValue = telemetry?.value;
                              let isOutOfRange = false;
                              if (currentValue !== undefined) {
                                  if (sensor.minValue !== undefined && sensor.minValue !== null && currentValue < sensor.minValue) isOutOfRange = true;
                                  if (sensor.maxValue !== undefined && sensor.maxValue !== null && currentValue > sensor.maxValue) isOutOfRange = true;
                              }
                              const valueColor = isOutOfRange ? '#ef4444' : 'white';

                              return (
                                <div key={sensor.id} style={{ padding: '16px', backgroundColor: 'rgba(0,0,0,0.3)', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '12px', border: isOutOfRange ? '1px solid rgba(239, 68, 68, 0.4)' : '1px solid rgba(255,255,255,0.02)' }}>
                                  <div style={{ padding: '10px', backgroundColor: isOutOfRange ? 'rgba(239, 68, 68, 0.1)' : 'rgba(56, 189, 248, 0.1)', borderRadius: '10px', color: isOutOfRange ? '#ef4444' : '#38bdf8' }}>
                                    {getSensorIcon(sensor.sensorTypeName || '')}
                                  </div>
                                  <div style={{ flex: 1 }}>
                                    <div style={{ fontWeight: '500', fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: '4px' }}>{sensor.name || sensor.sensorTypeName}</div>
                                    <div style={{ display: 'flex', alignItems: 'baseline', gap: '6px' }}>
                                      <span style={{ fontSize: '1.25rem', fontWeight: 700, color: valueColor }}>
                                        {telemetry ? telemetry.value.toFixed(2) : '--'}
                                      </span>
                                      <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                                        {sensor.unit}
                                      </span>
                                    </div>
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        ) : (
                          <div style={{ color: 'var(--text-secondary)', fontStyle: 'italic', fontSize: '0.9rem', padding: '16px', backgroundColor: 'rgba(255,255,255,0.02)', borderRadius: '8px', textAlign: 'center' }}>
                            Trạm chưa lắp đặt cảm biến nào.
                          </div>
                        )}
                      </div>
                    ) : (
                      <div style={{ color: 'var(--danger)', fontStyle: 'italic', fontSize: '0.95rem', padding: '16px', backgroundColor: 'rgba(239,68,68,0.05)', borderRadius: '8px', textAlign: 'center' }}>
                        Trạm đang mất kết nối, không thể tải số liệu!
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <div style={{ padding: '60px', textAlign: 'center', color: 'var(--text-secondary)' }}>
              Không có dữ liệu trạm quan trắc.
            </div>
          )}
        </div>
    </div>
  );
}
