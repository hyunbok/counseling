'use client';

import { useState, useEffect, useCallback } from 'react';
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

const PAGE_SIZE = 10;

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

  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [page, setPage] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  const handleStatusChange = useCallback((value: string) => {
    setStatusFilter(value);
    setPage(0);
  }, []);

  const { data: pageData, isLoading } = useGroupList({
    search: debouncedSearch,
    status: statusFilter,
    page,
    size: PAGE_SIZE,
  });
  const { mutate: createGroup, isPending } = useCreateGroup();
  const { mutate: deleteGroup, isPending: isDeleting } = useDeleteGroup();

  const groups = pageData?.content ?? [];
  const totalElements = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;

  const handleSubmit = () => {
    createGroup({ name: form.name }, {
      onSuccess: () => {
        setIsModalOpen(false);
        setForm({ name: '' });
      },
    });
  };

  const deleteTarget = groups.find((g) => g.id === deleteTargetId);

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

        <div className="flex flex-wrap items-center gap-3">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="그룹명 검색"
            className={`${inputClass} max-w-xs`}
          />
          <select
            value={statusFilter}
            onChange={(e) => handleStatusChange(e.target.value)}
            className="rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)"
          >
            <option value="">전체 상태</option>
            <option value="ACTIVE">활성</option>
            <option value="INACTIVE">비활성</option>
          </select>
          {totalElements > 0 && (
            <span className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              총 {totalElements}건
            </span>
          )}
        </div>

        <DataTable
          columns={columnsWithActions}
          data={groups}
          isLoading={isLoading}
          emptyMessage="조건에 맞는 그룹이 없습니다."
        />

        {/* Pagination */}
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-3 py-1.5 text-sm rounded-md border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) disabled:opacity-40 disabled:cursor-not-allowed hover:border-(--color-primary) transition-colors"
          >
            이전
          </button>
          {Array.from({ length: totalPages }, (_, i) => (
            <button
              key={i}
              onClick={() => setPage(i)}
              className={`px-3 py-1.5 text-sm rounded-md border transition-colors ${
                page === i
                  ? 'bg-(--color-primary) text-white border-(--color-primary)'
                  : 'border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) hover:border-(--color-primary)'
              }`}
            >
              {i + 1}
            </button>
          ))}
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="px-3 py-1.5 text-sm rounded-md border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) disabled:opacity-40 disabled:cursor-not-allowed hover:border-(--color-primary) transition-colors"
          >
            다음
          </button>
        </div>

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
