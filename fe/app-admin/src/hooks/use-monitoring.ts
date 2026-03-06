import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { ActiveChannel, AgentStatusInfo } from '@/types';

// BE has two separate endpoints for monitoring
export const useActiveChannels = ({ enabled = true }: { enabled?: boolean } = {}) => {
  return useQuery<ActiveChannel[]>({
    queryKey: ['monitoring', 'channels'],
    enabled,
    queryFn: async () => {
      const { data } = await api.get<ActiveChannel[]>('/api-adm/monitoring/channels');
      return data;
    },
    refetchInterval: enabled ? 5000 : false,
  });
};

interface AgentStatusParams {
  status?: string;
  enabled?: boolean;
}

export const useAgentStatuses = (params: AgentStatusParams = {}) => {
  const { status, enabled = true } = params;
  return useQuery<AgentStatusInfo[]>({
    queryKey: ['monitoring', 'agents', { status }],
    enabled,
    queryFn: async () => {
      const { data } = await api.get<AgentStatusInfo[]>('/api-adm/monitoring/agents', {
        params: { status: status || undefined },
      });
      return data;
    },
    refetchInterval: enabled ? 5000 : false,
  });
};
