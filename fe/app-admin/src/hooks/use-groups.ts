import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Group } from '@/types';

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface GroupListParams {
  search?: string;
  status?: string;
  page?: number;
  size?: number;
}

export const useGroupList = (params: GroupListParams = {}) => {
  const { search, status, page = 0, size = 10 } = params;
  return useQuery<PageResponse<Group>>({
    queryKey: ['groups', { search, status, page, size }],
    queryFn: async () => {
      const { data } = await api.get<PageResponse<Group>>('/api-adm/groups', {
        params: { search: search || undefined, status: status || undefined, page, size },
      });
      return data;
    },
  });
};

interface CreateGroupBody {
  name: string;
}

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

interface UpdateGroupBody {
  name?: string;
  status?: string;
}

export const useUpdateGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: string; body: UpdateGroupBody }): Promise<Group> => {
      const { data } = await api.put<Group>(`/api-adm/groups/${id}`, body);
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
