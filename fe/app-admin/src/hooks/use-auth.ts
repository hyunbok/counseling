import { useMutation } from '@tanstack/react-query';
import api from '@/lib/api';
import useAuthStore from '@/stores/auth-store';
import type { AdminRole } from '@/types';

interface LoginParams {
  username: string;
  password: string;
}

interface LoginResponse {
  user: {
    id: string;
    name: string;
    username: string;
    role: AdminRole;
    tenantId?: string;
    tenantName?: string;
  };
  accessToken: string;
  refreshToken: string;
}

export const useLogin = () => {
  const setAuth = useAuthStore((state) => state.setAuth);

  return useMutation({
    mutationFn: async (params: LoginParams): Promise<LoginResponse> => {
      const { data } = await api.post<LoginResponse>('/api-adm/auth/login', params);
      return data;
    },
    onSuccess: (data) => {
      setAuth(data.user, data.accessToken, data.refreshToken);
    },
  });
};

export const useLogout = () => {
  const logout = useAuthStore((state) => state.logout);

  return useMutation({
    mutationFn: async () => {
      await api.post('/api-adm/auth/logout');
    },
    onSettled: () => {
      logout();
    },
  });
};
