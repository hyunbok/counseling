'use client';

import { HistoryFilters } from '@/hooks/use-history';

interface HistoryFiltersProps {
  filters: HistoryFilters;
  onChange: (filters: HistoryFilters) => void;
}

export const HistoryFiltersBar = ({ filters, onChange }: HistoryFiltersProps) => {
  const hasActiveFilters = !!(filters.groupId || filters.dateFrom || filters.dateTo);

  const handleClear = () => {
    onChange({});
  };

  return (
    <div className="flex flex-wrap gap-3 items-end">
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
          onChange={(e) =>
            onChange({ ...filters, dateFrom: e.target.value || undefined })
          }
          className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) bg-white dark:bg-(--color-bg-elevated-dark) focus:outline-none focus:ring-2 focus:ring-indigo-500"
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
          onChange={(e) =>
            onChange({ ...filters, dateTo: e.target.value || undefined })
          }
          className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) bg-white dark:bg-(--color-bg-elevated-dark) focus:outline-none focus:ring-2 focus:ring-indigo-500"
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
          onChange={(e) =>
            onChange({ ...filters, groupId: e.target.value || undefined })
          }
          className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) bg-white dark:bg-(--color-bg-elevated-dark) focus:outline-none focus:ring-2 focus:ring-indigo-500"
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
};
