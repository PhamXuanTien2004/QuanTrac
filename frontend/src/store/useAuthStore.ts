import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import api from '../services/api';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  user: any | null;
  isLoading: boolean;
  error: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  clearError: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      user: null,
      isLoading: false,
      error: null,

      login: async (username, password) => {
        set({ isLoading: true, error: null });
        try {
          // Gửi request login tới auth-service qua Gateway
          const response = await api.post('/auth/login', { username, password });
          
          // Trả về TokenResponse từ BaseResponse<TokenResponse>
          const tokenData = response.data?.data || response.data;
          
          if (tokenData && tokenData.access_token) {
            let role = 'ROLE_STAFF';
            let stationId = null;
            try {
              const base64Url = tokenData.access_token.split('.')[1];
              const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
              const payload = JSON.parse(window.atob(base64));
              
              let roles: string[] = [];
              if (payload?.realm_access?.roles) {
                roles = roles.concat(payload.realm_access.roles);
              }
              if (payload?.resource_access) {
                Object.values(payload.resource_access).forEach((client: any) => {
                  if (client?.roles && Array.isArray(client.roles)) {
                    roles = roles.concat(client.roles);
                  }
                });
              }
              if (payload?.roles && Array.isArray(payload.roles)) {
                roles = roles.concat(payload.roles);
              }

              if (roles.some((r: string) => r.toUpperCase().includes('ADMIN'))) {
                role = 'ROLE_ADMIN';
              } else if (roles.some((r: string) => r.toUpperCase().includes('MANAGER'))) {
                role = 'ROLE_MANAGER';
              } else {
                role = 'ROLE_STAFF';
              }
              stationId = payload?.station_id || (Array.isArray(payload?.station_id) ? payload.station_id[0] : null);
            } catch (e) {
              console.warn('Failed to parse JWT payload', e);
            }

            set({
              accessToken: tokenData.access_token,
              refreshToken: tokenData.refresh_token,
              isAuthenticated: true,
              user: { username, role, stationId },
              isLoading: false,
            });

            // Lấy stationId chuẩn từ CSDL (đề phòng token thiếu)
            try {
              const uRes = await api.get('/users');
              const uList = Array.isArray(uRes.data?.data) ? uRes.data.data : (Array.isArray(uRes.data) ? uRes.data : []);
              const dbUser = uList.find((u: any) => u.username === username);
              if (dbUser && dbUser.stationId) {
                set((state) => ({ user: { ...state.user!, stationId: dbUser.stationId } }));
              }
            } catch (e) {
              console.warn('Failed to fetch DB user for stationId mapping', e);
            }
          } else {
            throw new Error('Dữ liệu token không hợp lệ');
          }
        } catch (error: any) {
          set({
            error: error.response?.data?.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại tài khoản.',
            isLoading: false,
            isAuthenticated: false,
          });
          throw error;
        }
      },

      logout: () => {
        set({
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
          user: null,
          error: null,
        });
      },

      clearError: () => set({ error: null }),
    }),
    {
      name: 'auth-storage', // Lưu token vào localStorage
      partialize: (state) => ({ accessToken: state.accessToken, refreshToken: state.refreshToken, isAuthenticated: state.isAuthenticated, user: state.user }),
    }
  )
);
