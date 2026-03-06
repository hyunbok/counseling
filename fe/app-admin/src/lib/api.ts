import axios from 'axios';
import useAuthStore from '@/stores/auth-store';

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8081';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach Bearer token and X-Tenant-Id on every request
api.interceptors.request.use((config) => {
  const { accessToken, user, selectedTenantId } = useAuthStore.getState();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  if (user?.role === 'SUPER_ADMIN') {
    if (selectedTenantId) {
      config.headers['X-Tenant-Id'] = selectedTenantId;
    }
  } else if (user?.tenantId) {
    config.headers['X-Tenant-Id'] = user.tenantId;
  }
  return config;
});

// On 401: try token refresh, on failure logout and redirect to /login
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const isLoginRequest = originalRequest.url?.includes('/auth/login');
    if (error.response?.status === 401 && !originalRequest._retry && !isLoginRequest) {
      originalRequest._retry = true;
      const { refreshToken, setTokens, logout } = useAuthStore.getState();
      if (refreshToken) {
        try {
          const { data } = await axios.post(`${API_BASE}/api-adm/auth/refresh`, { refreshToken });
          setTokens(data.accessToken, data.refreshToken);
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
          return api(originalRequest);
        } catch {
          logout();
          if (typeof window !== 'undefined') {
            window.location.href = '/login';
          }
        }
      } else {
        logout();
        if (typeof window !== 'undefined') {
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  },
);

export default api;
