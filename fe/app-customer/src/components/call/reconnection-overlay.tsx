'use client';

import type { ConnectionStatus } from '@/hooks/use-reconnection';

interface ReconnectionOverlayProps {
  status: ConnectionStatus;
  retryCount: number;
  elapsedMs: number;
}

function formatTime(ms: number): string {
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

export function ReconnectionOverlay({ status, retryCount, elapsedMs }: ReconnectionOverlayProps) {
  if (status === 'connected') {
    return null;
  }

  if (status === 'disconnected') {
    return (
      <div
        className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
        role="alert"
        aria-live="assertive"
      >
        <div className="flex flex-col items-center gap-4 rounded-2xl bg-gray-800 border border-gray-700 p-8 shadow-2xl">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-900/50 border border-red-700">
            <svg
              className="h-7 w-7 text-red-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={1.5}
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"
              />
            </svg>
          </div>
          <p className="text-base font-semibold text-white">연결이 끊어졌습니다</p>
        </div>
      </div>
    );
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
      role="alert"
      aria-live="polite"
    >
      <div className="flex flex-col items-center gap-4 rounded-2xl bg-gray-800 border border-gray-700 p-8 shadow-2xl">
        <div className="h-12 w-12 rounded-full border-4 border-gray-600 border-t-indigo-400 animate-spin" aria-hidden="true" />
        <p className="text-base font-semibold text-white">네트워크 재연결 중...</p>
        <div className="flex flex-col items-center gap-1">
          <p className="text-sm text-gray-400">
            재시도 횟수: <span className="text-indigo-400 font-medium">{retryCount}</span>
          </p>
          <p className="text-sm text-gray-400">
            경과 시간: <span className="text-indigo-400 font-medium tabular-nums">{formatTime(elapsedMs)}</span>
          </p>
        </div>
      </div>
    </div>
  );
}
