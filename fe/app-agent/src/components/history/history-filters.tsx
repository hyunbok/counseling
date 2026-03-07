'use client';

import { memo } from 'react';
import { HistoryFilters } from '@/hooks/use-history';

interface HistoryFiltersProps {
  filters: HistoryFilters;
  onChange: (filters: HistoryFilters) => void;
}

const inputClass =
  'rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) bg-white dark:bg-(--color-bg-elevated-dark) focus:outline-none focus:ring-2 focus:ring-indigo-500';

export const HistoryFiltersBar = memo(function HistoryFiltersBar({ filters, onChange }: HistoryFiltersProps) {
  const hasActiveFilters = !!(filters.groupId || filters.status || filters.customerName || filters.dateFrom || filters.dateTo);

  const handleClear = () => {
    onChange({});
  };

  const updateFilter = (patch: Partial<HistoryFilters>) => {
    onChange({ ...filters, ...patch, page: 0 });
  };

  return (
    <div className="flex flex-wrap gap-3 items-end">
      {/* 상태 */}
      <div className="flex flex-col gap-1">
        <label
          htmlFor="filter-status"
          className="text-xs font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
        >
          상태
        </label>
        <select
          id="filter-status"
          value={filters.status ?? ''}
          onChange={(e) => updateFilter({ status: e.target.value || undefined })}
          className={inputClass}
          aria-label="상태 필터"
        >
          <option value="">전체</option>
          <option value="CLOSED">완료</option>
          <option value="IN_PROGRESS">진행 중</option>
        </select>
      </div>

      {/* 고객명 */}
      <div className="flex flex-col gap-1">
        <label
          htmlFor="filter-customer"
          className="text-xs font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
        >
          고객명
        </label>
        <input
          id="filter-customer"
          type="text"
          placeholder="고객명 검색"
          value={filters.customerName ?? ''}
          onChange={(e) => updateFilter({ customerName: e.target.value || undefined })}
          className={inputClass}
          aria-label="고객명 필터"
        />
      </div>

      {/* Date From */}
      <div className="flex flex-col gap-1">
        <label
          htmlFor="filter-date-from"
          className="text-xs font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
        >
          시작일
        </label>
        <input
          id="filter-date-from"
          type="date"
          value={filters.dateFrom ?? ''}
          onChange={(e) => updateFilter({ dateFrom: e.target.value || undefined })}
          className={inputClass}
          aria-label="시작 날짜 필터"
        />
      </div>

      {/* Date To */}
      <div className="flex flex-col gap-1">
        <label
          htmlFor="filter-date-to"
          className="text-xs font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
        >
          종료일
        </label>
        <input
          id="filter-date-to"
          type="date"
          value={filters.dateTo ?? ''}
          onChange={(e) => updateFilter({ dateTo: e.target.value || undefined })}
          className={inputClass}
          aria-label="종료 날짜 필터"
        />
      </div>

      {/* Group select */}
      <div className="flex flex-col gap-1">
        <label
          htmlFor="filter-group"
          className="text-xs font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
        >
          그룹
        </label>
        <select
          id="filter-group"
          value={filters.groupId ?? ''}
          onChange={(e) => updateFilter({ groupId: e.target.value || undefined })}
          className={inputClass}
          aria-label="그룹 필터"
        >
          <option value="">전체 그룹</option>
        </select>
      </div>

      {/* Clear filters */}
      {hasActiveFilters && (
        <button
          onClick={handleClear}
          className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark) transition-colors"
          aria-label="필터 초기화"
        >
          필터 초기화
        </button>
      )}
    </div>
  );
});
