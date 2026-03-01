'use client';

import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { StatCard } from '@/components/ui/stat-card';
import { BuildingOfficeIcon, UsersIcon, ChartBarIcon, ChatBubbleLeftRightIcon } from '@heroicons/react/24/outline';
import { useAgentList } from '@/hooks/use-agents';
import { useActiveSessions } from '@/hooks/use-monitoring';
import { useFeedbackList } from '@/hooks/use-feedbacks';
import { useTenantList } from '@/hooks/use-tenants';
import { useAuthGuard } from '@/hooks/use-auth-guard';

export default function DashboardPage() {
  const { isAuthenticated } = useAuthGuard();
  const { data: tenantsData } = useTenantList({ size: 1 });
  const { data: agentsData } = useAgentList({ size: 1 });
  const { data: monitoringData } = useActiveSessions();
  const { data: feedbacksData } = useFeedbackList({ size: 1 });

  if (!isAuthenticated) return null;

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
          <StatCard label="테넌트" value={tenantsData?.totalElements ?? '-'} icon={BuildingOfficeIcon} />
          <StatCard label="상담사" value={agentsData?.totalElements ?? '-'} icon={UsersIcon} />
          <StatCard label="진행 중 세션" value={monitoringData?.totalActive ?? '-'} icon={ChartBarIcon} />
          <StatCard label="피드백" value={feedbacksData?.totalElements ?? '-'} icon={ChatBubbleLeftRightIcon} />
        </div>

        <div className="rounded-[--radius-card] bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) border border-gray-200 dark:border-gray-700 p-6">
          <h2 className="text-base font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark) mb-4">
            최근 활동
          </h2>
          {monitoringData?.sessions && monitoringData.sessions.length > 0 ? (
            <ul className="space-y-3">
              {monitoringData.sessions.slice(0, 5).map((session) => (
                <li key={session.id} className="flex items-center justify-between text-sm">
                  <span className="text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                    {session.agentName} — {session.clientName}
                  </span>
                  <span
                    className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                      session.status === 'IN_PROGRESS'
                        ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                        : session.status === 'WAITING'
                          ? 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400'
                          : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'
                    }`}
                  >
                    {session.status === 'IN_PROGRESS' ? '진행 중' : session.status === 'WAITING' ? '대기' : '종료'}
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
