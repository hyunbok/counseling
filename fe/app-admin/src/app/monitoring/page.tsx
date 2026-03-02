'use client';

import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { DataTable, type Column } from '@/components/ui/data-table';
import { StatCard } from '@/components/ui/stat-card';
import { useActiveChannels, useAgentStatuses } from '@/hooks/use-monitoring';
import { useAuthGuard } from '@/hooks/use-auth-guard';
import { ChartBarIcon, UsersIcon } from '@heroicons/react/24/outline';
import type { ActiveChannel, AgentStatusInfo } from '@/types';

const channelColumns: Column<ActiveChannel>[] = [
  {
    key: 'id',
    label: '채널 ID',
    render: (row) => <span className="font-mono text-xs">{row.id.slice(0, 8)}...</span>,
  },
  {
    key: 'agentId',
    label: '상담사 ID',
    render: (row) => (
      <span className="font-mono text-xs">{row.agentId ? `${row.agentId.slice(0, 8)}...` : '-'}</span>
    ),
  },
  {
    key: 'status',
    label: '상태',
    render: (row) => (
      <span
        className={`text-xs font-medium px-2 py-0.5 rounded-full ${
          row.status === 'OPEN' || row.status === 'IN_PROGRESS'
            ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
            : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
        }`}
      >
        {row.status}
      </span>
    ),
  },
  {
    key: 'startedAt',
    label: '시작 시각',
    render: (row) => (row.startedAt ? new Date(row.startedAt).toLocaleTimeString('ko-KR') : '-'),
  },
];

type AgentRow = AgentStatusInfo & { id: string };

const agentColumns: Column<AgentRow>[] = [
  { key: 'agentName', label: '상담사', sortable: true },
  {
    key: 'status',
    label: '상태',
    render: (row) => (
      <span
        className={`text-xs font-medium px-2 py-0.5 rounded-full ${
          row.status === 'AVAILABLE'
            ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
            : row.status === 'BUSY'
              ? 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400'
              : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
        }`}
      >
        {row.status}
      </span>
    ),
  },
  {
    key: 'active',
    label: '활성화',
    render: (row) => (
      <span
        className={`text-xs font-medium ${row.active ? 'text-green-600 dark:text-green-400' : 'text-gray-400'}`}
      >
        {row.active ? '활성' : '비활성'}
      </span>
    ),
  },
];

export default function MonitoringPage() {
  const { isAuthenticated } = useAuthGuard();
  const { data: channels, isLoading: channelsLoading } = useActiveChannels();
  const { data: agentStatuses, isLoading: agentsLoading } = useAgentStatuses();
  const agents: AgentRow[] = agentStatuses?.map((a) => ({ ...a, id: a.agentId })) ?? [];

  if (!isAuthenticated) return null;

  const activeChannels = channels?.filter((ch) => ch.status === 'OPEN' || ch.status === 'IN_PROGRESS').length ?? 0;
  const onlineAgents = agents?.filter((a) => a.status !== 'OFFLINE').length ?? 0;

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
          <StatCard label="진행 중 세션" value={activeChannels} icon={ChartBarIcon} />
          <StatCard label="온라인 상담사" value={onlineAgents} icon={UsersIcon} />
        </div>

        <div>
          <h2 className="text-base font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark) mb-3">
            활성 채널
          </h2>
          <DataTable
            columns={channelColumns}
            data={channels ?? []}
            isLoading={channelsLoading}
            emptyMessage="현재 활성 채널이 없습니다."
          />
        </div>

        <div>
          <h2 className="text-base font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark) mb-3">
            상담사 현황
          </h2>
          <DataTable
            columns={agentColumns}
            data={agents ?? []}
            isLoading={agentsLoading}
            emptyMessage="상담사 정보가 없습니다."
          />
        </div>
      </div>
    </SidebarLayout>
  );
}
