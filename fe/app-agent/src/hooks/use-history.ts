import { useQuery } from '@tanstack/react-query';
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
  totalCount: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface HistoryFilters {
  groupId?: string;
  status?: string;
  customerName?: string;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}

export function useHistory(filters: HistoryFilters = {}) {
  const page = filters.page ?? 0;
  const size = filters.size ?? 20;

  return useQuery<HistoryListResponse>({
    queryKey: ['history', filters],
    queryFn: async () => {
      const params: Record<string, string | number | undefined> = {
        groupId: filters.groupId,
        status: filters.status,
        customerName: filters.customerName || undefined,
        dateFrom: filters.dateFrom ? new Date(filters.dateFrom).toISOString() : undefined,
        dateTo: filters.dateTo ? new Date(`${filters.dateTo}T23:59:59`).toISOString() : undefined,
        page,
        size,
      };
      const { data } = await api.get<HistoryListResponse>('/api/history', {
        params,
      });
      return data;
    },
  });
}
