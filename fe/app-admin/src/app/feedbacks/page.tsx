'use client';

import { useState, useCallback } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { DataTable, type Column } from '@/components/ui/data-table';
import { useFeedbackList } from '@/hooks/use-feedbacks';
import { useAuthGuard } from '@/hooks/use-auth-guard';
import type { Feedback } from '@/types';

const selectClass =
  'rounded-lg border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-1.5 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)';

const PAGE_SIZE = 10;

const columns: Column<Feedback>[] = [
  {
    key: 'channelId',
    label: '채널 ID',
    render: (row) => <span className="font-mono text-xs">{row.channelId.slice(0, 8)}...</span>,
  },
  {
    key: 'rating',
    label: '평점',
    render: (row) => (
      <span className="flex items-center gap-0.5">
        {'★'.repeat(row.rating)}{'☆'.repeat(5 - row.rating)}
        <span className="ml-1 text-xs text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
          ({row.rating}/5)
        </span>
      </span>
    ),
  },
  {
    key: 'comment',
    label: '코멘트',
    render: (row) => (
      <span className="max-w-xs truncate block text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
        {row.comment ?? '-'}
      </span>
    ),
  },
  { key: 'createdAt', label: '날짜', render: (row) => new Date(row.createdAt).toLocaleDateString('ko-KR') },
];

export default function FeedbacksPage() {
  const { isAuthenticated } = useAuthGuard();
  const [ratingFilter, setRatingFilter] = useState<number | undefined>(undefined);
  const [page, setPage] = useState(0);

  const handleRatingChange = useCallback((value: number | undefined) => {
    setRatingFilter(value);
    setPage(0);
  }, []);

  const { data: pageData, isLoading } = useFeedbackList({ rating: ratingFilter, page, size: PAGE_SIZE });
  const feedbacks = pageData?.content ?? [];
  const totalElements = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;

  if (!isAuthenticated) return null;

  return (
    <SidebarLayout>
      <div className="p-8 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              피드백
            </h1>
            <p className="mt-1 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              상담 완료 후 고객 피드백을 확인합니다.
            </p>
          </div>

          {/* Rating filter */}
          <div className="flex items-center gap-2">
            <label
              htmlFor="rating-filter"
              className="text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
            >
              평점 필터
            </label>
            <select
              id="rating-filter"
              value={ratingFilter ?? ''}
              onChange={(e) => {
                const val = e.target.value;
                handleRatingChange(val === '' ? undefined : Number(val));
              }}
              className={selectClass}
            >
              <option value="">전체</option>
              {[5, 4, 3, 2, 1].map((r) => (
                <option key={r} value={r}>
                  {r}점
                </option>
              ))}
            </select>
          </div>
        </div>

        {totalElements > 0 && (
          <span className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
            총 {totalElements}건
          </span>
        )}

        <DataTable
          columns={columns}
          data={feedbacks}
          isLoading={isLoading}
          emptyMessage="등록된 피드백이 없습니다."
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
      </div>
    </SidebarLayout>
  );
}
