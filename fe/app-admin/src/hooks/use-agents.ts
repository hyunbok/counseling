import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Agent } from '@/types';

interface AgentsResponse {
  content: Agent[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

interface AgentParams {
  page?: number;
  size?: number;
  groupId?: string;
  tenantId?: string;
  status?: Agent['status'];
  search?: string;
}

interface CreateAgentBody {
  tenantId: string;
  groupId?: string;
  username: string;
  name: string;
  email: string;
  password: string;
}

interface UpdateAgentBody {
  name?: string;
  email?: string;
  groupId?: string;
  status?: Agent['status'];
}

export const useAgentList = (params: AgentParams = {}) => {
  return useQuery<AgentsResponse>({
    queryKey: ['agents', params],
    queryFn: async () => {
      const { data } = await api.get<AgentsResponse>('/api-adm/agents', { params });
      return data;
    },
  });
};

export const useCreateAgent = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: CreateAgentBody): Promise<Agent> => {
      const { data } = await api.post<Agent>('/api-adm/agents', body);
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
    mutationFn: async ({ id, status }: { id: string; status: Agent['status'] }): Promise<Agent> => {
      const { data } = await api.patch<Agent>(`/api-adm/agents/${id}`, { status });
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agents'] });
    },
  });
};

export const useUpdateAgent = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: string; body: UpdateAgentBody }): Promise<Agent> => {
      const { data } = await api.patch<Agent>(`/api-adm/agents/${id}`, body);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agents'] });
    },
  });
};

export const useDeleteAgent = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string): Promise<void> => {
      await api.delete(`/api-adm/agents/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agents'] });
    },
  });
};
