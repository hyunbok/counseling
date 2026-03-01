import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Group } from '@/types';

interface GroupsResponse {
  content: Group[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

interface GroupParams {
  page?: number;
  size?: number;
  tenantId?: string;
  search?: string;
}

interface CreateGroupBody {
  tenantId: string;
  name: string;
}

interface UpdateGroupBody {
  name?: string;
}

export const useGroupList = (params: GroupParams = {}) => {
  return useQuery<GroupsResponse>({
    queryKey: ['groups', params],
    queryFn: async () => {
      const { data } = await api.get<GroupsResponse>('/api-adm/groups', { params });
      return data;
    },
  });
};

export const useCreateGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: CreateGroupBody): Promise<Group> => {
      const { data } = await api.post<Group>('/api-adm/groups', body);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });
};

export const useUpdateGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: string; body: UpdateGroupBody }): Promise<Group> => {
      const { data } = await api.patch<Group>(`/api-adm/groups/${id}`, body);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });
};

export const useDeleteGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string): Promise<void> => {
      await api.delete(`/api-adm/groups/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });
};
