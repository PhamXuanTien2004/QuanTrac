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
  stompClient: Client | null;
  fetchRealtimeData: (stationId: string) => Promise<void>;
  fetchHistoricalData: (stationId: string, startTime: string, endTime: string) => Promise<void>;
  connectWebSocket: (stationId: string) => void;
  disconnectWebSocket: () => void;
}

export const useTelemetryStore = create<TelemetryState>((set, get) => ({
  realtimeData: [],
  historicalData: [],
  isLoadingRealtime: false,
  isLoadingHistory: false,
  error: null,
  stompClient: null,

  fetchRealtimeData: async (stationId: string) => {
    set({ isLoadingRealtime: true, error: null });
    try {
      const response = await api.get(`/telemetry/realtime?stationId=${stationId}`);
      set({ realtimeData: response.data || [], isLoadingRealtime: false });
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

  connectWebSocket: (stationId: string) => {
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
      client.subscribe(`/topic/station/${stationId}`, (message) => {
        if (message.body) {
          const newData: TelemetryData = JSON.parse(message.body);
          
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

