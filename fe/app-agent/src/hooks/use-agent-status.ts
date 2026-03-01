import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

type AgentStatus = 'ONLINE' | 'AWAY' | 'WRAP_UP' | 'OFFLINE' | 'BUSY';

interface AgentStatusResponse {
  status: AgentStatus;
  updatedAt: string;
}

export const useAgentStatus = () => {
  return useQuery<AgentStatusResponse>({
    queryKey: ['agent-status'],
    queryFn: async () => {
      const { data } = await api.get<AgentStatusResponse>('/api/agents/me/status');
      return data;
    },
  });
};

export const useUpdateAgentStatus = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (status: AgentStatus) => {
      const { data } = await api.put<AgentStatusResponse>('/api/agents/me/status', { status });
      return data;
    },
    onMutate: async (newStatus) => {
      await queryClient.cancelQueries({ queryKey: ['agent-status'] });
      const previous = queryClient.getQueryData<AgentStatusResponse>(['agent-status']);
      queryClient.setQueryData<AgentStatusResponse>(['agent-status'], (old) =>
        old ? { ...old, status: newStatus } : undefined,
      );
      return { previous };
    },
    onError: (_err, _vars, context) => {
      if (context?.previous) {
        queryClient.setQueryData(['agent-status'], context.previous);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['agent-status'] });
    },
  });
};
