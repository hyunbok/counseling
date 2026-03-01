import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { AdminRole } from '@/types';

interface User {
  id: string;
  name: string;
  username: string;
  role: AdminRole;
  tenantId?: string;
  tenantName?: string;
}

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  setAuth: (user: User, accessToken: string, refreshToken: string) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  logout: () => void;
}

const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      setAuth: (user, accessToken, refreshToken) => set({ user, accessToken, refreshToken }),
      setTokens: (accessToken, refreshToken) => set({ accessToken, refreshToken }),
      logout: () => set({ user: null, accessToken: null, refreshToken: null }),
    }),
    {
      name: 'admin-auth-store',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ user: state.user, accessToken: state.accessToken }),
    },
  ),
);

export default useAuthStore;
