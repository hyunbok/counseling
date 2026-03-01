'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { ThemeToggle } from '@/components/layout/theme-toggle';
import useCustomerStore from '@/stores/customer-store';
import { useEnterQueue, useLeaveQueue, useQueueStatus } from '@/hooks/use-queue';

export default function WaitingPage() {
  const router = useRouter();
  const { queuePosition, customerName, customerContact, channelId, setChannelId, setQueuePosition } =
    useCustomerStore();

  const enterQueue = useEnterQueue();
  const leaveQueue = useLeaveQueue();
  const queueStatus = useQueueStatus(channelId);

  // Enter queue on mount if not already in one
  useEffect(() => {
    if (channelId || !customerName || !customerContact) return;
    enterQueue.mutate(
      { customerName, customerContact },
      {
        onSuccess: (data) => {
          if (data?.channelId) setChannelId(data.channelId);
        },
      },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Sync queue position from polling into Zustand
  useEffect(() => {
    if (queueStatus.data?.position !== undefined) {
      setQueuePosition(queueStatus.data.position);
    }
  }, [queueStatus.data, setQueuePosition]);

  const handleLeave = async () => {
    if (channelId) {
      await leaveQueue.mutateAsync(channelId);
    }
    router.push('/');
  };

  const isEntering = enterQueue.isPending;

  return (
    <div className="flex min-h-screen items-center justify-center bg-(--color-bg-surface) dark:bg-(--color-bg-base-dark) px-4">
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>

      <div className="w-full max-w-md">
        <div className="rounded-xl bg-(--color-bg-base) shadow-sm p-6 dark:bg-(--color-bg-surface-dark) dark:border dark:border-gray-700 text-center">
          <div className="flex justify-center mb-6">
            <div
              className="h-16 w-16 rounded-full border-4 border-indigo-200 border-t-indigo-600 animate-spin dark:border-gray-600 dark:border-t-indigo-400"
              role="status"
              aria-label="대기 중"
            />
          </div>

          <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark) mb-2">
            상담사 연결 대기 중
          </h1>

          {customerName && (
            <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) mb-4">
              {customerName}님, 잠시만 기다려 주세요.
            </p>
          )}

          {isEntering && (
            <div className="rounded-lg bg-(--color-bg-surface) dark:bg-(--color-bg-elevated-dark) px-4 py-3 mb-6">
              <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                대기열에 입장하는 중...
              </p>
            </div>
          )}

          {!isEntering && queuePosition !== null && (
            <div className="rounded-lg bg-indigo-50 dark:bg-indigo-900/20 px-4 py-3 mb-6">
              <p className="text-sm text-indigo-700 dark:text-indigo-300 font-medium">
                현재 대기 순서
              </p>
              <p className="text-3xl font-bold text-indigo-600 dark:text-indigo-400">
                {queuePosition}번
              </p>
            </div>
          )}

          {!isEntering && queuePosition === null && (
            <div className="rounded-lg bg-(--color-bg-surface) dark:bg-(--color-bg-elevated-dark) px-4 py-3 mb-6">
              <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                대기열 정보를 가져오는 중...
              </p>
            </div>
          )}

          {enterQueue.isError && (
            <p className="text-sm text-red-500 dark:text-red-400 mb-4">
              대기열 입장에 실패했습니다. 다시 시도해 주세요.
            </p>
          )}

          <Button
            variant="secondary"
            onClick={handleLeave}
            disabled={leaveQueue.isPending}
            className="w-full"
            aria-label="대기열 나가기"
          >
            대기열 나가기
          </Button>
        </div>
      </div>
    </div>
  );
}
