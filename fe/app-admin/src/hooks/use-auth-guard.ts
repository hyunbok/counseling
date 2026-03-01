'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import useAuthStore from '@/stores/auth-store';

export const useAuthGuard = () => {
  const router = useRouter();
  const accessToken = useAuthStore((state) => state.accessToken);

  useEffect(() => {
    if (!accessToken) {
      router.push('/login');
    }
  }, [accessToken, router]);

  return { isAuthenticated: !!accessToken };
};
