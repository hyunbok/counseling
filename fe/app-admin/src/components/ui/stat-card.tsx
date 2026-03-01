'use client';

import { ArrowUpIcon, ArrowDownIcon } from '@heroicons/react/24/solid';

interface StatCardProps {
  label: string;
  value: string | number;
  icon?: React.ComponentType<{ className?: string }>;
  trend?: {
    value: string;
    positive: boolean;
  };
}

export const StatCard = ({ label, value, icon: Icon, trend }: StatCardProps) => {
  return (
    <div className="rounded-[--radius-card] bg-(--color-bg-surface) dark:bg-(--color-bg-surface-dark) border border-gray-200 dark:border-gray-700 p-6 shadow-sm">
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
          {label}
        </p>
        {Icon && (
          <div className="rounded-lg bg-(--color-primary)/10 dark:bg-(--color-primary-dark)/10 p-2">
            <Icon className="h-5 w-5 text-(--color-primary) dark:text-(--color-primary-dark)" />
          </div>
        )}
      </div>
      <p className="mt-3 text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
        {value}
      </p>
      {trend && (
        <div className="mt-2 flex items-center gap-1">
          {trend.positive ? (
            <ArrowUpIcon className="h-3.5 w-3.5 text-(--color-success)" />
          ) : (
            <ArrowDownIcon className="h-3.5 w-3.5 text-(--color-error)" />
          )}
          <span
            className={`text-xs font-medium ${trend.positive ? 'text-(--color-success)' : 'text-(--color-error)'}`}
          >
            {trend.value}
          </span>
        </div>
      )}
    </div>
  );
};
