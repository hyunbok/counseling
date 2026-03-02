import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Feedback } from '@/types';

interface FeedbackParams {
  agentId?: string;
  rating?: number;
}

// BE returns plain array (Flux<FeedbackResponse>), not paginated
export const useFeedbackList = (params: FeedbackParams = {}) => {
  return useQuery<Feedback[]>({
    queryKey: ['feedbacks', params],
    queryFn: async () => {
      const { data } = await api.get<Feedback[]>('/api-adm/feedbacks', { params });
      return data;
    },
  });
};
