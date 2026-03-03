import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

interface QueueItem {
  id: string;
  customerName: string;
  waitTime: number;
  createdAt: string;
}

interface AcceptResponse {
  channelId: string;
  customerName: string;
  customerContact: string;
  livekitRoomName: string;
  livekitUrl: string;
  agentToken: string;
  customerToken: string;
}

export const useQueueList = () => {
  return useQuery<QueueItem[]>({
    queryKey: ['queue'],
    queryFn: async () => {
      const { data } = await api.get<QueueItem[]>('/api/queue');
      return data;
    },
    refetchInterval: 10_000,
    retry: 3,
  });
};

export const useAcceptQueue = () => {
  const queryClient = useQueryClient();

  return useMutation<AcceptResponse, Error, string>({
    mutationFn: async (queueId: string) => {
      const { data } = await api.post<AcceptResponse>(`/api/queue/${queueId}/accept`);
      return data;
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['queue'] });
    },
  });
};
