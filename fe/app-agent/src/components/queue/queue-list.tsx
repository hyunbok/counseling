'use client';

import { Button } from '@/components/ui/button';
import { useQueueList, useAcceptQueue } from '@/hooks/use-queue';
import { useQueueStream } from '@/hooks/use-queue-stream';
import { useRouter } from 'next/navigation';
import useCallStore from '@/stores/call-store';

const formatWaitTime = (seconds: number): string => {
  if (seconds < 60) return `${seconds}초`;
  const minutes = Math.floor(seconds / 60);
  return `${minutes}분`;
};

export const QueueList = () => {
  const router = useRouter();
  const { data: queue, isLoading, isError } = useQueueList();
  const acceptQueue = useAcceptQueue();
  const setChannel = useCallStore((state) => state.setChannel);

  useQueueStream();

  const handleAccept = (queueId: string, customerName: string) => {
    acceptQueue.mutate(queueId, {
      onSuccess: (data) => {
        const channelId = data?.channelId ?? queueId;
        setChannel(channelId, customerName, data?.agentToken, data?.livekitUrl);
        router.push(`/call/${channelId}`);
      },
    });
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
          대기열을 불러오는 중...
        </p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-sm text-red-500 dark:text-red-400">
          대기열을 불러오는 데 실패했습니다.
        </p>
      </div>
    );
  }

  if (!queue || queue.length === 0) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
          대기 중인 고객이 없습니다.
        </p>
      </div>
    );
  }

  return (
    <ul className="divide-y divide-gray-200 dark:divide-gray-700">
      {queue.map((item) => {
        const initial = item.customerName.charAt(0).toUpperCase();
        return (
          <li
            key={item.id}
            className="flex items-center justify-between px-4 py-3 hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark) transition-colors"
          >
            <div className="flex items-center gap-3">
              {/* Avatar initial circle */}
              <div
                className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-indigo-100 dark:bg-indigo-900/40"
                aria-hidden="true"
              >
                <span className="text-sm font-semibold text-indigo-700 dark:text-indigo-300">
                  {initial}
                </span>
              </div>
              <div>
                <p className="text-sm font-medium text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
                  {item.customerName}
                </p>
                <span className="inline-flex items-center rounded-full bg-amber-50 dark:bg-amber-900/20 px-2 py-0.5 text-xs font-medium text-amber-700 dark:text-amber-300">
                  {formatWaitTime(item.waitTime)} 대기 중
                </span>
              </div>
            </div>
            <Button
              variant="primary"
              className="text-sm px-3 py-1.5"
              onClick={() => handleAccept(item.id, item.customerName)}
              disabled={acceptQueue.isPending}
              aria-label={`${item.customerName} 수락`}
            >
              수락
            </Button>
          </li>
        );
      })}
    </ul>
  );
};
