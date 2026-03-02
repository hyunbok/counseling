'use client';

import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { StatCard } from '@/components/ui/stat-card';
import { BuildingOfficeIcon, UsersIcon, ChartBarIcon, ChatBubbleLeftRightIcon } from '@heroicons/react/24/outline';
import { useAgentList } from '@/hooks/use-agents';
import { useActiveChannels, useAgentStatuses } from '@/hooks/use-monitoring';
import { useFeedbackList } from '@/hooks/use-feedbacks';
import { useTenantList } from '@/hooks/use-tenants';
import { useStatsSummary } from '@/hooks/use-stats';
import { useAuthGuard } from '@/hooks/use-auth-guard';

export default function DashboardPage() {
  const { isAuthenticated } = useAuthGuard();
  const { data: tenants } = useTenantList();
  const { data: agents } = useAgentList();
  const { data: channels } = useActiveChannels();
  const { data: feedbacks } = useFeedbackList();
  const { data: stats } = useStatsSummary();
  const { data: agentStatuses } = useAgentStatuses();

  if (!isAuthenticated) return null;

  const onlineAgents = agentStatuses?.filter((a) => a.status !== 'OFFLINE').length ?? 0;

  return (
    <SidebarLayout>
      <div className="p-8 space-y-6">
        <div>
          <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
            대시보드
          </h1>
          <p className="mt-1 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
            전체 현황을 확인하세요.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard label="테넌트" value={tenants?.length ?? '-'} icon={BuildingOfficeIcon} />
          <StatCard label="상담사" value={agents?.length ?? '-'} icon={UsersIcon} />
          <StatCard label="진행 중 세션" value={channels?.length ?? '-'} icon={ChartBarIcon} />
          <StatCard label="피드백" value={feedbacks?.length ?? '-'} icon={ChatBubbleLeftRightIcon} />
        </div>

        {/* Stats summary */}
        {stats && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="전체 상담" value={stats.totalChannels} />
            <StatCard label="완료 상담" value={stats.completedChannels} />
            <StatCard label="평균 평점" value={stats.averageRating.toFixed(1)} />
            <StatCard label="온라인 상담사" value={onlineAgents} />
          </div>
        )}

        <div className="rounded-[--radius-card] bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) border border-gray-200 dark:border-gray-700 p-6">
          <h2 className="text-base font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark) mb-4">
            실시간 세션
          </h2>
          {channels && channels.length > 0 ? (
            <ul className="space-y-3">
              {channels.slice(0, 5).map((ch) => (
                <li key={ch.id} className="flex items-center justify-between text-sm">
                  <span className="text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                    채널 {ch.id.slice(0, 8)}...
                  </span>
                  <span
                    className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                      ch.status === 'OPEN' || ch.status === 'IN_PROGRESS'
                        ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                        : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
                    }`}
                  >
                    {ch.status}
                  </span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              현재 활동 중인 세션이 없습니다.
            </p>
          )}
        </div>
      </div>
    </SidebarLayout>
  );
}
