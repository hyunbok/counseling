import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { StatsSummary, AgentStats } from '@/types';

export const useStatsSummary = (from?: string, to?: string) => {
  return useQuery<StatsSummary>({
    queryKey: ['stats', 'summary', from, to],
    enabled: !!from && !!to,
    queryFn: async () => {
      const { data } = await api.get<StatsSummary>('/api-adm/stats/summary', {
        params: { from, to },
      });
      return data;
    },
  });
};

export const useAgentStats = (from?: string, to?: string) => {
  return useQuery<AgentStats[]>({
    queryKey: ['stats', 'agents', from, to],
    enabled: !!from && !!to,
    queryFn: async () => {
      const { data } = await api.get<AgentStats[]>('/api-adm/stats/agents', {
        params: { from, to },
      });
      return data;
    },
  });
};
