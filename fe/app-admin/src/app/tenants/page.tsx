'use client';

import { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
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
  DEACTIVATED: '비활성',
  SUSPENDED: '정지',
  PENDING: '대기',
};

const tenantStatusColor: Record<string, string> = {
  ACTIVE: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  DEACTIVATED: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400',
  SUSPENDED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  PENDING: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
};

const PAGE_SIZE = 10;

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
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Server-side search state
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [page, setPage] = useState(0);

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  // Reset page when status filter changes
  const handleStatusChange = useCallback((value: string) => {
    setStatusFilter(value);
    setPage(0);
  }, []);

  const { data: pageData, isLoading } = useTenantList({
    search: debouncedSearch,
    status: statusFilter,
    page,
    size: PAGE_SIZE,
  });
  const { mutate: createTenant, isPending } = useCreateTenant();
  const { mutate: updateTenantStatus, isPending: isDeactivating } = useUpdateTenantStatus();

  const tenants = pageData?.content ?? [];
  const totalElements = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;

  const getErrorMessage = (error: unknown): string => {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const message = error.response?.data?.message as string | undefined;
      if (status === 409) {
        if (message?.includes('slug')) return '이미 존재하는 슬러그입니다.';
        if (message?.includes('DB host')) return '동일한 DB 호스트/포트를 사용하는 테넌트가 이미 존재합니다.';
        return message ?? '중복된 데이터가 존재합니다.';
      }
      if (status === 400 && message?.includes('Cannot connect'))
        return 'DB 연결에 실패했습니다. 연결 정보를 확인해주세요.';
    }
    return '요청 처리 중 오류가 발생했습니다.';
  };

  const handleSubmit = () => {
    setErrorMessage(null);
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
        onError: (error) => {
          setErrorMessage(getErrorMessage(error));
        },
      },
    );
  };

  const deactivateTarget = tenants.find((t) => t.id === deactivateTargetId);

  const columnsWithActions: Column<Tenant>[] = [
    ...columns,
    {
      key: 'actions',
      label: '',
      render: (row) => (
        row.status === 'ACTIVE' ? (
          <button
            onClick={() => setDeactivateTargetId(row.id)}
            className="text-xs font-medium px-3 py-1 rounded-md border border-red-300 dark:border-red-600 text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 hover:bg-red-100 dark:hover:bg-red-900/40 transition-colors"
          >
            비활성화
          </button>
        ) : row.status === 'PENDING' || row.status === 'DEACTIVATED' ? (
          <button
            onClick={() => {
              setErrorMessage(null);
              updateTenantStatus(
                { id: row.id, status: 'ACTIVE' },
                { onError: (error) => setErrorMessage(getErrorMessage(error)) },
              );
            }}
            className="text-xs font-medium px-3 py-1 rounded-md border border-blue-300 dark:border-blue-600 text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/20 hover:bg-blue-100 dark:hover:bg-blue-900/40 transition-colors"
          >
            활성화
          </button>
        ) : null
      ),
    },
  ];

  if (!isAuthenticated) return null;

  return (
    <SidebarLayout>
      <div className="p-8 space-y-6">
        {errorMessage && (
          <div className="flex items-center justify-between rounded-lg border border-red-300 dark:border-red-600 bg-red-50 dark:bg-red-900/20 px-4 py-3 text-sm text-red-700 dark:text-red-400">
            <span>{errorMessage}</span>
            <button onClick={() => setErrorMessage(null)} className="ml-4 font-medium hover:underline">닫기</button>
          </div>
        )}
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

        <div className="flex flex-wrap items-center gap-3">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="이름 또는 슬러그 검색"
            className={`${inputClass} max-w-xs`}
          />
          <select
            value={statusFilter}
            onChange={(e) => handleStatusChange(e.target.value)}
            className="rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)"
          >
            <option value="">전체 상태</option>
            <option value="ACTIVE">활성</option>
            <option value="DEACTIVATED">비활성</option>
            <option value="SUSPENDED">정지</option>
            <option value="PENDING">대기</option>
          </select>
          {totalElements > 0 && (
            <span className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              총 {totalElements}건
            </span>
          )}
        </div>

        <DataTable
          columns={columnsWithActions}
          data={tenants}
          isLoading={isLoading}
          emptyMessage="조건에 맞는 테넌트가 없습니다."
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
                { id: deactivateTargetId, status: 'DEACTIVATED' },
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
