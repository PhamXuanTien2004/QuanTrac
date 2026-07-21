import axios from 'axios';
import { useAuthStore } from '../store/useAuthStore';

const api = axios.create({
  baseURL: 'http://localhost:8180/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().accessToken;
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      console.error('Unauthorized access - token missing or expired');
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  }
);

export default api;
