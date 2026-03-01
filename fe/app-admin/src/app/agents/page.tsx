'use client';

import { useState } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { DataTable, type Column } from '@/components/ui/data-table';
import { CreateModal } from '@/components/ui/create-modal';
import { Button } from '@/components/ui/button';
import { useAgentList, useCreateAgent, useDeleteAgent } from '@/hooks/use-agents';
import { useAuthGuard } from '@/hooks/use-auth-guard';
import type { Agent } from '@/types';

const inputClass =
  'w-full rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)';

const selectClass =
  'w-full rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)';

const labelClass =
  'block text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)';

// Mock tenant and group lists for dropdowns until real endpoints are wired up
const MOCK_TENANTS = [
  { id: 'tenant-001', name: '(주)상담플러스' },
  { id: 'tenant-002', name: '케어24' },
  { id: 'tenant-003', name: '마음터' },
];

const MOCK_GROUPS = [
  { id: 'group-001', name: '서울 1팀', tenantId: 'tenant-001' },
  { id: 'group-002', name: '서울 2팀', tenantId: 'tenant-001' },
  { id: 'group-003', name: '부산팀', tenantId: 'tenant-002' },
];

const statusLabel: Record<Agent['status'], string> = {
  ACTIVE: '활성',
  INACTIVE: '비활성',
  BUSY: '상담 중',
};

const statusColor: Record<Agent['status'], string> = {
  ACTIVE: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  INACTIVE: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400',
  BUSY: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
};

const columns: Column<Agent>[] = [
  { key: 'name', label: '이름', sortable: true },
  { key: 'username', label: '아이디', sortable: true },
  { key: 'email', label: '이메일' },
  { key: 'groupName', label: '그룹' },
  {
    key: 'status',
    label: '상태',
    render: (row) => (
      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${statusColor[row.status]}`}>
        {statusLabel[row.status]}
      </span>
    ),
  },
  { key: 'createdAt', label: '등록일', render: (row) => new Date(row.createdAt).toLocaleDateString('ko-KR') },
];

export default function AgentsPage() {
  const { isAuthenticated } = useAuthGuard();
  const [page, setPage] = useState(0);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form, setForm] = useState({ tenantId: '', groupId: '', username: '', name: '', email: '', password: '' });
  const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);

  const { data, isLoading } = useAgentList({ page, size: 10 });
  const { mutate: createAgent, isPending } = useCreateAgent();
  const { mutate: deleteAgent, isPending: isDeleting } = useDeleteAgent();

  const availableGroups = MOCK_GROUPS.filter((g) => !form.tenantId || g.tenantId === form.tenantId);

  const handleSubmit = () => {
    const { groupId, ...rest } = form;
    createAgent(
      { ...rest, ...(groupId ? { groupId } : {}) },
      {
        onSuccess: () => {
          setIsModalOpen(false);
          setForm({ tenantId: '', groupId: '', username: '', name: '', email: '', password: '' });
        },
      },
    );
  };

  const deleteTarget = data?.content.find((a) => a.id === deleteTargetId);

  const columnsWithActions: Column<Agent>[] = [
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
              상담사 관리
            </h1>
            <p className="mt-1 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              상담사 계정을 조회하고 관리합니다.
            </p>
          </div>
          <Button onClick={() => setIsModalOpen(true)}>상담사 추가</Button>
        </div>

        <DataTable
          columns={columnsWithActions}
          data={data?.content ?? []}
          totalElements={data?.totalElements}
          page={page}
          pageSize={10}
          onPageChange={setPage}
          isLoading={isLoading}
          emptyMessage="등록된 상담사가 없습니다."
        />

        {/* Create modal */}
        <CreateModal
          title="상담사 추가"
          open={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSubmit={handleSubmit}
          isPending={isPending}
        >
          <div className="space-y-1">
            <label htmlFor="agent-tenant" className={labelClass}>테넌트</label>
            <select
              id="agent-tenant"
              value={form.tenantId}
              onChange={(e) => setForm((f) => ({ ...f, tenantId: e.target.value, groupId: '' }))}
              className={selectClass}
            >
              <option value="">테넌트 선택</option>
              {MOCK_TENANTS.map((t) => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label htmlFor="agent-group" className={labelClass}>그룹 (선택)</label>
            <select
              id="agent-group"
              value={form.groupId}
              onChange={(e) => setForm((f) => ({ ...f, groupId: e.target.value }))}
              className={selectClass}
            >
              <option value="">그룹 없음</option>
              {availableGroups.map((g) => (
                <option key={g.id} value={g.id}>{g.name}</option>
              ))}
            </select>
          </div>
          {[
            { id: 'agent-username', field: 'username', label: '아이디', placeholder: '아이디' },
            { id: 'agent-name', field: 'name', label: '이름', placeholder: '이름' },
            { id: 'agent-email', field: 'email', label: '이메일', placeholder: 'example@email.com' },
            { id: 'agent-password', field: 'password', label: '비밀번호', placeholder: '비밀번호', type: 'password' },
          ].map(({ id, field, label, placeholder, type = 'text' }) => (
            <div key={field} className="space-y-1">
              <label htmlFor={id} className={labelClass}>{label}</label>
              <input
                id={id}
                type={type}
                value={form[field as keyof typeof form]}
                onChange={(e) => setForm((f) => ({ ...f, [field]: e.target.value }))}
                className={inputClass}
                placeholder={placeholder}
              />
            </div>
          ))}
        </CreateModal>

        {/* Delete confirmation modal */}
        <CreateModal
          title="상담사 삭제"
          open={deleteTargetId !== null}
          onClose={() => setDeleteTargetId(null)}
          onSubmit={() => {
            if (deleteTargetId) {
              deleteAgent(deleteTargetId, { onSuccess: () => setDeleteTargetId(null) });
            }
          }}
          isPending={isDeleting}
          submitLabel="삭제"
        >
          <p className="text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
            <span className="font-semibold">&ldquo;{deleteTarget?.name}&rdquo;</span> 상담사를 삭제하시겠습니까?
            이 작업은 되돌릴 수 없습니다.
          </p>
        </CreateModal>
      </div>
    </SidebarLayout>
  );
}
