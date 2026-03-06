'use client';

import { useState, useEffect, useCallback } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { DataTable, type Column } from '@/components/ui/data-table';
import { CreateModal } from '@/components/ui/create-modal';
import { Button } from '@/components/ui/button';
import { useAgentList, useCreateAgent, useUpdateAgentStatus, useResetAgentPassword } from '@/hooks/use-agents';
import { useGroupList } from '@/hooks/use-groups';
import { useAuthGuard } from '@/hooks/use-auth-guard';
import type { Agent, CreateAgentResult } from '@/types';

const inputClass =
  'w-full rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)';

const selectClass =
  'w-full rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)';

const labelClass =
  'block text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)';

const agentStatusLabel: Record<string, string> = {
  AVAILABLE: '대기 중',
  BUSY: '상담 중',
  OFFLINE: '오프라인',
};

const agentStatusColor: Record<string, string> = {
  AVAILABLE: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  BUSY: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
  OFFLINE: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400',
};

const roleLabel: Record<string, string> = {
  COUNSELOR: '상담사',
  ADMIN: '관리자',
};

const PAGE_SIZE = 10;

const selectFilterClass =
  'rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)';

const columns: Column<Agent>[] = [
  { key: 'name', label: '이름', sortable: true },
  { key: 'username', label: '아이디', sortable: true },
  {
    key: 'role',
    label: '역할',
    render: (row) => <span>{roleLabel[row.role] ?? row.role}</span>,
  },
  {
    key: 'active',
    label: '활성화',
    render: (row) => (
      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${row.active ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'}`}>
        {row.active ? '활성' : '비활성'}
      </span>
    ),
  },
  {
    key: 'agentStatus',
    label: '상태',
    render: (row) => (
      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${agentStatusColor[row.agentStatus] ?? 'bg-gray-100 text-gray-600'}`}>
        {agentStatusLabel[row.agentStatus] ?? row.agentStatus}
      </span>
    ),
  },
  { key: 'createdAt', label: '등록일', render: (row) => new Date(row.createdAt).toLocaleDateString('ko-KR') },
];

export default function AgentsPage() {
  const { isAuthenticated } = useAuthGuard();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form, setForm] = useState({ groupId: '', username: '', name: '', role: 'COUNSELOR' });
  const [deactivateTargetId, setDeactivateTargetId] = useState<string | null>(null);
  const [resetPasswordTargetId, setResetPasswordTargetId] = useState<string | null>(null);
  const [tempPassword, setTempPassword] = useState<string | null>(null);

  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [activeFilter, setActiveFilter] = useState('');
  const [agentStatusFilter, setAgentStatusFilter] = useState('');
  const [page, setPage] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  const handleRoleChange = useCallback((value: string) => {
    setRoleFilter(value);
    setPage(0);
  }, []);

  const handleActiveChange = useCallback((value: string) => {
    setActiveFilter(value);
    setPage(0);
  }, []);

  const handleAgentStatusChange = useCallback((value: string) => {
    setAgentStatusFilter(value);
    setPage(0);
  }, []);

  const { data: pageData, isLoading } = useAgentList({
    search: debouncedSearch,
    role: roleFilter,
    active: activeFilter,
    agentStatus: agentStatusFilter,
    page,
    size: PAGE_SIZE,
  });
  const { data: groupsPage } = useGroupList({ size: 100 });
  const groups = groupsPage?.content;
  const agents = pageData?.content ?? [];
  const totalElements = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;
  const { mutate: createAgent, isPending } = useCreateAgent();
  const { mutate: updateAgentStatus, isPending: isDeactivating } = useUpdateAgentStatus();
  const { mutate: resetAgentPassword, isPending: isResetting } = useResetAgentPassword();

  const handleSubmit = () => {
    const { groupId, ...rest } = form;
    createAgent(
      { ...rest, ...(groupId ? { groupId } : {}) },
      {
        onSuccess: (result: CreateAgentResult) => {
          setIsModalOpen(false);
          setForm({ groupId: '', username: '', name: '', role: 'COUNSELOR' });
          setTempPassword(result.temporaryPassword);
        },
      },
    );
  };

  const deactivateTarget = agents.find((a) => a.id === deactivateTargetId);
  const resetPasswordTarget = agents.find((a) => a.id === resetPasswordTargetId);

  const columnsWithActions: Column<Agent>[] = [
    ...columns,
    {
      key: 'actions',
      label: '',
      render: (row) => (
        <div className="flex items-center gap-2">
          {row.active && (
            <button
              onClick={() => setDeactivateTargetId(row.id)}
              className="text-xs text-(--color-error) hover:underline"
            >
              비활성화
            </button>
          )}
          <button
            onClick={() => setResetPasswordTargetId(row.id)}
            className="text-xs text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) hover:underline"
          >
            비밀번호 초기화
          </button>
        </div>
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

        <div className="flex flex-wrap items-center gap-3">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="이름/아이디 검색"
            className={`${inputClass} max-w-xs`}
          />
          <select
            value={roleFilter}
            onChange={(e) => handleRoleChange(e.target.value)}
            className={selectFilterClass}
          >
            <option value="">전체 역할</option>
            <option value="COUNSELOR">상담사</option>
            <option value="ADMIN">관리자</option>
          </select>
          <select
            value={activeFilter}
            onChange={(e) => handleActiveChange(e.target.value)}
            className={selectFilterClass}
          >
            <option value="">전체 활성화</option>
            <option value="true">활성</option>
            <option value="false">비활성</option>
          </select>
          <select
            value={agentStatusFilter}
            onChange={(e) => handleAgentStatusChange(e.target.value)}
            className={selectFilterClass}
          >
            <option value="">전체 상태</option>
            <option value="AVAILABLE">대기 중</option>
            <option value="BUSY">상담 중</option>
            <option value="OFFLINE">오프라인</option>
          </select>
          {totalElements > 0 && (
            <span className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              총 {totalElements}건
            </span>
          )}
        </div>

        <DataTable
          columns={columnsWithActions}
          data={agents}
          isLoading={isLoading}
          emptyMessage="조건에 맞는 상담사가 없습니다."
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
          title="상담사 추가"
          open={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSubmit={handleSubmit}
          isPending={isPending}
        >
          <div className="space-y-1">
            <label htmlFor="agent-role" className={labelClass}>역할</label>
            <select
              id="agent-role"
              value={form.role}
              onChange={(e) => setForm((f) => ({ ...f, role: e.target.value }))}
              className={selectClass}
            >
              <option value="COUNSELOR">상담사</option>
              <option value="ADMIN">관리자</option>
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
              {(groups ?? []).map((g) => (
                <option key={g.id} value={g.id}>{g.name}</option>
              ))}
            </select>
          </div>
          {[
            { id: 'agent-username', field: 'username', label: '아이디', placeholder: '아이디' },
            { id: 'agent-name', field: 'name', label: '이름', placeholder: '이름' },
          ].map(({ id, field, label, placeholder }) => (
            <div key={field} className="space-y-1">
              <label htmlFor={id} className={labelClass}>{label}</label>
              <input
                id={id}
                type="text"
                value={form[field as keyof typeof form]}
                onChange={(e) => setForm((f) => ({ ...f, [field]: e.target.value }))}
                className={inputClass}
                placeholder={placeholder}
              />
            </div>
          ))}
        </CreateModal>

        {/* Deactivate confirmation modal */}
        <CreateModal
          title="상담사 비활성화"
          open={deactivateTargetId !== null}
          onClose={() => setDeactivateTargetId(null)}
          onSubmit={() => {
            if (deactivateTargetId) {
              updateAgentStatus(
                { id: deactivateTargetId, active: false },
                { onSuccess: () => setDeactivateTargetId(null) },
              );
            }
          }}
          isPending={isDeactivating}
          submitLabel="비활성화"
        >
          <p className="text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
            <span className="font-semibold">&ldquo;{deactivateTarget?.name}&rdquo;</span> 상담사를 비활성화하시겠습니까?
          </p>
        </CreateModal>

        {/* Reset password confirmation modal */}
        <CreateModal
          title="비밀번호 초기화"
          open={resetPasswordTargetId !== null}
          onClose={() => setResetPasswordTargetId(null)}
          onSubmit={() => {
            if (resetPasswordTargetId) {
              resetAgentPassword(resetPasswordTargetId, {
                onSuccess: (result) => {
                  setResetPasswordTargetId(null);
                  setTempPassword(result.temporaryPassword);
                },
              });
            }
          }}
          isPending={isResetting}
          submitLabel="초기화"
        >
          <p className="text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
            <span className="font-semibold">&ldquo;{resetPasswordTarget?.name}&rdquo;</span> 상담사의 비밀번호를 초기화하시겠습니까?
          </p>
        </CreateModal>

        {/* Temporary password display modal */}
        <CreateModal
          title="임시 비밀번호"
          open={tempPassword !== null}
          onClose={() => setTempPassword(null)}
          onSubmit={() => setTempPassword(null)}
          isPending={false}
          submitLabel="확인"
        >
          <p className="text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
            임시 비밀번호가 생성되었습니다. 상담사에게 전달해 주세요.
          </p>
          <div className="mt-2 rounded-[--radius-input] border border-gray-200 dark:border-gray-600 bg-(--color-bg-surface) dark:bg-(--color-bg-elevated-dark) px-4 py-3">
            <p className="font-mono text-base font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark) tracking-widest">
              {tempPassword}
            </p>
          </div>
        </CreateModal>
      </div>
    </SidebarLayout>
  );
}
