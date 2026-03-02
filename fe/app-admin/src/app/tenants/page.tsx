'use client';

import { useState } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { DataTable, type Column } from '@/components/ui/data-table';
import { CreateModal } from '@/components/ui/create-modal';
import { Button } from '@/components/ui/button';
import { useTenantList, useCreateTenant, useUpdateTenantStatus } from '@/hooks/use-tenants';
import { useAuthGuard } from '@/hooks/use-auth-guard';
import type { Tenant } from '@/types';

const inputClass =
  'w-full rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)';

const labelClass =
  'block text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)';

const tenantStatusLabel: Record<string, string> = {
  ACTIVE: '활성',
  INACTIVE: '비활성',
  SUSPENDED: '정지',
};

const tenantStatusColor: Record<string, string> = {
  ACTIVE: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  INACTIVE: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400',
  SUSPENDED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
};

const columns: Column<Tenant>[] = [
  { key: 'name', label: '이름', sortable: true },
  { key: 'slug', label: '슬러그', sortable: true },
  {
    key: 'status',
    label: '상태',
    render: (row) => (
      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${tenantStatusColor[row.status] ?? 'bg-gray-100 text-gray-600'}`}>
        {tenantStatusLabel[row.status] ?? row.status}
      </span>
    ),
  },
  { key: 'createdAt', label: '생성일', render: (row) => new Date(row.createdAt).toLocaleDateString('ko-KR') },
];

interface TenantForm {
  name: string;
  slug: string;
  dbHost: string;
  dbPort: string;
  dbName: string;
  dbUsername: string;
  dbPassword: string;
}

const emptyForm: TenantForm = {
  name: '',
  slug: '',
  dbHost: '',
  dbPort: '5432',
  dbName: '',
  dbUsername: '',
  dbPassword: '',
};

export default function TenantsPage() {
  const { isAuthenticated } = useAuthGuard();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form, setForm] = useState<TenantForm>(emptyForm);
  const [deactivateTargetId, setDeactivateTargetId] = useState<string | null>(null);

  const { data, isLoading } = useTenantList();
  const { mutate: createTenant, isPending } = useCreateTenant();
  const { mutate: updateTenantStatus, isPending: isDeactivating } = useUpdateTenantStatus();

  const handleSubmit = () => {
    createTenant(
      {
        name: form.name,
        slug: form.slug,
        dbHost: form.dbHost,
        dbPort: Number(form.dbPort),
        dbName: form.dbName,
        dbUsername: form.dbUsername,
        dbPassword: form.dbPassword,
      },
      {
        onSuccess: () => {
          setIsModalOpen(false);
          setForm(emptyForm);
        },
      },
    );
  };

  const deactivateTarget = data?.find((t) => t.id === deactivateTargetId);

  const columnsWithActions: Column<Tenant>[] = [
    ...columns,
    {
      key: 'actions',
      label: '',
      render: (row) => (
        row.status === 'ACTIVE' ? (
          <button
            onClick={() => setDeactivateTargetId(row.id)}
            className="text-xs text-(--color-error) hover:underline"
          >
            비활성화
          </button>
        ) : null
      ),
    },
  ];

  if (!isAuthenticated) return null;

  return (
    <SidebarLayout>
      <div className="p-8 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              테넌트 관리
            </h1>
            <p className="mt-1 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              전체 테넌트를 조회하고 관리합니다.
            </p>
          </div>
          <Button onClick={() => setIsModalOpen(true)}>테넌트 추가</Button>
        </div>

        <DataTable
          columns={columnsWithActions}
          data={data ?? []}
          isLoading={isLoading}
          emptyMessage="등록된 테넌트가 없습니다."
        />

        {/* Create modal */}
        <CreateModal
          title="테넌트 추가"
          open={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSubmit={handleSubmit}
          isPending={isPending}
        >
          {[
            { id: 'tenant-name', field: 'name', label: '이름', placeholder: '테넌트 이름' },
            { id: 'tenant-slug', field: 'slug', label: '슬러그', placeholder: 'my-company' },
            { id: 'tenant-db-host', field: 'dbHost', label: 'DB 호스트', placeholder: 'localhost' },
            { id: 'tenant-db-port', field: 'dbPort', label: 'DB 포트', placeholder: '5432' },
            { id: 'tenant-db-name', field: 'dbName', label: 'DB 이름', placeholder: 'counseling_db' },
            { id: 'tenant-db-username', field: 'dbUsername', label: 'DB 사용자', placeholder: 'db_user' },
            { id: 'tenant-db-password', field: 'dbPassword', label: 'DB 비밀번호', placeholder: '비밀번호', type: 'password' },
          ].map(({ id, field, label, placeholder, type = 'text' }) => (
            <div key={field} className="space-y-1">
              <label htmlFor={id} className={labelClass}>{label}</label>
              <input
                id={id}
                type={type}
                value={form[field as keyof TenantForm]}
                onChange={(e) => setForm((f) => ({ ...f, [field]: e.target.value }))}
                className={inputClass}
                placeholder={placeholder}
              />
            </div>
          ))}
        </CreateModal>

        {/* Deactivate confirmation modal */}
        <CreateModal
          title="테넌트 비활성화"
          open={deactivateTargetId !== null}
          onClose={() => setDeactivateTargetId(null)}
          onSubmit={() => {
            if (deactivateTargetId) {
              updateTenantStatus(
                { id: deactivateTargetId, status: 'INACTIVE' },
                { onSuccess: () => setDeactivateTargetId(null) },
              );
            }
          }}
          isPending={isDeactivating}
          submitLabel="비활성화"
        >
          <p className="text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
            <span className="font-semibold">&ldquo;{deactivateTarget?.name}&rdquo;</span> 테넌트를 비활성화하시겠습니까?
          </p>
        </CreateModal>
      </div>
    </SidebarLayout>
  );
}
