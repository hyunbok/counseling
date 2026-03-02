import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Group } from '@/types';

// BE returns plain array (Flux<GroupResponse>), not paginated
export const useGroupList = () => {
  return useQuery<Group[]>({
    queryKey: ['groups'],
    queryFn: async () => {
      const { data } = await api.get<Group[]>('/api-adm/groups');
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
