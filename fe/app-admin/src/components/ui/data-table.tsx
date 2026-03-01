'use client';

import { ChevronUpIcon, ChevronDownIcon, ChevronLeftIcon, ChevronRightIcon } from '@heroicons/react/24/outline';
import { useState } from 'react';

export interface Column<T> {
  key: keyof T | string;
  label: string;
  sortable?: boolean;
  render?: (row: T) => React.ReactNode;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  totalElements?: number;
  page?: number;
  pageSize?: number;
  onPageChange?: (page: number) => void;
  isLoading?: boolean;
  emptyMessage?: string;
}

type SortDirection = 'asc' | 'desc';

export const DataTable = <T extends { id: string }>({
  columns,
  data,
  totalElements,
  page = 0,
  pageSize = 10,
  onPageChange,
  isLoading = false,
  emptyMessage = '데이터가 없습니다.',
}: DataTableProps<T>) => {
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortDir, setSortDir] = useState<SortDirection>('asc');

  const handleSort = (key: string) => {
    if (sortKey === key) {
      setSortDir((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  };

  const sortedData = [...data].sort((a, b) => {
    if (!sortKey) return 0;
    const aVal = (a as Record<string, unknown>)[sortKey];
    const bVal = (b as Record<string, unknown>)[sortKey];
    if (aVal === undefined || bVal === undefined) return 0;
    const cmp = String(aVal).localeCompare(String(bVal), 'ko');
    return sortDir === 'asc' ? cmp : -cmp;
  });

  const totalPages = totalElements !== undefined ? Math.ceil(totalElements / pageSize) : undefined;

  return (
    <div className="overflow-hidden rounded-[--radius-card] border border-gray-200 dark:border-gray-700 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-sm">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-200 dark:border-gray-700 bg-(--color-bg-surface) dark:bg-(--color-bg-elevated-dark)">
              {columns.map((col) => (
                <th
                  key={String(col.key)}
                  className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
                >
                  {col.sortable ? (
                    <button
                      onClick={() => handleSort(String(col.key))}
                      className="flex items-center gap-1 hover:text-(--color-text-primary) dark:hover:text-(--color-text-primary-dark) transition-colors"
                    >
                      {col.label}
                      {sortKey === String(col.key) ? (
                        sortDir === 'asc' ? (
                          <ChevronUpIcon className="h-3.5 w-3.5" />
                        ) : (
                          <ChevronDownIcon className="h-3.5 w-3.5" />
                        )
                      ) : (
                        <ChevronUpIcon className="h-3.5 w-3.5 opacity-30" />
                      )}
                    </button>
                  ) : (
                    col.label
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-4 py-8 text-center text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)"
                >
                  불러오는 중...
                </td>
              </tr>
            ) : sortedData.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-4 py-8 text-center text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)"
                >
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              sortedData.map((row) => (
                <tr
                  key={row.id}
                  className="border-b border-gray-100 dark:border-gray-700/50 last:border-0 hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark) transition-colors"
                >
                  {columns.map((col) => (
                    <td
                      key={String(col.key)}
                      className="px-4 py-3 text-(--color-text-primary) dark:text-(--color-text-primary-dark)"
                    >
                      {col.render
                        ? col.render(row)
                        : String((row as Record<string, unknown>)[String(col.key)] ?? '')}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages !== undefined && totalPages > 1 && onPageChange && (
        <div className="flex items-center justify-between border-t border-gray-200 dark:border-gray-700 px-4 py-3">
          <p className="text-xs text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
            전체 {totalElements}건
          </p>
          <div className="flex items-center gap-1">
            <button
              onClick={() => onPageChange(page - 1)}
              disabled={page === 0}
              aria-label="이전 페이지"
              className="rounded-lg p-1.5 text-(--color-text-tertiary) hover:bg-(--color-bg-surface) dark:text-(--color-text-tertiary-dark) dark:hover:bg-(--color-bg-elevated-dark) disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronLeftIcon className="h-4 w-4" />
            </button>
            <span className="text-xs text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) px-2">
              {page + 1} / {totalPages}
            </span>
            <button
              onClick={() => onPageChange(page + 1)}
              disabled={page >= totalPages - 1}
              aria-label="다음 페이지"
              className="rounded-lg p-1.5 text-(--color-text-tertiary) hover:bg-(--color-bg-surface) dark:text-(--color-text-tertiary-dark) dark:hover:bg-(--color-bg-elevated-dark) disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              <ChevronRightIcon className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
