import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { ActiveChannel, AgentStatusInfo } from '@/types';

// BE has two separate endpoints for monitoring
export const useActiveChannels = () => {
  return useQuery<ActiveChannel[]>({
    queryKey: ['monitoring', 'channels'],
    queryFn: async () => {
      const { data } = await api.get<ActiveChannel[]>('/api-adm/monitoring/channels');
      return data;
    },
    refetchInterval: 5000,
  });
};

export const useAgentStatuses = () => {
  return useQuery<AgentStatusInfo[]>({
    queryKey: ['monitoring', 'agents'],
    queryFn: async () => {
      const { data } = await api.get<AgentStatusInfo[]>('/api-adm/monitoring/agents');
      return data;
    },
    refetchInterval: 5000,
  });
};
