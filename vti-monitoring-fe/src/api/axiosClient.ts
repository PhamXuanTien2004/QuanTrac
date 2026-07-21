import axios from 'axios';
import keycloak from '../keycloak';

const axiosClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1', // Đi qua API Gateway
  headers: { 'Content-Type': 'application/json' },
});

axiosClient.interceptors.request.use(
  async (config) => {
    if (keycloak.token) {
      try {
        await keycloak.updateToken(30); // Tự làm mới token nếu sắp hết hạn
        config.headers.Authorization = `Bearer ${keycloak.token}`;
      } catch (error) {
        console.error("Token refresh failed", error);
        keycloak.logout();
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export default axiosClient;