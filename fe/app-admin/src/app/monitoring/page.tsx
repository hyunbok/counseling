'use client';

import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { DataTable, type Column } from '@/components/ui/data-table';
import { StatCard } from '@/components/ui/stat-card';
import { useActiveSessions } from '@/hooks/use-monitoring';
import { useAuthGuard } from '@/hooks/use-auth-guard';
import { ChartBarIcon, UsersIcon } from '@heroicons/react/24/outline';
import type { MonitoringSession } from '@/types';

const statusLabel: Record<MonitoringSession['status'], string> = {
  WAITING: '대기',
  IN_PROGRESS: '진행 중',
  ENDED: '종료',
};

const statusColor: Record<MonitoringSession['status'], string> = {
  WAITING: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
  IN_PROGRESS: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  ENDED: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400',
};

const columns: Column<MonitoringSession>[] = [
  { key: 'agentName', label: '상담사', sortable: true },
  { key: 'clientName', label: '고객', sortable: true },
  {
    key: 'status',
    label: '상태',
    render: (row) => (
      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${statusColor[row.status]}`}>
        {statusLabel[row.status]}
      </span>
    ),
  },
  {
    key: 'startedAt',
    label: '시작 시각',
    render: (row) => new Date(row.startedAt).toLocaleTimeString('ko-KR'),
  },
  {
    key: 'duration',
    label: '진행 시간',
    render: (row) =>
      row.duration !== undefined ? `${Math.floor(row.duration / 60)}분 ${row.duration % 60}초` : '-',
  },
];

export default function MonitoringPage() {
  const { isAuthenticated } = useAuthGuard();
  const { data, isLoading } = useActiveSessions();

  if (!isAuthenticated) return null;

  return (
    <SidebarLayout>
      <div className="p-8 space-y-6">
        <div>
          <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
            모니터링
          </h1>
          <p className="mt-1 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
            실시간 상담 세션을 모니터링합니다. (5초마다 자동 갱신)
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <StatCard label="진행 중 세션" value={data?.totalActive ?? '-'} icon={ChartBarIcon} />
          <StatCard label="대기 중 세션" value={data?.totalWaiting ?? '-'} icon={UsersIcon} />
        </div>

        <DataTable
          columns={columns}
          data={data?.sessions ?? []}
          isLoading={isLoading}
          emptyMessage="현재 활성 세션이 없습니다."
        />
      </div>
    </SidebarLayout>
  );
}
