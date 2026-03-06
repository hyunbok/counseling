import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Agent, CreateAgentResult } from '@/types';

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface AgentListParams {
  search?: string;
  role?: string;
  active?: string;
  agentStatus?: string;
  page?: number;
  size?: number;
  enabled?: boolean;
}

export const useAgentList = (params: AgentListParams = {}) => {
  const { search, role, agentStatus, active, page = 0, size = 10, enabled = true } = params;
  return useQuery<PageResponse<Agent>>({
    queryKey: ['agents', { search, role, active, agentStatus, page, size }],
    enabled,
    queryFn: async () => {
      const { data } = await api.get<PageResponse<Agent>>('/api-adm/agents', {
        params: {
          search: search || undefined,
          role: role || undefined,
          active: active === '' ? undefined : active === 'true' ? true : active === 'false' ? false : undefined,
          agentStatus: agentStatus || undefined,
          page,
          size,
        },
      });
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
