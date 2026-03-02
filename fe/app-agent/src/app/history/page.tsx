'use client';

import { useState } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { MagnifyingGlassIcon } from '@heroicons/react/24/outline';
import { useHistory, ChannelSummary } from '@/hooks/use-history';

function formatDateTime(iso: string | null): string {
  if (!iso) return '-';
  const d = new Date(iso);
  return d.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatDuration(startedAt: string | null, endedAt: string | null): string {
  if (!startedAt || !endedAt) return '-';
  const diff = new Date(endedAt).getTime() - new Date(startedAt).getTime();
  const minutes = Math.round(diff / 60000);
  return `${minutes}분`;
}

function statusLabel(status: ChannelSummary['status']): { text: string; className: string } {
  switch (status) {
    case 'CLOSED':
      return { text: '완료', className: 'bg-green-100 text-green-700 dark:bg-green-900/20 dark:text-green-400' };
    case 'IN_PROGRESS':
      return { text: '진행중', className: 'bg-blue-100 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400' };
    default:
      return { text: '대기', className: 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400' };
  }
}

export default function HistoryPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const pageSize = 20;

  const { data: channels, isLoading, isError } = useHistory({ page, size: pageSize });

  const filtered = (channels ?? []).filter((ch) => {
    if (!search.trim()) return true;
    return ch.customerName?.includes(search.trim());
  });

  return (
    <SidebarLayout>
      <div className="p-6 space-y-6">
        {/* Header */}
        <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
          상담 이력
        </h1>

        {/* Filters */}
        <div className="flex flex-wrap gap-3 items-end">
          <div className="relative flex-1 min-w-48">
            <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="고객명 검색"
              className="w-full rounded-lg border border-gray-300 pl-9 pr-3 py-2 text-sm text-(--color-text-primary) dark:bg-(--color-bg-elevated-dark) dark:border-gray-600 dark:text-(--color-text-primary-dark) placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              aria-label="상담 이력 검색"
            />
          </div>
        </div>

        {/* Table */}
        <div className="rounded-xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-sm dark:border dark:border-gray-700 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-(--color-bg-surface) dark:bg-(--color-bg-elevated-dark) border-b border-gray-200 dark:border-gray-700">
                <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                  상담일시
                </th>
                <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                  고객명
                </th>
                <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                  상태
                </th>
                <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                  소요시간
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {isLoading ? (
                <tr>
                  <td
                    colSpan={4}
                    className="px-4 py-12 text-center text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)"
                  >
                    불러오는 중...
                  </td>
                </tr>
              ) : isError ? (
                <tr>
                  <td
                    colSpan={4}
                    className="px-4 py-12 text-center text-red-500 dark:text-red-400"
                  >
                    데이터를 불러오는 중 오류가 발생했습니다.
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td
                    colSpan={4}
                    className="px-4 py-12 text-center text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)"
                  >
                    상담 이력이 없습니다.
                  </td>
                </tr>
              ) : (
                filtered.map((ch) => {
                  const badge = statusLabel(ch.status);
                  return (
                    <tr
                      key={ch.id}
                      className="hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark)/50 transition-colors"
                    >
                      <td className="px-4 py-3 text-(--color-text-primary) dark:text-(--color-text-primary-dark) whitespace-nowrap">
                        {formatDateTime(ch.createdAt)}
                      </td>
                      <td className="px-4 py-3 font-medium text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
                        {ch.customerName ?? '-'}
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${badge.className}`}
                        >
                          {badge.text}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                        {formatDuration(ch.startedAt, ch.endedAt)}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between">
          <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
            총 {filtered.length}건
          </p>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) disabled:opacity-50"
              aria-label="이전 페이지"
            >
              이전
            </button>
            <span className="text-sm font-medium text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              {page + 1}
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={!channels || channels.length < pageSize}
              className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) disabled:opacity-50"
              aria-label="다음 페이지"
            >
              다음
            </button>
          </div>
        </div>
      </div>
    </SidebarLayout>
  );
}
