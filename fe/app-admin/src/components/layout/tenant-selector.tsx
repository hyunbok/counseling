'use client';

import { BuildingOfficeIcon } from '@heroicons/react/24/outline';
import { useTenantList } from '@/hooks/use-tenants';
import useAuthStore from '@/stores/auth-store';

export const TenantSelector = () => {
  const user = useAuthStore((state) => state.user);
  const selectedTenantId = useAuthStore((state) => state.selectedTenantId);
  const setSelectedTenant = useAuthStore((state) => state.setSelectedTenant);
  const { data: tenantsPage } = useTenantList({ size: 100 });
  const tenants = tenantsPage?.content;

  if (user?.role !== 'SUPER_ADMIN') return null;

  const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value;
    setSelectedTenant(value === '' ? null : value);
  };

  return (
    <div className="px-3 py-3 border-b border-gray-200 dark:border-gray-700">
      <label
        htmlFor="tenant-selector"
        className="flex items-center gap-1.5 text-xs font-medium text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) mb-1.5"
      >
        <BuildingOfficeIcon className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
        테넌트 선택
      </label>
      <select
        id="tenant-selector"
        value={selectedTenantId ?? ''}
        onChange={handleChange}
        aria-label="테넌트 선택"
        className="w-full rounded-lg border border-gray-200 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-elevated-dark) text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) px-2.5 py-1.5 focus:outline-none focus:ring-2 focus:ring-(--color-primary)/50 dark:focus:ring-(--color-primary-dark)/50 cursor-pointer"
      >
        <option value="">전체 (테넌트 선택)</option>
        {tenants?.map((tenant) => (
          <option key={tenant.slug} value={tenant.slug}>
            {tenant.name}
          </option>
        ))}
      </select>
    </div>
  );
};
