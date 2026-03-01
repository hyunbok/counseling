import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

interface QueueItem {
  id: string;
  customerName: string;
  waitTime: number;
  createdAt: string;
}

export const useQueueList = () => {
  return useQuery<QueueItem[]>({
    queryKey: ['queue'],
    queryFn: async () => {
      const { data } = await api.get<QueueItem[]>('/api/queue');
      return data;
    },
    refetchInterval: 5000,
    retry: 3,
  });
};

export const useAcceptQueue = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (queueId: string) => {
      const { data } = await api.post(`/api/queue/${queueId}/accept`);
      return data;
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['queue'] });
    },
  });
};
