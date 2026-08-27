import { create } from 'zustand';
import api from '../services/api';

export interface SensorType {
  id: string;
  code: string;
  name: string;
  unit: string;
  minRange: number;
  maxRange: number;
  description?: string;
}

interface SensorTypeState {
  sensorTypes: SensorType[];
  isLoading: boolean;
  error: string | null;
  fetchSensorTypes: () => Promise<void>;
  addSensorType: (sensorTypeData: Partial<SensorType>) => Promise<void>;
  updateSensorType: (sensorTypeData: Partial<SensorType>) => Promise<void>;
  removeSensorType: (id: string) => Promise<void>;
}

export const useSensorTypeStore = create<SensorTypeState>((set, get) => ({
  sensorTypes: [],
  isLoading: false,
  error: null,

  fetchSensorTypes: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await api.post('/sensor-types/filter', { size: 1000 });
      const data = response.data.data?.content || response.data.content || response.data.data || response.data;
      set({ sensorTypes: data, isLoading: false });
    } catch (err: any) {
      set({ error: err.message || 'Lỗi tải danh sách Loại Sensor', isLoading: false });
    }
  },

  addSensorType: async (sensorTypeData) => {
    set({ isLoading: true, error: null });
    try {
      const response = await api.post('/sensor-types', sensorTypeData);
      const newData = response.data.data || response.data;
      set((state) => ({ 
        sensorTypes: [...state.sensorTypes, newData], 
        isLoading: false 
      }));
    } catch (err: any) {
      set({ error: err.response?.data?.message || err.message || 'Lỗi thêm Loại Sensor', isLoading: false });
      throw err;
    }
  },

  removeSensorType: async (id) => {
    set({ isLoading: true, error: null });
    try {
      await api.delete(`/sensor-types/${id}`);
      set((state) => ({
        sensorTypes: state.sensorTypes.filter(s => s.id !== id),
        isLoading: false
      }));
    } catch (err: any) {
      set({ error: err.response?.data?.message || err.message || 'Lỗi xóa Loại Sensor', isLoading: false });
      throw err;
    }
  },

  updateSensorType: async (sensorTypeData) => {
    set({ isLoading: true, error: null });
    try {
      const response = await api.put('/sensor-types', sensorTypeData);
      const updatedData = response.data.data || response.data;
      set((state) => ({
        sensorTypes: state.sensorTypes.map(s => s.id === updatedData.id ? updatedData : s),
        isLoading: false
      }));
    } catch (err: any) {
      set({ error: err.response?.data?.message || err.message || 'Lỗi cập nhật Loại Sensor', isLoading: false });
      throw err;
    }
  }
}));
