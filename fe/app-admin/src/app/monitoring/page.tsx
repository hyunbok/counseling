'use client';

import { useState, useMemo, useCallback } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { DataTable, type Column } from '@/components/ui/data-table';
import { StatCard } from '@/components/ui/stat-card';
import { useActiveChannels, useAgentStatuses } from '@/hooks/use-monitoring';
import { useAuthGuard } from '@/hooks/use-auth-guard';
import { ChartBarIcon, UsersIcon } from '@heroicons/react/24/outline';

const AGENT_PAGE_SIZE = 10;
import type { ActiveChannel, AgentStatusInfo } from '@/types';

const agentStatusLabel: Record<string, string> = {
  ONLINE: '온라인',
  OFFLINE: '오프라인',
  BUSY: '상담 중',
  AWAY: '자리 비움',
  WRAP_UP: '후처리',
};

const agentStatusColor: Record<string, string> = {
  ONLINE: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  BUSY: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
  AWAY: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400',
  WRAP_UP: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  OFFLINE: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400',
};

const channelStatusLabel: Record<string, string> = {
  OPEN: '대기',
  IN_PROGRESS: '진행 중',
  CLOSED: '종료',
};

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
        {channelStatusLabel[row.status] ?? row.status}
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
        className={`text-xs font-medium px-2 py-0.5 rounded-full ${agentStatusColor[row.status] ?? 'bg-gray-100 text-gray-600'}`}
      >
        {agentStatusLabel[row.status] ?? row.status}
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

const selectFilterClass =
  'rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-(--color-primary)';

export default function MonitoringPage() {
  const { isAuthenticated } = useAuthGuard();
  const { data: channels, isLoading: channelsLoading } = useActiveChannels();
  const [agentStatusFilter, setAgentStatusFilter] = useState('ONLINE');
  const [agentPage, setAgentPage] = useState(0);
  const { data: agentStatuses, isLoading: agentsLoading } = useAgentStatuses({ status: agentStatusFilter });
  const { data: allAgentStatuses } = useAgentStatuses();
  const allAgents: AgentRow[] = agentStatuses?.map((a) => ({ ...a, id: a.agentId })) ?? [];
  const agentTotalPages = Math.ceil(allAgents.length / AGENT_PAGE_SIZE);
  const agents = useMemo(
    () => allAgents.slice(agentPage * AGENT_PAGE_SIZE, (agentPage + 1) * AGENT_PAGE_SIZE),
    [allAgents, agentPage],
  );
  const handleAgentStatusFilterChange = useCallback((value: string) => {
    setAgentStatusFilter(value);
    setAgentPage(0);
  }, []);

  if (!isAuthenticated) return null;

  const activeChannels = channels?.filter((ch) => ch.status === 'OPEN' || ch.status === 'IN_PROGRESS').length ?? 0;
  const onlineAgents = allAgentStatuses?.filter((a) => a.status !== 'OFFLINE').length ?? 0;

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
          <div className="flex items-center gap-3 mb-3">
            <h2 className="text-base font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              상담사 현황
            </h2>
            <select
              value={agentStatusFilter}
              onChange={(e) => handleAgentStatusFilterChange(e.target.value)}
              className={selectFilterClass}
            >
              <option value="">전체</option>
              <option value="ONLINE">온라인</option>
              <option value="BUSY">상담 중</option>
              <option value="AWAY">자리 비움</option>
              <option value="WRAP_UP">후처리</option>
              <option value="OFFLINE">오프라인</option>
            </select>
            <span className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              {allAgents.length}명
            </span>
          </div>
          <DataTable
            columns={agentColumns}
            data={agents}
            isLoading={agentsLoading}
            emptyMessage="상담사 정보가 없습니다."
          />
          <div className="flex items-center justify-center gap-2 mt-3">
            <button
              onClick={() => setAgentPage((p) => Math.max(0, p - 1))}
              disabled={agentPage === 0}
              className="px-3 py-1.5 text-sm rounded-md border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) disabled:opacity-40 disabled:cursor-not-allowed hover:border-(--color-primary) transition-colors"
            >
              이전
            </button>
            {Array.from({ length: agentTotalPages }, (_, i) => (
              <button
                key={i}
                onClick={() => setAgentPage(i)}
                className={`px-3 py-1.5 text-sm rounded-md border transition-colors ${
                  agentPage === i
                    ? 'bg-(--color-primary) text-white border-(--color-primary)'
                    : 'border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) hover:border-(--color-primary)'
                }`}
              >
                {i + 1}
              </button>
            ))}
            <button
              onClick={() => setAgentPage((p) => Math.min(agentTotalPages - 1, p + 1))}
              disabled={agentPage >= agentTotalPages - 1}
              className="px-3 py-1.5 text-sm rounded-md border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) disabled:opacity-40 disabled:cursor-not-allowed hover:border-(--color-primary) transition-colors"
            >
              다음
            </button>
          </div>
        </div>
      </div>
    </SidebarLayout>
  );
}
