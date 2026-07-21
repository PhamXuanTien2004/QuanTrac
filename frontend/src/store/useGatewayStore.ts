import { create } from 'zustand';
import api from '../services/api';

export interface Gateway {
  id: string;
  stationId: string;
  code: string;
  serialNumber?: string;
  model?: string;
  firmwareVersion?: string;
  ipAddress?: string;
  macAddress?: string;
  lastSeen?: string;
  status: 'ONLINE' | 'OFFLINE' | 'WARNING';
}

interface GatewayState {
  gateways: Gateway[];
  isLoading: boolean;
  error: string | null;
  fetchGateways: () => Promise<void>;
  addGateway: (gatewayData: Partial<Gateway>) => Promise<void>;
  updateGateway: (gatewayData: Partial<Gateway>) => Promise<void>;
  removeGateway: (id: string) => Promise<void>;
}

export const useGatewayStore = create<GatewayState>((set, get) => ({
  gateways: [],
  isLoading: false,
  error: null,

  fetchGateways: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await api.post('/gateways/filter', {});
      const data = response.data.data?.content || response.data.content || response.data.data || response.data;
      set({ gateways: data, isLoading: false });
    } catch (err: any) {
      set({ error: err.message || 'Lỗi tải danh sách Gateway', isLoading: false });
    }
  },

  addGateway: async (gatewayData) => {
    set({ isLoading: true, error: null });
    try {
      const response = await api.post('/gateways', gatewayData);
      const newData = response.data.data || response.data;
      set((state) => ({ 
        gateways: [...state.gateways, newData], 
        isLoading: false 
      }));
    } catch (err: any) {
      set({ error: err.response?.data?.message || err.message || 'Lỗi thêm Gateway', isLoading: false });
      throw err;
    }
  },

  removeGateway: async (id) => {
    set({ isLoading: true, error: null });
    try {
      await api.delete(`/gateways/${id}`);
      set((state) => ({
        gateways: state.gateways.filter(s => s.id !== id),
        isLoading: false
      }));
    } catch (err: any) {
      set({ error: err.response?.data?.message || err.message || 'Lỗi xóa Gateway', isLoading: false });
      throw err;
    }
  },

  updateGateway: async (gatewayData) => {
    set({ isLoading: true, error: null });
    try {
      const response = await api.put('/gateways/update', gatewayData);
      const updatedData = response.data.data || response.data;
      set((state) => ({
        gateways: state.gateways.map(g => g.id === updatedData.id ? updatedData : g),
        isLoading: false
      }));
    } catch (err: any) {
      set({ error: err.response?.data?.message || err.message || 'Lỗi cập nhật Gateway', isLoading: false });
      throw err;
    }
  }
}));
