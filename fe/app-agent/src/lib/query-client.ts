import { QueryClient } from '@tanstack/react-query';

const defaultOptions = {
  queries: {
    staleTime: 60_000,
    retry: 1,
    refetchOnWindowFocus: false,
  },
};

let client: QueryClient | undefined;

export function getQueryClient(): QueryClient {
  if (typeof window === 'undefined') {
    // Server: always create a new instance to avoid sharing state between requests
    return new QueryClient({ defaultOptions });
  }
  // Browser: use singleton so state is shared across the app
  return (client ??= new QueryClient({ defaultOptions }));
}
