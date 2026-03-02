'use client';

import { useState } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { DataTable, type Column } from '@/components/ui/data-table';
import { CreateModal } from '@/components/ui/create-modal';
import { Button } from '@/components/ui/button';
import { useGroupList, useCreateGroup, useDeleteGroup } from '@/hooks/use-groups';
import { useAuthGuard } from '@/hooks/use-auth-guard';
import type { Group } from '@/types';

const inputClass =
  'w-full rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)';

const labelClass =
  'block text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)';

const groupStatusLabel: Record<string, string> = {
  ACTIVE: '활성',
  INACTIVE: '비활성',
};

const groupStatusColor: Record<string, string> = {
  ACTIVE: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  INACTIVE: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400',
};

const columns: Column<Group>[] = [
  { key: 'name', label: '그룹명', sortable: true },
  {
    key: 'status',
    label: '상태',
    render: (row) => (
      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${groupStatusColor[row.status] ?? 'bg-gray-100 text-gray-600'}`}>
        {groupStatusLabel[row.status] ?? row.status}
      </span>
    ),
  },
  { key: 'createdAt', label: '생성일', render: (row) => new Date(row.createdAt).toLocaleDateString('ko-KR') },
];

export default function GroupsPage() {
  const { isAuthenticated } = useAuthGuard();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form, setForm] = useState({ name: '' });
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);

  const { data, isLoading } = useGroupList();
  const { mutate: createGroup, isPending } = useCreateGroup();
  const { mutate: deleteGroup, isPending: isDeleting } = useDeleteGroup();

  const handleSubmit = () => {
    createGroup({ name: form.name }, {
      onSuccess: () => {
        setIsModalOpen(false);
        setForm({ name: '' });
      },
    });
  };

  const deleteTarget = data?.find((g) => g.id === deleteTargetId);

  const columnsWithActions: Column<Group>[] = [
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
              그룹 관리
            </h1>
            <p className="mt-1 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              테넌트 내 그룹을 조회하고 관리합니다.
            </p>
          </div>
          <Button onClick={() => setIsModalOpen(true)}>그룹 추가</Button>
        </div>

        <DataTable
          columns={columnsWithActions}
          data={data ?? []}
          isLoading={isLoading}
          emptyMessage="등록된 그룹이 없습니다."
        />

        {/* Create modal */}
        <CreateModal
          title="그룹 추가"
          open={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSubmit={handleSubmit}
          isPending={isPending}
        >
          <div className="space-y-1">
            <label htmlFor="group-name" className={labelClass}>그룹명</label>
            <input
              id="group-name"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              className={inputClass}
              placeholder="그룹명"
            />
          </div>
        </CreateModal>

        {/* Delete confirmation modal */}
        <CreateModal
          title="그룹 삭제"
          open={deleteTargetId !== null}
          onClose={() => setDeleteTargetId(null)}
          onSubmit={() => {
            if (deleteTargetId) {
              deleteGroup(deleteTargetId, { onSuccess: () => setDeleteTargetId(null) });
            }
          }}
          isPending={isDeleting}
          submitLabel="삭제"
        >
          <p className="text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
            <span className="font-semibold">&ldquo;{deleteTarget?.name}&rdquo;</span> 그룹을 삭제하시겠습니까?
            이 작업은 되돌릴 수 없습니다.
          </p>
        </CreateModal>
      </div>
    </SidebarLayout>
  );
}
