import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Tenant } from '@/types';

interface TenantsResponse {
  content: Tenant[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

interface TenantParams {
  page?: number;
  size?: number;
  search?: string;
}

interface CreateTenantBody {
  name: string;
  domain: string;
  plan: string;
}

interface UpdateTenantBody {
  name?: string;
  domain?: string;
  plan?: string;
}

export const useTenantList = (params: TenantParams = {}) => {
  return useQuery<TenantsResponse>({
    queryKey: ['tenants', params],
    queryFn: async () => {
      const { data } = await api.get<TenantsResponse>('/api-adm/tenants', { params });
      return data;
    },
  });
};

export const useCreateTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: CreateTenantBody): Promise<Tenant> => {
      const { data } = await api.post<Tenant>('/api-adm/tenants', body);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};

export const useUpdateTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: string; body: UpdateTenantBody }): Promise<Tenant> => {
      const { data } = await api.patch<Tenant>(`/api-adm/tenants/${id}`, body);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};

export const useDeleteTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: string): Promise<void> => {
      await api.delete(`/api-adm/tenants/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};
