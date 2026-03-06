'use client';

import { useMemo, useState, useCallback } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { StatCard } from '@/components/ui/stat-card';
import { BuildingOfficeIcon, UsersIcon, ChartBarIcon, ChatBubbleLeftRightIcon } from '@heroicons/react/24/outline';
import { useAgentList } from '@/hooks/use-agents';
import { useActiveChannels, useAgentStatuses } from '@/hooks/use-monitoring';
import { useFeedbackList } from '@/hooks/use-feedbacks';
import { useTenantList } from '@/hooks/use-tenants';
import { useStatsSummary } from '@/hooks/use-stats';
import { useAuthGuard } from '@/hooks/use-auth-guard';
import useAuthStore from '@/stores/auth-store';

export default function DashboardPage() {
  const { isAuthenticated } = useAuthGuard();
  const user = useAuthStore((state) => state.user);
  const selectedTenantId = useAuthStore((state) => state.selectedTenantId);

  // Tenant-dependent queries are only enabled when a tenant context is available:
  // - SUPER_ADMIN must explicitly select a tenant
  // - COMPANY_ADMIN / GROUP_ADMIN always have a tenantId on their user object
  const hasTenantContext = user?.role === 'SUPER_ADMIN' ? !!selectedTenantId : !!user?.tenantId;

  const { data: tenantsPage } = useTenantList({ size: 100 });
  const tenants = tenantsPage?.content;
  const { data: agents } = useAgentList({ enabled: hasTenantContext });
  const { data: channels } = useActiveChannels({ enabled: hasTenantContext });
  const { data: feedbacks } = useFeedbackList({ enabled: hasTenantContext });
  const { data: agentStatuses } = useAgentStatuses({ enabled: hasTenantContext });

  const toDateString = (d: Date) => d.toISOString().slice(0, 10);
  const [preset, setPreset] = useState<'today' | '7d' | '30d' | 'custom'>('30d');
  const [customFrom, setCustomFrom] = useState(() => toDateString(new Date(Date.now() - 30 * 86400000)));
  const [customTo, setCustomTo] = useState(() => toDateString(new Date()));

  const { from, to } = useMemo(() => {
    if (preset === 'custom') {
      return { from: new Date(customFrom).toISOString(), to: new Date(customTo + 'T23:59:59').toISOString() };
    }
    const now = new Date();
    const days = preset === 'today' ? 1 : preset === '7d' ? 7 : 30;
    const start = new Date(now.getTime() - days * 86400000);
    return { from: start.toISOString(), to: now.toISOString() };
  }, [preset, customFrom, customTo]);

  const handlePreset = useCallback((p: 'today' | '7d' | '30d' | 'custom') => {
    setPreset(p);
    if (p !== 'custom') {
      const now = new Date();
      const days = p === 'today' ? 1 : p === '7d' ? 7 : 30;
      setCustomFrom(toDateString(new Date(now.getTime() - days * 86400000)));
      setCustomTo(toDateString(now));
    }
  }, []);

  const { data: stats } = useStatsSummary(from, to, hasTenantContext);

  if (!isAuthenticated) return null;

  const onlineAgents = agentStatuses?.filter((a) => a.status !== 'OFFLINE').length ?? 0;
  const isSuperAdminWithoutTenant = user?.role === 'SUPER_ADMIN' && !selectedTenantId;

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

        {/* Date filter */}
        <div className="flex flex-wrap items-center gap-2">
          {(['today', '7d', '30d', 'custom'] as const).map((p) => {
            const label = { today: '오늘', '7d': '7일', '30d': '30일', custom: '직접 선택' }[p];
            return (
              <button
                key={p}
                onClick={() => handlePreset(p)}
                className={`px-3 py-1.5 text-sm rounded-md border transition-colors ${
                  preset === p
                    ? 'bg-(--color-primary) text-white border-(--color-primary)'
                    : 'bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) border-gray-300 dark:border-gray-600 hover:border-(--color-primary)'
                }`}
              >
                {label}
              </button>
            );
          })}
          {preset === 'custom' && (
            <div className="flex items-center gap-2 ml-2">
              <input
                type="date"
                value={customFrom}
                onChange={(e) => setCustomFrom(e.target.value)}
                className="rounded-md border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-2 py-1.5 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark)"
              />
              <span className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">~</span>
              <input
                type="date"
                value={customTo}
                onChange={(e) => setCustomTo(e.target.value)}
                className="rounded-md border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-2 py-1.5 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark)"
              />
            </div>
          )}
        </div>

        {isSuperAdminWithoutTenant && (
          <div
            role="status"
            className="rounded-[--radius-card] border border-yellow-200 dark:border-yellow-700/50 bg-yellow-50 dark:bg-yellow-900/20 px-5 py-4 text-sm text-yellow-800 dark:text-yellow-300"
          >
            사이드바에서 테넌트를 선택하면 상담사, 세션, 피드백 등 테넌트별 데이터를 확인할 수 있습니다.
          </div>
        )}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard label="테넌트" value={tenants?.length ?? '-'} icon={BuildingOfficeIcon} />
          <StatCard label="상담사" value={agents?.totalElements ?? '-'} icon={UsersIcon} />
          <StatCard label="진행 중 세션" value={channels?.filter((ch) => ch.status === 'OPEN' || ch.status === 'IN_PROGRESS').length ?? '-'} icon={ChartBarIcon} />
          <StatCard label="피드백" value={feedbacks?.totalElements ?? '-'} icon={ChatBubbleLeftRightIcon} />
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
