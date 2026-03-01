'use client';

import { QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { getQueryClient } from '@/lib/query-client';

interface ProvidersProps {
  children: React.ReactNode;
}

export default function Providers({ children }: ProvidersProps) {
  // useState with lazy initializer ensures a single QueryClient instance per component mount
  const [queryClient] = useState(getQueryClient);

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
