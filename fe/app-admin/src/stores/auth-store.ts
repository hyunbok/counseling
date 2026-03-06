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
  selectedTenantId: string | null;
  setAuth: (user: User, accessToken: string, refreshToken: string) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  setSelectedTenant: (tenantId: string | null) => void;
  logout: () => void;
}

const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      selectedTenantId: null,
      setAuth: (user, accessToken, refreshToken) => set({ user, accessToken, refreshToken }),
      setTokens: (accessToken, refreshToken) => set({ accessToken, refreshToken }),
      setSelectedTenant: (tenantId) => set({ selectedTenantId: tenantId }),
      logout: () => set({ user: null, accessToken: null, refreshToken: null, selectedTenantId: null }),
    }),
    {
      name: 'admin-auth-store',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ user: state.user, accessToken: state.accessToken, selectedTenantId: state.selectedTenantId }),
    },
  ),
);

export default useAuthStore;
