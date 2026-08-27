import { create } from 'zustand';
import api from '../services/api';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface TelemetryData {
  sensorId: string;
  sensorType: string;
  value: number;
  timestamp: string;
}

interface TelemetryState {
  realtimeData: TelemetryData[];
  historicalData: TelemetryData[];
  isLoadingRealtime: boolean;
  isLoadingHistory: boolean;
  error: string | null;
  realtimeAqi: Record<string, any>;
  stompClient: Client | null;
  fetchRealtimeData: (stationIds: string | string[]) => Promise<void>;
  fetchHistoricalData: (stationId: string, startTime: string, endTime: string) => Promise<void>;
  connectWebSocket: (stationIds: string | string[]) => void;
  disconnectWebSocket: () => void;
}

export const useTelemetryStore = create<TelemetryState>((set, get) => ({
  realtimeData: [],
  historicalData: [],
  isLoadingRealtime: false,
  isLoadingHistory: false,
  error: null,
  realtimeAqi: {},
  stompClient: null,

  fetchRealtimeData: async (stationIds: string | string[]) => {
    set({ isLoadingRealtime: true, error: null });
    try {
      const ids = Array.isArray(stationIds) ? stationIds : [stationIds];
      const promises = ids.map(id => api.get(`/telemetry/realtime?stationId=${id}`));
      const responses = await Promise.all(promises);
      const allData = responses.flatMap(res => res.data || []);
      set({ realtimeData: allData, isLoadingRealtime: false });
    } catch (err: any) {
      set({ error: err.response?.data?.message || err.message || 'Lỗi tải dữ liệu thời gian thực', isLoadingRealtime: false });
    }
  },

  fetchHistoricalData: async (stationId: string, startTime: string, endTime: string) => {
    set({ isLoadingHistory: true, error: null });
    try {
      const response = await api.get(`/telemetry/history?stationId=${stationId}&startTime=${startTime}&endTime=${endTime}`);
      set({ historicalData: response.data || [], isLoadingHistory: false });
    } catch (err: any) {
      set({ error: err.response?.data?.message || err.message || 'Lỗi tải dữ liệu lịch sử', isLoadingHistory: false });
    }
  },

  connectWebSocket: (stationIds: string | string[]) => {
    const currentClient = get().stompClient;
    if (currentClient && currentClient.active) {
      currentClient.deactivate();
    }

    const socketUrl = 'http://localhost:8180/ws/telemetry';
    
    const client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      console.log('Connected to WebSocket');
      const ids = Array.isArray(stationIds) ? stationIds : [stationIds];
      
      ids.forEach(stationId => {
        // Lắng nghe dữ liệu telemetry bình thường
        client.subscribe(`/topic/station/${stationId}`, (message) => {
          if (message.body) {
            const newData: TelemetryData = JSON.parse(message.body);
            
            // --- BẮT ĐẦU ĐO ĐỘ TRỄ ---
            const receiveTime = Date.now();
            let sendTime = 0;
            if (Array.isArray(newData.timestamp)) {
                // Backend gửi mảng [year, month, day, hour, minute, second, nano]
                const [y, m, d, h, min, s, nano] = newData.timestamp;
                sendTime = new Date(y, m - 1, d, h, min, s, (nano || 0) / 1000000).getTime();
            } else if (typeof newData.timestamp === 'string') {
                sendTime = new Date(newData.timestamp).getTime();
            } else {
                sendTime = Number(newData.timestamp);
            }

            if (sendTime > 1000000000) { 
                const latency = receiveTime - sendTime;
                if (latency > 0 && latency < 5000) {
                    (window as any).latencyData = (window as any).latencyData || [];
                    (window as any).latencyData.push(latency);
                    console.log(`[Đo độ trễ] Bản tin #${(window as any).latencyData.length}: ${latency} ms`);
                    
                    if ((window as any).latencyData.length === 100) {
                        console.log("=== ĐÃ THU THẬP ĐỦ 100 MẪU, HÃY COPY MẢNG DƯỚI ĐÂY BỎ VÀO PYTHON ===");
                        console.log(JSON.stringify((window as any).latencyData));
                    }
                }
            }
            // --- KẾT THÚC ĐO ĐỘ TRỄ ---

            set((state) => {
              const existingData = [...state.realtimeData];
              const index = existingData.findIndex(d => d.sensorId === newData.sensorId);
              if (index !== -1) {
                existingData[index] = newData;
              } else {
                existingData.push(newData);
              }
              return { realtimeData: existingData };
            });
          }
        });

        // Lắng nghe dữ liệu AQI thời gian thực (Để tự cập nhật đồng hồ AQI)
        client.subscribe(`/topic/aqi/station/${stationId}`, (message) => {
          if (message.body) {
            try {
               const aqiData = JSON.parse(message.body);
               set((state) => ({
                 realtimeAqi: {
                   ...state.realtimeAqi,
                   [stationId]: aqiData
                 }
               }));
            } catch(e) {
               console.error('Lỗi khi parse aqi update message', e);
            }
          }
        });

        // Lắng nghe dữ liệu cảnh báo khẩn cấp
        client.subscribe(`/topic/alerts/station/${stationId}`, (message) => {
          if (message.body) {
            try {
               const alertData = JSON.parse(message.body);
               // Hiển thị thông báo Toast khẩn cấp
               import('react-hot-toast').then(({ default: toast }) => {
                  toast.error(alertData.message || 'Phát hiện dữ liệu bất thường!', {
                    duration: 8000,
                    position: 'top-right',
                    style: {
                      background: '#ef4444',
                      color: '#fff',
                      fontWeight: 'bold',
                      padding: '16px',
                      borderRadius: '8px',
                      boxShadow: '0 10px 15px -3px rgba(239, 68, 68, 0.4)'
                    },
                  });
               });
            } catch(e) {
               console.error('Lỗi khi parse alert message', e);
            }
          }
        });
      });
    };

    client.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    client.activate();
    set({ stompClient: client });
  },

  disconnectWebSocket: () => {
    const currentClient = get().stompClient;
    if (currentClient && currentClient.active) {
      currentClient.deactivate();
    }
    set({ stompClient: null });
  }
}));

