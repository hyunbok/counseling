import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { HistoryItem } from '@/hooks/use-history';

export interface HistoryRecording {
  recordingId: string;
  status: string;
  startedAt: string;
  stoppedAt: string | null;
  durationSeconds: number | null;
}

export interface HistoryDetail extends HistoryItem {
  recordings: HistoryRecording[];
  feedback: { rating: number; comment: string | null } | null;
  counselNotes: { id: string; content: string; createdAt: string }[];
}

export function useHistoryDetail(channelId: string | null) {
  return useQuery<HistoryDetail>({
    queryKey: ['history', channelId],
    queryFn: async () => {
      const { data } = await api.get<HistoryDetail>(`/api/history/${channelId}`);
      return data;
    },
    enabled: !!channelId,
  });
}
