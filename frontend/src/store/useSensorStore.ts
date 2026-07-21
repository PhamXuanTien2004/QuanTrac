import { create } from 'zustand';
import api from '../services/api';

export interface Sensor {
  id: string;
  sensorCode: string;
  name: string;
  gatewayId?: string; // used for mapping back, or gatewayCode
  gatewayCode?: string;
  sensorTypeId?: string; // used for mapping back, or sensorTypeName
  sensorTypeName?: string;
  model?: string;
  manufacturer?: string;
  installationDate?: string;
  calibrationDate?: string;
  minValue?: number;
  maxValue?: number;
  status: 'ONLINE' | 'OFFLINE' | 'WARNING';
  lastReading?: number;
  lastReadingTime?: string;
}

interface SensorState {
  sensors: Sensor[];
  isLoading: boolean;
  error: string | null;
  fetchSensors: () => Promise<void>;
  addSensor: (sensorData: Partial<Sensor>) => Promise<void>;
  updateSensor: (sensorData: Partial<Sensor>) => Promise<void>;
  removeSensor: (id: string) => Promise<void>;
}

export const useSensorStore = create<SensorState>((set, get) => ({
  sensors: [],
  isLoading: false,
  error: null,

  fetchSensors: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await api.post('/sensors/filter', {});
      const data = response.data.data?.content || response.data.content || response.data.data || response.data;
      set({ sensors: data, isLoading: false });
    } catch (err: any) {
      set({ error: err.message || 'Lỗi tải danh sách Sensor', isLoading: false });
    }
  },

  addSensor: async (sensorData) => {
    set({ isLoading: true, error: null });
    try {
      const response = await api.post('/sensors', sensorData);
      const newData = response.data.data || response.data;
      set((state) => ({ 
        sensors: [...state.sensors, newData], 
        isLoading: false 
      }));
    } catch (err: any) {
      set({ error: err.response?.data?.message || err.message || 'Lỗi thêm Sensor', isLoading: false });
      throw err;
    }
  },

  removeSensor: async (id) => {
    set({ isLoading: true, error: null });
    try {
      await api.delete(`/sensors/${id}`);
      set((state) => ({
        sensors: state.sensors.filter(s => s.id !== id),
        isLoading: false
      }));
    } catch (err: any) {
      set({ error: err.response?.data?.message || err.message || 'Lỗi xóa Sensor', isLoading: false });
      throw err;
    }
  },

  updateSensor: async (sensorData) => {
    set({ isLoading: true, error: null });
    try {
      const response = await api.put('/sensors', sensorData);
      const updatedData = response.data.data || response.data;
      set((state) => ({
        sensors: state.sensors.map(s => s.id === updatedData.id ? updatedData : s),
        isLoading: false
      }));
    } catch (err: any) {
      set({ error: err.response?.data?.message || err.message || 'Lỗi cập nhật Sensor', isLoading: false });
      throw err;
    }
  }
}));
