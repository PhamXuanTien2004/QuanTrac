import axios from 'axios';
import { useAuthStore } from '../store/useAuthStore';
import toast from 'react-hot-toast';

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

// Biến cờ để chống việc gọi API refresh token liên tục khi có nhiều request cùng lúc
let isRefreshing = false;
// Hàng đợi lưu các request bị lỗi 401 tạm thời chờ token mới
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // Nếu mã lỗi là 401 (Unauthorized) và request này chưa từng được thử lại
    if (error.response && error.response.status === 401 && !originalRequest._retry) {
      
      // Đang có 1 tiến trình refresh token khác đang chạy, cho vào hàng đợi
      if (isRefreshing) {
        return new Promise(function(resolve, reject) {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers.Authorization = 'Bearer ' + token;
          return api(originalRequest);
        }).catch(err => {
          return Promise.reject(err);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = useAuthStore.getState().refreshToken;
      
      if (!refreshToken) {
        useAuthStore.getState().logout();
        toast.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
        return Promise.reject(error);
      }

      try {
        // Dùng axios gốc (không dùng instance 'api' để tránh vòng lặp vô tận) gọi API refresh
        const res = await axios.post('http://localhost:8180/api/v1/auth/refresh-token', null, {
          params: { refreshToken }
        });
        
        // Lấy token mới từ dữ liệu trả về của Keycloak
        const tokenData = res.data?.data || res.data;
        const newAccessToken = tokenData.access_token || tokenData.accessToken;
        // Keycloak thường trả về luôn refresh token mới, nếu không có thì xài lại cái cũ
        const newRefreshToken = tokenData.refresh_token || tokenData.refreshToken || refreshToken;
        
        // Cập nhật token mới vào kho chứa (Store)
        useAuthStore.setState({ 
          accessToken: newAccessToken,
          refreshToken: newRefreshToken
        });

        // Kích hoạt lại toàn bộ các API đang bị kẹt trong hàng đợi với token mới
        processQueue(null, newAccessToken);
        
        // Thực hiện lại API bị lỗi ban đầu
        originalRequest.headers.Authorization = 'Bearer ' + newAccessToken;
        return api(originalRequest);
        
      } catch (err) {
        // Token làm mới cũng đã hỏng / hết hạn
        processQueue(err, null);
        console.error('Lỗi khi làm mới token (Refresh Token Expired):', err);
        useAuthStore.getState().logout();
        toast.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
        return Promise.reject(err);
      } finally {
        isRefreshing = false; // Mở khóa
      }
    } 
    
    // Nếu không phải 401, hiển thị thông báo lỗi bình thường
    if (error.response && error.response.status !== 401) {
      const errorMsg = error.response?.data?.message || error.response?.data?.error || 'Đã xảy ra lỗi hệ thống';
      toast.error(errorMsg);
    }
    
    return Promise.reject(error);
  }
);

export default api;
