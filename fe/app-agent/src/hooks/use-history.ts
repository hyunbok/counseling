import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';

export interface ChannelSummary {
  id: string;
  status: 'WAITING' | 'IN_PROGRESS' | 'CLOSED';
  customerName: string | null;
  startedAt: string | null;
  endedAt: string | null;
  createdAt: string;
}

interface UseHistoryParams {
  status?: string;
  page?: number;
  size?: number;
}

export function useHistory(params: UseHistoryParams = {}) {
  const { status = 'CLOSED', page = 0, size = 20 } = params;

  return useQuery<ChannelSummary[]>({
    queryKey: ['history', status, page, size],
    queryFn: async () => {
      const { data } = await api.get<ChannelSummary[]>('/api/channels', {
        params: { status, page, size },
      });
      return data;
    },
  });
}
