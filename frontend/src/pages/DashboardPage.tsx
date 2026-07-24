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

          {/* DANH SÁCH TRẠM (ADMIN) */}
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
                          <div style={{ transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.3s' }}>▼</div>
                        </div>
                        
                        {isExpanded && (
                          <div style={{ padding: '16px', backgroundColor: 'rgba(255,255,255,0.02)' }}>
                            <h4 style={{ marginBottom: '12px', color: 'var(--text-secondary)' }}>Thông số Cảm biến:</h4>
                            {stSensors.length > 0 ? (
                              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '12px' }}>
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
                                    <div key={sensor.id} style={{ padding: '16px', backgroundColor: 'rgba(0,0,0,0.3)', borderRadius: '12px', display: 'flex', alignItems: 'center', gap: '12px', border: isOutOfRange ? '1px solid rgba(239, 68, 68, 0.3)' : '1px solid rgba(255,255,255,0.05)' }}>
                                      <div style={{ padding: '10px', backgroundColor: isOutOfRange ? 'rgba(239, 68, 68, 0.1)' : 'rgba(56, 189, 248, 0.1)', borderRadius: '10px', color: isOutOfRange ? '#ef4444' : '#38bdf8' }}>
                                        {getSensorIcon(sensor.sensorTypeName || '')}
                                      </div>
                                      <div style={{ flex: 1 }}>
                                        <div style={{ fontWeight: '500', fontSize: '0.9rem' }}>{sensor.name || sensor.sensorTypeName}</div>
                                        <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', marginTop: '4px' }}>
                                          <span style={{ fontSize: '1.25rem', fontWeight: 700, color: valueColor }}>
                                            {telemetry ? telemetry.value.toFixed(2) : '--'}
                                          </span>
                                          {isOutOfRange && (
                                            <span style={{ fontSize: '0.65rem', color: '#ef4444', fontWeight: 600, padding: '2px 4px', backgroundColor: 'rgba(239, 68, 68, 0.1)', borderRadius: '4px' }}>Cảnh báo</span>
                                          )}
                                        </div>
                                      </div>
                                    </div>
                                  );
                                })}
                              </div>
                            ) : (
                              <div style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>Chưa có cảm biến nào được lắp đặt.</div>
                            )}
                            <div style={{ marginTop: '24px' }}>
                              <h4 style={{ marginBottom: '12px', color: 'var(--text-secondary)' }}>Lịch sử dữ liệu (Trạm {st.name}):</h4>
                              <div style={{ height: '300px', borderRadius: 'var(--radius-md)', overflow: 'hidden', backgroundColor: 'rgba(0,0,0,0.3)' }}>
                                <iframe src={`http://localhost:3000/d-solo/ad5x4sl/new-dashboard?orgId=1&timezone=browser&panelId=panel-1&theme=dark&kiosk=tv&var-stationId=${st.id}`} width="100%" height="100%" style={{ border: 'none' }} title={`Grafana Chart ${st.name}`}></iframe>
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
                          <div style={{ transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.3s' }}>▼</div>
                        </div>
                        {isExpanded && (
                          <div style={{ padding: '16px', backgroundColor: 'rgba(255,255,255,0.02)' }}>
                            <div style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>Không thể lấy số liệu vì trạm mất kết nối.</div>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        </>
      ) : (
        <>
          {/* GIAO DIỆN MỚI DÀNH RIÊNG CHO STAFF VÀ MANAGER */}
          {displayedStations.length > 0 ? (
            <div className="glass-panel" style={{ padding: '32px' }}>
              <div style={{ marginBottom: '32px', paddingBottom: '24px', borderBottom: '1px solid var(--border-glass)' }}>
                <h2 style={{ fontSize: '1.75rem', marginBottom: '12px', color: 'white', fontWeight: 700 }}>
                  Trạm: {displayedStations[0].name}
                </h2>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '16px', color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>📍 {displayedStations[0].address}</span>
                  <span>|</span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>Mã trạm: <strong style={{ color: 'white' }}>{displayedStations[0].stationCode}</strong></span>
                  <span>|</span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>Trạng thái: 
                    <span style={{ padding: '2px 8px', borderRadius: '12px', backgroundColor: displayedStations[0].status === 'ONLINE' ? 'rgba(34, 197, 94, 0.1)' : 'rgba(239, 68, 68, 0.1)', color: displayedStations[0].status === 'ONLINE' ? 'var(--success)' : 'var(--danger)', fontWeight: 'bold' }}>
                      {displayedStations[0].status}
                    </span>
                  </span>
                </div>
              </div>

              <h3 style={{ fontSize: '1.25rem', marginBottom: '24px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Activity size={20} color="var(--primary-color)" />
                Chi tiết Cảm biến & Biểu đồ
              </h3>

              {getSensorsByStation(displayedStations[0].id).length > 0 ? (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '24px' }}>
                  {getSensorsByStation(displayedStations[0].id).map(sensor => {
                    const telemetry = realtimeData.find(t => t.sensorId === sensor.id);
                    const currentValue = telemetry?.value;
                    let isOutOfRange = false;
                    
                    if (currentValue !== undefined) {
                        if (sensor.minValue !== undefined && sensor.minValue !== null && currentValue < sensor.minValue) isOutOfRange = true;
                        if (sensor.maxValue !== undefined && sensor.maxValue !== null && currentValue > sensor.maxValue) isOutOfRange = true;
                    }
                    const valueColor = isOutOfRange ? '#ef4444' : 'white';

                    return (
                      <div key={sensor.id} style={{ backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: 'var(--radius-lg)', overflow: 'hidden', border: isOutOfRange ? '1px solid rgba(239, 68, 68, 0.4)' : '1px solid rgba(255,255,255,0.05)' }}>
                        
                        {/* Header của từng Cảm biến */}
                        <div style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', backgroundColor: isOutOfRange ? 'rgba(239, 68, 68, 0.05)' : 'rgba(255,255,255,0.02)', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                            <div style={{ padding: '16px', backgroundColor: isOutOfRange ? 'rgba(239, 68, 68, 0.1)' : 'rgba(56, 189, 248, 0.1)', borderRadius: '12px', color: isOutOfRange ? '#ef4444' : '#38bdf8' }}>
                              {getSensorIcon(sensor.sensorTypeName || '')}
                            </div>
                            <div>
                              <h4 style={{ fontSize: '1.2rem', fontWeight: 600, color: 'white' }}>{sensor.name || sensor.sensorTypeName}</h4>
                              <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginTop: '6px' }}>
                                Mã CB: <strong>{sensor.sensorCode}</strong> | Ngưỡng an toàn: <strong>{sensor.minValue ?? '-'} ~ {sensor.maxValue ?? '-'}</strong>
                              </p>
                            </div>
                          </div>
                          
                          <div style={{ textAlign: 'right' }}>
                            <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px' }}>Giá trị Real-time:</div>
                            <div style={{ display: 'flex', alignItems: 'baseline', gap: '12px', justifyContent: 'flex-end' }}>
                              <span style={{ fontSize: '2.5rem', fontWeight: 800, color: valueColor, textShadow: isOutOfRange ? '0 0 10px rgba(239,68,68,0.5)' : 'none' }}>
                                {telemetry ? telemetry.value.toFixed(2) : '--'}
                              </span>
                              {isOutOfRange && (
                                <span style={{ fontSize: '0.85rem', color: '#ef4444', fontWeight: 600, padding: '4px 10px', backgroundColor: 'rgba(239, 68, 68, 0.15)', borderRadius: '6px' }}>⚠️ Vượt ngưỡng</span>
                              )}
                            </div>
                          </div>
                        </div>

                        {/* Grafana Iframe gắn riêng cho Cảm biến này */}
                        <div style={{ height: '350px', width: '100%', backgroundColor: 'rgba(0,0,0,0.5)' }}>
                          <iframe 
                            src={`http://localhost:3000/d-solo/ad5x4sl/new-dashboard?orgId=1&timezone=browser&panelId=panel-1&theme=dark&kiosk=tv&var-stationId=${displayedStations[0].id}&var-sensorId=${sensor.id}&var-min=${sensor.minValue ?? 0}&var-max=${sensor.maxValue ?? 100}`}
                            width="100%" 
                            height="100%" 
                            style={{ border: 'none' }}
                            title={`Grafana Chart ${sensor.name}`}
                          ></iframe>
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div style={{ padding: '60px', textAlign: 'center', color: 'var(--text-secondary)', backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: 'var(--radius-md)' }}>
                  Trạm này hiện tại chưa có cảm biến nào được khai báo.
                </div>
              )}
            </div>
          ) : (
            <div className="glass-panel" style={{ padding: '60px', textAlign: 'center', color: 'var(--text-secondary)' }}>
              Bạn chưa được phân công quản lý trạm quan trắc nào. Vui lòng liên hệ Admin.
            </div>
          )}
        </>
      )}
    </div>
  );
}
