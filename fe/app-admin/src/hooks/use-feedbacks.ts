import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Feedback } from '@/types';

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface FeedbackListParams {
  rating?: number;
  agentId?: string;
  page?: number;
  size?: number;
  enabled?: boolean;
}

export const useFeedbackList = (params: FeedbackListParams = {}) => {
  const { rating, agentId, page = 0, size = 10, enabled = true } = params;
  return useQuery<PageResponse<Feedback>>({
    queryKey: ['feedbacks', { rating, agentId, page, size }],
    enabled,
    queryFn: async () => {
      const { data } = await api.get<PageResponse<Feedback>>('/api-adm/feedbacks', {
        params: { rating: rating ?? undefined, agentId: agentId || undefined, page, size },
      });
      return data;
    },
  });
};
