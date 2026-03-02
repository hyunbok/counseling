import { useInfiniteQuery } from '@tanstack/react-query';
import api from '@/lib/api';

export interface HistoryItem {
  channelId: string;
  agentId: string | null;
  agentName: string | null;
  groupId: string | null;
  groupName: string | null;
  customerName: string | null;
  status: string;
  startedAt: string | null;
  endedAt: string | null;
  durationSeconds: number | null;
  hasRecording: boolean;
  hasFeedback: boolean;
  feedbackRating: number | null;
}

export interface HistoryListResponse {
  items: HistoryItem[];
  hasMore: boolean;
}

export interface HistoryFilters {
  groupId?: string;
  dateFrom?: string;
  dateTo?: string;
}

export function useHistory(filters: HistoryFilters = {}) {
  return useInfiniteQuery<HistoryListResponse>({
    queryKey: ['history', filters],
    queryFn: async ({ pageParam }) => {
      const { data } = await api.get<HistoryListResponse>('/api/history', {
        params: {
          ...filters,
          before: pageParam as string | undefined,
          limit: 20,
        },
      });
      return data;
    },
    initialPageParam: undefined,
    getNextPageParam: (lastPage) => {
      if (!lastPage.hasMore || lastPage.items.length === 0) return undefined;
      return lastPage.items[lastPage.items.length - 1].endedAt ?? undefined;
    },
  });
}
