'use client';

import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { QueueList } from '@/components/queue/queue-list';
import { useQueueList } from '@/hooks/use-queue';
import { useQueueStream } from '@/hooks/use-queue-stream';
import { useDashboardSummary } from '@/hooks/use-dashboard-summary';

export default function DashboardPage() {
  const { data: queue } = useQueueList();

  useQueueStream();

  const { data: summary } = useDashboardSummary();

  const todayCount = summary?.todayCount ?? 0;
  const totalDuration = summary?.totalDurationSeconds ?? null;
  const avgDuration =
    summary?.avgDurationSeconds != null ? Math.round(summary.avgDurationSeconds / 60) : null;
  const recentSessions = summary?.recentItems ?? [];

  const formatDuration = (seconds: number) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0) return `${h}시간 ${m}분`;
    return `${m}분`;
  };

  const queueCount = queue?.length ?? 0;

  return (
    <SidebarLayout>
      <div className="p-6 space-y-6">
        {/* Header */}
        <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
          대시보드
        </h1>

        {/* Summary cards */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
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
              {todayCount}
            </p>
          </div>
          <div className="rounded-xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-sm p-6 dark:border dark:border-gray-700">
            <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              총 상담 시간
            </p>
            <p className="mt-1 text-3xl font-bold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              {totalDuration != null ? formatDuration(totalDuration) : '-'}
            </p>
          </div>
          <div className="rounded-xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-sm p-6 dark:border dark:border-gray-700">
            <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              평균 상담 시간
            </p>
            <p className="mt-1 text-3xl font-bold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              {avgDuration != null ? `${avgDuration}분` : '-'}
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
          {recentSessions.length === 0 ? (
            <p className="px-6 py-6 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              오늘 진행한 상담이 없습니다.
            </p>
          ) : (
            <ul className="divide-y divide-gray-200 dark:divide-gray-700">
              {recentSessions.map((session) => (
                <li key={session.channelId} className="flex items-center justify-between px-6 py-3">
                  <div className="flex flex-col">
                    <span className="text-sm font-medium text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
                      {session.customerName ?? '알 수 없음'}
                    </span>
                    <span className="text-xs text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                      {session.status === 'IN_PROGRESS' ? '진행 중' : '완료'}
                    </span>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-xs text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                      {session.durationSeconds != null
                        ? `${Math.round(session.durationSeconds / 60)}분`
                        : '진행 중'}
                    </span>
                    {session.feedbackRating != null && (
                      <span className="inline-flex items-center rounded-full bg-(--color-bg-surface) dark:bg-(--color-bg-elevated-dark) px-2.5 py-0.5 text-xs font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                        ★ {session.feedbackRating}
                      </span>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </SidebarLayout>
  );
}
