import { create } from 'zustand';
import { stationApi } from '../services/stations';
import type { Station } from '../services/stations';

interface StationState {
  stations: Station[];
  isLoading: boolean;
  error: string | null;
  fetchStations: () => Promise<void>;
  addStation: (data: Partial<Station>) => Promise<void>;
  updateStation: (data: Partial<Station>) => Promise<void>;
  removeStation: (id: string) => Promise<void>;
}

export const useStationStore = create<StationState>((set) => ({
  stations: [],
  isLoading: false,
  error: null,

  fetchStations: async () => {
    set({ isLoading: true, error: null });
    try {
      const data = await stationApi.getStations();
      const sortedData = Array.isArray(data) ? [...data].sort((a: any, b: any) => (a.name || '').localeCompare(b.name || '', 'vi')) : data;
      set({ stations: sortedData, isLoading: false });
    } catch (error: any) {
      set({ 
        error: error.response?.data?.message || 'Không thể tải danh sách trạm. Vui lòng kiểm tra kết nối Backend.',
        isLoading: false 
      });
    }
  },

  addStation: async (data: Partial<Station>) => {
    set({ isLoading: true, error: null });
    try {
      const newStationResponse = await stationApi.createStation(data);
      // Wait, stationApi.createStation might already extract data. Let's check stations.ts!
      // In stations.ts: return response.data?.data || response.data;
      // So newStationResponse IS the actual station! We just push it.
      set((state) => ({ 
        stations: [...state.stations, newStationResponse],
        isLoading: false 
      }));
    } catch (error: any) {
      set({ 
        error: error.response?.data?.message || error.message || 'Không thể tạo trạm mới.',
        isLoading: false 
      });
      throw error;
    }
  },

  removeStation: async (id: string) => {
    set({ isLoading: true, error: null });
    try {
      await stationApi.deleteStation(id);
      set((state) => ({
        stations: state.stations.filter(s => s.id !== id),
        isLoading: false
      }));
    } catch (error: any) {
      set({ 
        error: error.response?.data?.message || error.message || 'Không thể xóa trạm.',
        isLoading: false 
      });
      throw error;
    }
  },

  updateStation: async (data: Partial<Station>) => {
    set({ isLoading: true, error: null });
    try {
      const updatedStation = await stationApi.updateStation(data);
      set((state) => ({
        stations: state.stations.map(s => s.id === updatedStation.id ? updatedStation : s),
        isLoading: false
      }));
    } catch (error: any) {
      set({ 
        error: error.response?.data?.message || 'Không thể cập nhật trạm.',
        isLoading: false 
      });
      throw error;
    }
  }
}));
