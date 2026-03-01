import { useMutation, useQuery } from '@tanstack/react-query';
import api from '@/lib/api';

interface EnterQueueParams {
  customerName: string;
  customerContact: string;
}

interface QueueStatusResponse {
  position: number;
  estimatedWait: number;
}

export const useEnterQueue = () => {
  return useMutation({
    mutationFn: async (params: EnterQueueParams) => {
      const { data } = await api.post('/api/queue/enter', params);
      return data;
    },
  });
};

export const useLeaveQueue = () => {
  return useMutation({
    mutationFn: async (channelId: string) => {
      const { data } = await api.post('/api/queue/leave', { channelId });
      return data;
    },
  });
};

export const useQueueStatus = (channelId: string | null) => {
  return useQuery<QueueStatusResponse>({
    queryKey: ['queue-status', channelId],
    queryFn: async () => {
      const { data } = await api.get(`/api/queue/status/${channelId}`);
      return data;
    },
    enabled: !!channelId,
    refetchInterval: 5000,
    retry: 3,
    retryDelay: 1000,
  });
};
