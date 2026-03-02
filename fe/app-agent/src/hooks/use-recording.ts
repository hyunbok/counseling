import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

interface RecordingInfo {
  recordingId: string;
  channelId: string;
  egressId: string;
  status: 'RECORDING' | 'STOPPED' | 'FAILED';
  startedAt: string;
  stoppedAt: string | null;
  filePath: string | null;
}

interface StartRecordingResponse {
  recordingId: string;
  channelId: string;
  egressId: string;
  status: string;
  startedAt: string;
}

interface StopRecordingResponse {
  recordingId: string;
  channelId: string;
  egressId: string;
  status: string;
  startedAt: string;
  stoppedAt: string | null;
  filePath: string | null;
}

export function useRecording(channelId: string) {
  const queryClient = useQueryClient();

  const { data: recordings } = useQuery<{ recordings: RecordingInfo[] }>({
    queryKey: ['recordings', channelId],
    queryFn: async () => {
      const { data } = await api.get(`/api/channels/${channelId}/recordings`);
      return data;
    },
    enabled: !!channelId,
  });

  const isRecording = recordings?.recordings?.some((r) => r.status === 'RECORDING') ?? false;

  const startMutation = useMutation({
    mutationFn: async () => {
      const { data } = await api.post<StartRecordingResponse>(`/api/channels/${channelId}/recordings`);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recordings', channelId] });
    },
  });

  const stopMutation = useMutation({
    mutationFn: async () => {
      const { data } = await api.post<StopRecordingResponse>(`/api/channels/${channelId}/recordings/stop`);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recordings', channelId] });
    },
  });

  return {
    isRecording,
    startRecording: () => startMutation.mutate(),
    stopRecording: () => stopMutation.mutate(),
    isStarting: startMutation.isPending,
    isStopping: stopMutation.isPending,
  };
}
