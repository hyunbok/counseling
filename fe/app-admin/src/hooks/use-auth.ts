import { useMutation } from '@tanstack/react-query';
import api from '@/lib/api';
import useAuthStore from '@/stores/auth-store';
import type { AdminRole } from '@/types';

interface LoginParams {
  username: string;
  password: string;
  type: string;
}

interface LoginResponse {
  admin: {
    id: string;
    name?: string;
    username: string;
    role: string;
    tenantId?: string;
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
      const admin = data.admin;
      setAuth(
        {
          id: admin.id,
          name: admin.name ?? admin.username,
          username: admin.username,
          role: admin.role as AdminRole,
          tenantId: admin.tenantId,
        },
        data.accessToken,
        data.refreshToken,
      );
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
