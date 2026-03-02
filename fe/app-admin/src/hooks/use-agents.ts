import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Agent, CreateAgentResult } from '@/types';

// BE returns plain array (Flux<AgentResponse>), not paginated
export const useAgentList = () => {
  return useQuery<Agent[]>({
    queryKey: ['agents'],
    queryFn: async () => {
      const { data } = await api.get<Agent[]>('/api-adm/agents');
      return data;
    },
  });
};

interface CreateAgentBody {
  username: string;
  name: string;
  role: string;
  groupId?: string;
}

export const useCreateAgent = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: CreateAgentBody): Promise<CreateAgentResult> => {
      const { data } = await api.post<CreateAgentResult>('/api-adm/agents', body);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agents'] });
    },
  });
};

interface UpdateAgentBody {
  name?: string;
  role?: string;
  groupId?: string;
}

export const useUpdateAgent = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: string; body: UpdateAgentBody }): Promise<Agent> => {
      const { data } = await api.put<Agent>(`/api-adm/agents/${id}`, body);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agents'] });
    },
  });
};

export const useUpdateAgentStatus = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, active }: { id: string; active: boolean }): Promise<Agent> => {
      const { data } = await api.patch<Agent>(`/api-adm/agents/${id}/status`, { active });
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agents'] });
    },
  });
};

export const useResetAgentPassword = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string): Promise<{ temporaryPassword: string }> => {
      const { data } = await api.post<{ temporaryPassword: string }>(`/api-adm/agents/${id}/reset-password`);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agents'] });
    },
  });
};
