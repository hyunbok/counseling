import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Feedback } from '@/types';

interface FeedbacksResponse {
  content: Feedback[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

interface FeedbackParams {
  page?: number;
  size?: number;
  agentId?: string;
  rating?: number;
  search?: string;
  dateFrom?: string;
  dateTo?: string;
}

export const useFeedbackList = (params: FeedbackParams = {}) => {
  return useQuery<FeedbacksResponse>({
    queryKey: ['feedbacks', params],
    queryFn: async () => {
      const { data } = await api.get<FeedbacksResponse>('/api-adm/feedbacks', { params });
      return data;
    },
  });
};
