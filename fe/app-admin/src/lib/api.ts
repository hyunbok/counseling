import axios from 'axios';
import useAuthStore from '@/stores/auth-store';

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8081';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach Bearer token on every request
api.interceptors.request.use((config) => {
  const accessToken = useAuthStore.getState().accessToken;
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// On 401: try token refresh, on failure logout and redirect to /login
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
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
