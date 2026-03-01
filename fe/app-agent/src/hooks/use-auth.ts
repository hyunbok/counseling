import { useMutation } from '@tanstack/react-query';
import api from '@/lib/api';
import useAuthStore from '@/stores/auth-store';

interface LoginParams {
  username: string;
  password: string;
}

interface LoginResponse {
  user: {
    id: string;
    name: string;
    username: string;
    role: string;
    groupName?: string;
  };
  accessToken: string;
  refreshToken: string;
}

export const useLogin = () => {
  const setAuth = useAuthStore((state) => state.setAuth);

  return useMutation({
    mutationFn: async (params: LoginParams): Promise<LoginResponse> => {
      const { data } = await api.post<LoginResponse>('/api/auth/login', params);
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
      await api.post('/api/auth/logout');
    },
    onSettled: () => {
      logout();
    },
  });
};
