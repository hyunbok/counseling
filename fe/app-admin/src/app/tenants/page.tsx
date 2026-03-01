'use client';

import { useState } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { DataTable, type Column } from '@/components/ui/data-table';
import { CreateModal } from '@/components/ui/create-modal';
import { Button } from '@/components/ui/button';
import { useTenantList, useCreateTenant, useDeleteTenant } from '@/hooks/use-tenants';
import { useAuthGuard } from '@/hooks/use-auth-guard';
import type { Tenant } from '@/types';

const inputClass =
  'w-full rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)';

const selectClass =
  'w-full rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)';

const labelClass =
  'block text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)';

const columns: Column<Tenant>[] = [
  { key: 'name', label: '이름', sortable: true },
  { key: 'domain', label: '도메인', sortable: true },
  { key: 'plan', label: '플랜' },
  { key: 'agentCount', label: '상담사 수' },
  { key: 'createdAt', label: '생성일', render: (row) => new Date(row.createdAt).toLocaleDateString('ko-KR') },
];

export default function TenantsPage() {
  const { isAuthenticated } = useAuthGuard();
  const [page, setPage] = useState(0);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form, setForm] = useState({ name: '', domain: '', plan: '' });
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);

  const { data, isLoading } = useTenantList({ page, size: 10 });
  const { mutate: createTenant, isPending } = useCreateTenant();
  const { mutate: deleteTenant, isPending: isDeleting } = useDeleteTenant();

  const handleSubmit = () => {
    createTenant(form, {
      onSuccess: () => {
        setIsModalOpen(false);
        setForm({ name: '', domain: '', plan: '' });
      },
    });
  };

  const deleteTarget = data?.content.find((t) => t.id === deleteTargetId);

  const columnsWithActions: Column<Tenant>[] = [
    ...columns,
    {
      key: 'actions',
      label: '',
      render: (row) => (
        <button
          onClick={() => setDeleteTargetId(row.id)}
          className="text-xs text-(--color-error) hover:underline"
        >
          삭제
        </button>
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
          data={data?.content ?? []}
          totalElements={data?.totalElements}
          page={page}
          pageSize={10}
          onPageChange={setPage}
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
          <div className="space-y-1">
            <label htmlFor="tenant-name" className={labelClass}>이름</label>
            <input
              id="tenant-name"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              className={inputClass}
              placeholder="테넌트 이름"
            />
          </div>
          <div className="space-y-1">
            <label htmlFor="tenant-domain" className={labelClass}>도메인</label>
            <input
              id="tenant-domain"
              value={form.domain}
              onChange={(e) => setForm((f) => ({ ...f, domain: e.target.value }))}
              className={inputClass}
              placeholder="example.com"
            />
          </div>
          <div className="space-y-1">
            <label htmlFor="tenant-plan" className={labelClass}>플랜</label>
            <select
              id="tenant-plan"
              value={form.plan}
              onChange={(e) => setForm((f) => ({ ...f, plan: e.target.value }))}
              className={selectClass}
            >
              <option value="">플랜 선택</option>
              <option value="BASIC">BASIC</option>
              <option value="PRO">PRO</option>
              <option value="ENTERPRISE">ENTERPRISE</option>
            </select>
          </div>
        </CreateModal>

        {/* Delete confirmation modal */}
        <CreateModal
          title="테넌트 삭제"
          open={deleteTargetId !== null}
          onClose={() => setDeleteTargetId(null)}
          onSubmit={() => {
            if (deleteTargetId) {
              deleteTenant(deleteTargetId, { onSuccess: () => setDeleteTargetId(null) });
            }
          }}
          isPending={isDeleting}
          submitLabel="삭제"
        >
          <p className="text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
            <span className="font-semibold">&ldquo;{deleteTarget?.name}&rdquo;</span> 테넌트를 삭제하시겠습니까?
            이 작업은 되돌릴 수 없습니다.
          </p>
        </CreateModal>
      </div>
    </SidebarLayout>
  );
}
