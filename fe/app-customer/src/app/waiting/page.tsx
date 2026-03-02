'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { ThemeToggle } from '@/components/layout/theme-toggle';
import useCustomerStore from '@/stores/customer-store';
import { useEnterQueue, useLeaveQueue, useQueuePositionStream } from '@/hooks/use-queue';

export default function WaitingPage() {
  const router = useRouter();
  const {
    customerName,
    customerContact,
    entryId,
    queuePosition,
    setEntryId,
    setQueuePosition,
    setChannelId,
    reset,
  } = useCustomerStore();

  const enterQueue = useEnterQueue();
  const leaveQueue = useLeaveQueue();
  const positionStream = useQueuePositionStream(entryId);

  // If no customer info, redirect to join page
  useEffect(() => {
    if (!customerName || !customerContact) {
      router.replace('/');
    }
  }, [customerName, customerContact, router]);

  // Enter queue on mount if no entryId yet
  useEffect(() => {
    if (entryId || !customerName || !customerContact) return;
    enterQueue.mutate(
      { name: customerName, contact: customerContact },
      {
        onSuccess: (data) => {
          setEntryId(data.entryId);
          setQueuePosition(data.position);
        },
      },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Sync position from SSE stream
  useEffect(() => {
    if (positionStream.position !== null) {
      setQueuePosition(positionStream.position);
    }
  }, [positionStream.position, setQueuePosition]);

  // Navigate to call when accepted (channelId received)
  useEffect(() => {
    if (positionStream.channelId) {
      setChannelId(positionStream.channelId);
      router.push(`/call/${positionStream.channelId}`);
    }
  }, [positionStream.channelId, setChannelId, router]);

  const handleLeave = async () => {
    if (entryId) {
      try {
        await leaveQueue.mutateAsync(entryId);
      } catch {
        // Continue to home even if leave fails
      }
    }
    reset();
    router.push('/');
  };

  const displayPosition = positionStream.position ?? queuePosition;
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

          {!isEntering && displayPosition !== null && displayPosition > 0 && (
            <div className="rounded-lg bg-indigo-50 dark:bg-indigo-900/20 px-4 py-3 mb-6">
              <p className="text-sm text-indigo-700 dark:text-indigo-300 font-medium">
                현재 대기 순서
              </p>
              <p className="text-3xl font-bold text-indigo-600 dark:text-indigo-400">
                {displayPosition}번
              </p>
              {positionStream.queueSize !== null && (
                <p className="text-xs text-indigo-500 dark:text-indigo-400 mt-1">
                  전체 대기 {positionStream.queueSize}명
                </p>
              )}
            </div>
          )}

          {!isEntering && displayPosition === null && !enterQueue.isError && (
            <div className="rounded-lg bg-(--color-bg-surface) dark:bg-(--color-bg-elevated-dark) px-4 py-3 mb-6">
              <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                대기열 정보를 가져오는 중...
              </p>
            </div>
          )}

          {(enterQueue.isError || positionStream.error) && (
            <div className="rounded-lg bg-red-50 dark:bg-red-900/20 px-4 py-3 mb-6">
              <p className="text-sm text-red-600 dark:text-red-400">
                {enterQueue.isError
                  ? '대기열 입장에 실패했습니다.'
                  : '연결이 끊어졌습니다.'}
              </p>
              {positionStream.error && (
                <button
                  onClick={positionStream.reconnect}
                  className="text-sm text-indigo-600 dark:text-indigo-400 underline mt-1"
                >
                  다시 연결
                </button>
              )}
            </div>
          )}

          <Button
            variant="secondary"
            onClick={handleLeave}
            disabled={leaveQueue.isPending}
            className="w-full"
            aria-label="대기열 나가기"
          >
            {leaveQueue.isPending ? '나가는 중...' : '대기열 나가기'}
          </Button>
        </div>
      </div>
    </div>
  );
}
