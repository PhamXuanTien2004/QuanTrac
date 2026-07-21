import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { useEffect, useState, useMemo } from 'react';
import { useStationStore } from '../store/useStationStore';
import { useAuthStore } from '../store/useAuthStore';
import { useGatewayStore } from '../store/useGatewayStore';
import { useSensorStore } from '../store/useSensorStore';

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

  const userRole = user?.role || 'ROLE_STAFF';
  const isAdmin = userRole === 'ROLE_ADMIN' || userRole === 'Admin';
  const userStationId = user?.stationId;

  const [mapCenter, setMapCenter] = useState<[number, number]>([21.0285, 105.8542]);
  const [mapZoom, setMapZoom] = useState(13);
  
  // Trạng thái cho Accordion (Danh sách trạm của Admin)
  const [expandedStationId, setExpandedStationId] = useState<string | null>(null);

  useEffect(() => {
    fetchStations();
    // Admin cần load cả gateway và sensor để hiển thị trong list danh sách trạm
    fetchGateways();
    fetchSensors();
  }, [fetchStations, fetchGateways, fetchSensors]);

  // Ensure the map resizes correctly
  useEffect(() => {
    window.dispatchEvent(new Event('resize'));
  }, [stations]);

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
  
  let grafanaUrl = "http://localhost:3000/d-solo/adj5lfz/test?orgId=1&from=now-15m&to=now&timezone=browser&var-datasource0=ffsqx9mqb79q8a&refresh=5s&panelId=panel-1&theme=dark&kiosk=tv";
  if (!isAdmin && hasActiveDevices) {
    grafanaUrl += `&var-stationId=${activeStationId}&var-gatewayId=${activeGatewayId}&var-status=ONLINE`;
  }

  const toggleStation = (stId: string) => {
    setExpandedStationId(prev => prev === stId ? null : stId);
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
      
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
        {/* Bản đồ vị trí trạm */}
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

        {/* Biểu đồ Grafana (nhúng qua iFrame) */}
        <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column' }}>
          <h2 style={{ fontSize: '1.25rem', marginBottom: '16px' }}>Biểu đồ Thời gian thực (Grafana)</h2>
          <div style={{ flex: 1, minHeight: '400px', borderRadius: 'var(--radius-md)', overflow: 'hidden', backgroundColor: 'rgba(0,0,0,0.2)' }}>
            {hasActiveDevices ? (
              <iframe 
                src={grafanaUrl} 
                width="100%" 
                height="100%" 
                style={{ border: 'none' }}
                title="Grafana Dashboard"
              ></iframe>
            ) : (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', padding: '20px', textAlign: 'center' }}>
                <div>
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--warning)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ marginBottom: '16px' }}>
                    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                    <line x1="12" y1="9" x2="12" y2="13"></line>
                    <line x1="12" y1="17" x2="12.01" y2="17"></line>
                  </svg>
                  <h3 style={{ color: 'var(--text-secondary)' }}>Không có thiết bị nào đang hoạt động</h3>
                  <p style={{ marginTop: '8px', color: 'var(--text-secondary)', opacity: 0.7 }}>
                    Hệ thống không tìm thấy Gateway hoặc Sensor nào đang ONLINE thuộc quyền quản lý của bạn. Vui lòng kiểm tra lại thiết bị phần cứng để tiếp tục xem dữ liệu thời gian thực.
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Danh sách Trạm (Dành riêng cho Admin) */}
      {isAdmin && (
        <div className="glass-panel" style={{ padding: '24px' }}>
          <h2 style={{ fontSize: '1.25rem', marginBottom: '24px' }}>Danh sách Trạm Quan trắc (Admin)</h2>
          
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
            {/* Trạm Đang Hoạt Động */}
            <div>
              <h3 style={{ color: 'var(--success)', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <div style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: 'var(--success)' }}></div>
                Trạm Đang Hoạt Động ({onlineStationsList.length})
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {onlineStationsList.map(st => {
                  const stSensors = getSensorsByStation(st.id);
                  const isExpanded = expandedStationId === st.id;
                  
                  return (
                    <div key={st.id} style={{ backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: 'var(--radius-md)', overflow: 'hidden' }}>
                      <div 
                        onClick={() => toggleStation(st.id)}
                        style={{ padding: '16px', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: isExpanded ? '1px solid rgba(255,255,255,0.1)' : 'none' }}
                      >
                        <div>
                          <strong style={{ fontSize: '1.1rem' }}>{st.name}</strong>
                          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '4px' }}>Mã: {st.stationCode}</div>
                        </div>
                        <div style={{ transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.3s' }}>
                          ▼
                        </div>
                      </div>
                      
                      {isExpanded && (
                        <div style={{ padding: '16px', backgroundColor: 'rgba(255,255,255,0.02)' }}>
                          <h4 style={{ marginBottom: '12px', color: 'var(--text-secondary)' }}>Thông số Cảm biến:</h4>
                          {stSensors.length > 0 ? (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                              {stSensors.map(sensor => (
                                <div key={sensor.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px', backgroundColor: 'rgba(0,0,0,0.3)', borderRadius: '4px' }}>
                                  <div>
                                    <div style={{ fontWeight: '500' }}>{sensor.name || sensor.sensorTypeName}</div>
                                    <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Mã: {sensor.sensorCode}</div>
                                  </div>
                                  <div style={{ textAlign: 'right' }}>
                                    <div style={{ color: sensor.status === 'ONLINE' ? 'var(--success)' : 'var(--danger)', fontSize: '0.9rem', fontWeight: 'bold' }}>
                                      {sensor.status}
                                    </div>
                                    <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                                      Ngưỡng: {sensor.minValue ?? '-'} ~ {sensor.maxValue ?? '-'}
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <div style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>Chưa có cảm biến nào được lắp đặt.</div>
                          )}
                          
                          {/* Biểu đồ Grafana động cho Trạm này */}
                          <div style={{ marginTop: '24px' }}>
                            <h4 style={{ marginBottom: '12px', color: 'var(--text-secondary)' }}>Biểu đồ Thời gian thực (Trạm {st.name}):</h4>
                            <div style={{ height: '300px', borderRadius: 'var(--radius-md)', overflow: 'hidden', backgroundColor: 'rgba(0,0,0,0.3)' }}>
                              <iframe 
                                src={`http://localhost:3000/d-solo/adj5lfz/test?orgId=1&from=now-15m&to=now&timezone=browser&var-datasource0=ffsqx9mqb79q8a&refresh=5s&panelId=panel-1&theme=dark&kiosk=tv&var-stationId=${st.id}`}
                                width="100%" 
                                height="100%" 
                                style={{ border: 'none' }}
                                title={`Grafana Chart ${st.name}`}
                              ></iframe>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Trạm Mất Kết Nối */}
            <div>
              <h3 style={{ color: 'var(--danger)', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <div style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: 'var(--danger)' }}></div>
                Trạm Mất Kết Nối ({offlineStationsList.length})
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {offlineStationsList.map(st => {
                  const stSensors = getSensorsByStation(st.id);
                  const isExpanded = expandedStationId === st.id;
                  
                  return (
                    <div key={st.id} style={{ backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: 'var(--radius-md)', overflow: 'hidden' }}>
                      <div 
                        onClick={() => toggleStation(st.id)}
                        style={{ padding: '16px', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: isExpanded ? '1px solid rgba(255,255,255,0.1)' : 'none' }}
                      >
                        <div>
                          <strong style={{ fontSize: '1.1rem' }}>{st.name}</strong>
                          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '4px' }}>Mã: {st.stationCode}</div>
                        </div>
                        <div style={{ transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.3s' }}>
                          ▼
                        </div>
                      </div>
                      
                      {isExpanded && (
                        <div style={{ padding: '16px', backgroundColor: 'rgba(255,255,255,0.02)' }}>
                          <h4 style={{ marginBottom: '12px', color: 'var(--text-secondary)' }}>Thông số Cảm biến:</h4>
                          {stSensors.length > 0 ? (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                              {stSensors.map(sensor => (
                                <div key={sensor.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px', backgroundColor: 'rgba(0,0,0,0.3)', borderRadius: '4px' }}>
                                  <div>
                                    <div style={{ fontWeight: '500' }}>{sensor.name || sensor.sensorTypeName}</div>
                                    <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Mã: {sensor.sensorCode}</div>
                                  </div>
                                  <div style={{ textAlign: 'right' }}>
                                    <div style={{ color: sensor.status === 'ONLINE' ? 'var(--success)' : 'var(--danger)', fontSize: '0.9rem', fontWeight: 'bold' }}>
                                      {sensor.status}
                                    </div>
                                    <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                                      Ngưỡng: {sensor.minValue ?? '-'} ~ {sensor.maxValue ?? '-'}
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <div style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>Chưa có cảm biến nào được lắp đặt.</div>
                          )}
                          
                          {/* Biểu đồ Grafana động cho Trạm này */}
                          <div style={{ marginTop: '24px' }}>
                            <h4 style={{ marginBottom: '12px', color: 'var(--text-secondary)' }}>Lịch sử dữ liệu (Trạm {st.name}):</h4>
                            <div style={{ height: '300px', borderRadius: 'var(--radius-md)', overflow: 'hidden', backgroundColor: 'rgba(0,0,0,0.3)' }}>
                              <iframe 
                                src={`http://localhost:3000/d-solo/adj5lfz/test?orgId=1&from=now-15m&to=now&timezone=browser&var-datasource0=ffsqx9mqb79q8a&refresh=5s&panelId=panel-1&theme=dark&kiosk=tv&var-stationId=${st.id}`}
                                width="100%" 
                                height="100%" 
                                style={{ border: 'none' }}
                                title={`Grafana Chart ${st.name}`}
                              ></iframe>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
