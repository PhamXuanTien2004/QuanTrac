import api from './api';

export interface AqiHistory {
  id: number;
  stationId: string;
  aqiValue: number;
  mainPollutant: string;
  level: string;
  calculatedAt: string;
}

export const notificationApi = {
  getLatestAqi: async (stationId: string) => {
    const response = await api.get(`/notifications/aqi/station/${stationId}/latest`);
    return response.data?.data; // Returns AqiHistory object
  },
  getAqiHistory: async (stationId: string, startTime: string, endTime: string) => {
    const response = await api.get(`/notifications/aqi/station/${stationId}/history`, {
      params: { startTime, endTime }
    });
    return response.data?.data; // Returns AqiHistory[] object
  },
  getLatestAqiAll: async () => {
    const response = await api.get(`/notifications/aqi/latest`);
    return response.data?.data; // Returns AqiHistory[] object
  }
};
