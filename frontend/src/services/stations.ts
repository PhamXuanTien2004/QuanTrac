import api from './api';

export interface Station {
  id: string;
  stationCode: string;
  name: string;
  description?: string;
  address?: string;
  latitude: number;
  longitude: number;
  installationDate?: string;
  status: 'ONLINE' | 'OFFLINE';
  createdAt?: string;
}

export const stationApi = {
  // Get all stations
  getStations: async (): Promise<Station[]> => {
    const response = await api.get('/stations');
    // device-service usually returns a standard wrapped response like { data: [...] } or just the array.
    // Assuming it returns the array directly or in `data`
    return response.data?.data || response.data;
  },

  // Create new station
  createStation: async (stationData: Partial<Station>): Promise<Station> => {
    const response = await api.post('/stations', stationData);
    return response.data?.data || response.data;
  },

  // Delete station (soft delete via API)
  deleteStation: async (id: string): Promise<void> => {
    await api.delete(`/stations/${id}`);
  },

  // Update station
  updateStation: async (stationData: Partial<Station>): Promise<Station> => {
    const response = await api.put(`/stations`, stationData);
    return response.data?.data || response.data;
  }
};
