import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';

interface DashboardRecentItem {
  channelId: string;
  customerName: string | null;
  status: string;
  startedAt: string | null;
  durationSeconds: number | null;
  feedbackRating: number | null;
}

interface DashboardSummary {
  todayCount: number;
  totalDurationSeconds: number | null;
  avgDurationSeconds: number | null;
  recentItems: DashboardRecentItem[];
}

export function useDashboardSummary() {
  return useQuery<DashboardSummary>({
    queryKey: ['dashboard-summary'],
    queryFn: async () => {
      const { data } = await api.get<DashboardSummary>('/api/history/dashboard-summary');
      return data;
    },
    refetchInterval: 10_000,
    refetchOnWindowFocus: true,
    staleTime: 0,
  });
}
