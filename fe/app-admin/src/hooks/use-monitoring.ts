import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { MonitoringSession } from '@/types';

interface MonitoringResponse {
  sessions: MonitoringSession[];
  totalActive: number;
  totalWaiting: number;
  avgWaitSeconds: number;
  onlineAgents: number;
}

export const useActiveSessions = () => {
  return useQuery<MonitoringResponse>({
    queryKey: ['monitoring'],
    queryFn: async () => {
      const { data } = await api.get<MonitoringResponse>('/api-adm/monitoring/sessions');
      return data;
    },
    refetchInterval: 5000,
  });
};
