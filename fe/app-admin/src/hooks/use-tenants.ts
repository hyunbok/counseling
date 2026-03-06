import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Tenant } from '@/types';

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface TenantListParams {
  search?: string;
  status?: string;
  page?: number;
  size?: number;
}

export const useTenantList = (params: TenantListParams = {}) => {
  const { search, status, page = 0, size = 10 } = params;
  return useQuery<PageResponse<Tenant>>({
    queryKey: ['tenants', { search, status, page, size }],
    queryFn: async () => {
      const { data } = await api.get<PageResponse<Tenant>>('/api-adm/tenants', {
        params: { search: search || undefined, status: status || undefined, page, size },
      });
      return data;
    },
  });
};

interface CreateTenantBody {
  name: string;
  slug: string;
  dbHost: string;
  dbPort: number;
  dbName: string;
  dbUsername: string;
  dbPassword: string;
}

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

interface UpdateTenantBody {
  name: string;
  dbHost: string;
  dbPort: number;
  dbName: string;
  dbUsername: string;
  dbPassword: string;
}

export const useUpdateTenant = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: string; body: UpdateTenantBody }): Promise<Tenant> => {
      const { data } = await api.put<Tenant>(`/api-adm/tenants/${id}`, body);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};

export const useUpdateTenantStatus = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, status }: { id: string; status: string }): Promise<Tenant> => {
      const { data } = await api.patch<Tenant>(`/api-adm/tenants/${id}/status`, { status });
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tenants'] });
    },
  });
};
