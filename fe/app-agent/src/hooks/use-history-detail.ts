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

export interface CustomerDevice {
  deviceType: string | null;
  deviceBrand: string | null;
  osName: string | null;
  osVersion: string | null;
  browserName: string | null;
  browserVersion: string | null;
}

export interface HistoryDetail extends HistoryItem {
  customerContact: string | null;
  customerDevice: CustomerDevice | null;
  recording: HistoryRecording | null;
  feedback: { rating: number; comment: string | null } | null;
  counselNote: { noteId: string; content: string; createdAt: string; updatedAt: string } | null;
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
