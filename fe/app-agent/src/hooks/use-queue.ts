import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

interface QueueItem {
  id: string;
  customerName: string;
  waitTime: number;
  createdAt: string;
}

interface QueueApiItem {
  entryId: string;
  name: string;
  contact: string;
  enteredAt: string;
  waitDurationSeconds: number;
  position: number;
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
      const { data } = await api.get<QueueApiItem[]>('/api/queue');
      return data.map((item) => ({
        id: item.entryId,
        customerName: item.name,
        waitTime: item.waitDurationSeconds,
        createdAt: item.enteredAt,
      }));
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
