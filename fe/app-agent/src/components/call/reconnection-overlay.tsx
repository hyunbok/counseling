'use client';

import type { ConnectionStatus } from '@/hooks/use-reconnection';

interface ReconnectionOverlayProps {
  status: ConnectionStatus;
  retryCount: number;
  elapsedMs: number;
}

const formatElapsedMs = (ms: number) => {
  const totalSeconds = Math.floor(ms / 1000);
  const m = Math.floor(totalSeconds / 60).toString().padStart(2, '0');
  const s = (totalSeconds % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
};

export const ReconnectionOverlay = ({ status, retryCount, elapsedMs }: ReconnectionOverlayProps) => {
  if (status === 'connected') return null;

  if (status === 'disconnected') {
    return (
      <div
        className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
        role="alert"
        aria-live="assertive"
      >
        <div className="flex flex-col items-center gap-4 rounded-2xl bg-gray-900/90 px-8 py-8 shadow-2xl border border-gray-700">
          <div className="h-12 w-12 rounded-full bg-red-500/20 flex items-center justify-center">
            <span className="h-5 w-5 rounded-full bg-red-500" />
          </div>
          <p className="text-base font-semibold text-white">연결이 끊어졌습니다</p>
          <p className="text-sm text-gray-400">대시보드로 이동합니다.</p>
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
      <div className="flex flex-col items-center gap-4 rounded-2xl bg-gray-900/90 px-8 py-8 shadow-2xl border border-gray-700">
        <div className="h-12 w-12 rounded-full border-4 border-gray-600 border-t-indigo-400 animate-spin" aria-hidden="true" />
        <p className="text-base font-semibold text-white">네트워크 재연결 중...</p>
        <div className="flex flex-col items-center gap-1">
          <p className="text-sm text-gray-400">
            재시도 횟수: <span className="font-mono text-amber-400">{retryCount}</span>
          </p>
          <p className="text-sm text-gray-400">
            경과 시간: <span className="font-mono text-amber-400">{formatElapsedMs(elapsedMs)}</span>
          </p>
        </div>
      </div>
    </div>
  );
};
