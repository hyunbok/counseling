'use client';

import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { QueueList } from '@/components/queue/queue-list';
import { useAgentStatus, useUpdateAgentStatus } from '@/hooks/use-agent-status';
import { useQueueList } from '@/hooks/use-queue';
import { useQueueStream } from '@/hooks/use-queue-stream';

type AgentStatus = 'ONLINE' | 'AWAY' | 'WRAP_UP';

const statusConfig: Record<AgentStatus, { label: string; dotClass: string }> = {
  ONLINE: { label: '온라인', dotClass: 'bg-green-500' },
  AWAY: { label: '자리비움', dotClass: 'bg-amber-500' },
  WRAP_UP: { label: '마무리 중', dotClass: 'bg-purple-500' },
};

const recentSessions = [
  { id: '1', customerName: '김민준', type: '일반 상담', duration: '23분', memo: '계약 관련 문의' },
  { id: '2', customerName: '이서연', type: '기술 지원', duration: '15분', memo: '앱 오류 해결' },
  { id: '3', customerName: '박지훈', type: '일반 상담', duration: '31분', memo: '해지 상담' },
];

export default function DashboardPage() {
  const { data: agentStatus } = useAgentStatus();
  const updateStatus = useUpdateAgentStatus();
  const { data: queue } = useQueueList();

  useQueueStream();

  const currentStatus = (agentStatus?.status as AgentStatus | undefined) ?? 'ONLINE';
  const queueCount = queue?.length ?? 0;

  const handleStatusChange = (status: AgentStatus) => {
    updateStatus.mutate(status);
  };

  return (
    <SidebarLayout>
      <div className="p-6 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
            대시보드
          </h1>

          {/* Status selector */}
          <div className="flex items-center gap-2">
            {(['ONLINE', 'AWAY', 'WRAP_UP'] as AgentStatus[]).map((status) => {
              const config = statusConfig[status];
              const isActive = currentStatus === status;
              return (
                <button
                  key={status}
                  onClick={() => handleStatusChange(status)}
                  className={`flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-medium transition-colors border ${
                    isActive
                      ? 'bg-(--color-bg-elevated) dark:bg-(--color-bg-elevated-dark) border-gray-300 dark:border-gray-600 text-(--color-text-primary) dark:text-(--color-text-primary-dark)'
                      : 'border-transparent text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark)'
                  }`}
                  aria-label={`상태를 ${config.label}으로 변경`}
                  aria-pressed={isActive}
                >
                  <span className={`h-2 w-2 rounded-full ${config.dotClass}`} />
                  {config.label}
                </button>
              );
            })}
          </div>
        </div>

        {/* Summary cards */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div className="rounded-xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-sm p-6 dark:border dark:border-gray-700">
            <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              대기 고객 수
            </p>
            <p className="mt-1 text-3xl font-bold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              {queueCount}
            </p>
          </div>
          <div className="rounded-xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-sm p-6 dark:border dark:border-gray-700">
            <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              오늘 상담 수
            </p>
            <p className="mt-1 text-3xl font-bold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              3
            </p>
          </div>
          <div className="rounded-xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-sm p-6 dark:border dark:border-gray-700">
            <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              평균 상담 시간
            </p>
            <p className="mt-1 text-3xl font-bold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              23분
            </p>
          </div>
        </div>

        {/* Queue list */}
        <div className="rounded-xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-sm dark:border dark:border-gray-700">
          <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <h2 className="text-base font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              대기 고객
            </h2>
          </div>
          <QueueList />
        </div>

        {/* Recent sessions */}
        <div className="rounded-xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-sm dark:border dark:border-gray-700">
          <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <h2 className="text-base font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              최근 상담
            </h2>
          </div>
          <ul className="divide-y divide-gray-200 dark:divide-gray-700">
            {recentSessions.map((session) => (
              <li key={session.id} className="flex items-center justify-between px-6 py-3">
                <div className="flex flex-col">
                  <span className="text-sm font-medium text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
                    {session.customerName}
                  </span>
                  <span className="text-xs text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                    {session.memo}
                  </span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                    {session.duration}
                  </span>
                  <span className="inline-flex items-center rounded-full bg-(--color-bg-surface) dark:bg-(--color-bg-elevated-dark) px-2.5 py-0.5 text-xs font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                    {session.type}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </SidebarLayout>
  );
}
