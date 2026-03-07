'use client';

import { VideoCameraIcon, StarIcon } from '@heroicons/react/24/outline';
import { HistoryItem } from '@/hooks/use-history';

interface HistoryTableProps {
  items: HistoryItem[];
  isLoading: boolean;
  page: number;
  totalPages: number;
  totalCount: number;
  onPageChange: (page: number) => void;
  onRowClick: (channelId: string) => void;
}

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

function formatDuration(seconds: number | null): string {
  if (seconds == null) return '-';
  if (seconds < 60) return `${seconds}초`;
  const minutes = Math.floor(seconds / 60);
  const remaining = seconds % 60;
  return remaining > 0 ? `${minutes}분 ${remaining}초` : `${minutes}분`;
}

const SkeletonRow = () => (
  <tr>
    {[...Array(7)].map((_, i) => (
      <td key={i} className="px-4 py-3">
        <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
      </td>
    ))}
  </tr>
);

export const HistoryTable = ({
  items,
  isLoading,
  page,
  totalPages,
  totalCount,
  onPageChange,
  onRowClick,
}: HistoryTableProps) => {
  return (
    <div className="space-y-4">
      <div className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
        총 {totalCount}건
      </div>
      <div className="rounded-xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-sm dark:border dark:border-gray-700 overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-(--color-bg-surface) dark:bg-(--color-bg-elevated-dark) border-b border-gray-200 dark:border-gray-700">
              <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                상담일시
              </th>
              <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                상태
              </th>
              <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                고객명
              </th>
              <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                상담사
              </th>
              <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                그룹
              </th>
              <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                소요시간
              </th>
              <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                녹화 / 평점
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
            {isLoading ? (
              <>
                <SkeletonRow />
                <SkeletonRow />
                <SkeletonRow />
                <SkeletonRow />
                <SkeletonRow />
              </>
            ) : items.length === 0 ? (
              <tr>
                <td
                  colSpan={7}
                  className="px-4 py-12 text-center text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)"
                >
                  상담 이력이 없습니다.
                </td>
              </tr>
            ) : (
              items.map((item) => (
                <tr
                  key={item.channelId}
                  onClick={() => onRowClick(item.channelId)}
                  className="hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark)/50 transition-colors cursor-pointer"
                  role="button"
                  aria-label={`${item.customerName ?? '알 수 없음'} 상담 이력 상세 보기`}
                >
                  <td className="px-4 py-3 text-(--color-text-primary) dark:text-(--color-text-primary-dark) whitespace-nowrap">
                    {formatDateTime(item.startedAt)}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                      item.status === 'CLOSED' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                      : item.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'
                      : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
                    }`}>
                      {item.status === 'CLOSED' ? '완료' : item.status === 'IN_PROGRESS' ? '진행 중' : item.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-medium text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
                    {item.customerName ?? '-'}
                  </td>
                  <td className="px-4 py-3 text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                    {item.agentName ?? '-'}
                  </td>
                  <td className="px-4 py-3 text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                    {item.groupName ?? '-'}
                  </td>
                  <td className="px-4 py-3 text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                    {formatDuration(item.durationSeconds)}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      {item.hasRecording && (
                        <VideoCameraIcon
                          className="h-4 w-4 text-indigo-500 dark:text-indigo-400"
                          aria-label="녹화 있음"
                        />
                      )}
                      {item.feedbackRating != null && (
                        <span className="inline-flex items-center gap-0.5 text-amber-500 dark:text-amber-400">
                          <StarIcon className="h-4 w-4" aria-hidden="true" />
                          <span className="text-xs font-medium">
                            {item.feedbackRating}
                          </span>
                        </span>
                      )}
                      {!item.hasRecording && item.feedbackRating == null && (
                        <span className="text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                          -
                        </span>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="flex flex-col items-center gap-2">
          <div className="flex items-center gap-1">
            <button
              onClick={() => onPageChange(0)}
              disabled={page === 0}
              className="px-2 py-1 text-sm rounded-md disabled:opacity-30 text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark) transition-colors"
              aria-label="첫 페이지"
            >
              «
            </button>
            <button
              onClick={() => onPageChange(page - 1)}
              disabled={page === 0}
              className="px-2 py-1 text-sm rounded-md disabled:opacity-30 text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark) transition-colors"
              aria-label="이전 페이지"
            >
              ‹
            </button>
            {Array.from({ length: totalPages }, (_, i) => i)
              .filter((i) => i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 2)
              .reduce<number[]>((acc, curr) => {
                if (acc.length > 0 && curr - acc[acc.length - 1] > 1) {
                  acc.push(-1);
                }
                acc.push(curr);
                return acc;
              }, [])
              .map((p, idx) =>
                p === -1 ? (
                  <span key={`ellipsis-${idx}`} className="px-2 py-1 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                    …
                  </span>
                ) : (
                  <button
                    key={p}
                    onClick={() => onPageChange(p)}
                    className={`px-3 py-1 text-sm rounded-md transition-colors ${
                      p === page
                        ? 'bg-indigo-600 text-white'
                        : 'text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark)'
                    }`}
                  >
                    {p + 1}
                  </button>
                ),
              )}
            <button
              onClick={() => onPageChange(page + 1)}
              disabled={page >= totalPages - 1}
              className="px-2 py-1 text-sm rounded-md disabled:opacity-30 text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark) transition-colors"
              aria-label="다음 페이지"
            >
              ›
            </button>
            <button
              onClick={() => onPageChange(totalPages - 1)}
              disabled={page >= totalPages - 1}
              className="px-2 py-1 text-sm rounded-md disabled:opacity-30 text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark) transition-colors"
              aria-label="마지막 페이지"
            >
              »
            </button>
          </div>
        </div>
    </div>
  );
};
