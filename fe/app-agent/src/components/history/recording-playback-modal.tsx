'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { XMarkIcon, VideoCameraIcon } from '@heroicons/react/24/outline';
import api from '@/lib/api';
import { HistoryRecording } from '@/hooks/use-history-detail';

interface RecordingPlaybackModalProps {
  recording: HistoryRecording;
  onClose: () => void;
}

function formatDuration(seconds: number | null): string {
  if (seconds == null) return '-';
  if (seconds < 60) return `${seconds}초`;
  const minutes = Math.floor(seconds / 60);
  const remaining = seconds % 60;
  return remaining > 0 ? `${minutes}분 ${remaining}초` : `${minutes}분`;
}

function formatDateTime(iso: string | null): string {
  if (!iso) return '-';
  const d = new Date(iso);
  return d.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export const RecordingPlaybackModal = ({ recording, onClose }: RecordingPlaybackModalProps) => {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const blobUrlRef = useRef<string | null>(null);

  const fetchRecordingBlob = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await api.get(`/api/recordings/${recording.recordingId}/stream`, {
        responseType: 'blob',
      });
      const url = URL.createObjectURL(response.data as Blob);
      blobUrlRef.current = url;
      setBlobUrl(url);
    } catch {
      setError('녹화 파일을 불러오는 데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, [recording.recordingId]);

  useEffect(() => {
    fetchRecordingBlob();
    return () => {
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current);
        blobUrlRef.current = null;
      }
    };
  }, [fetchRecordingBlob]);

  // Close on ESC key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
      onClick={handleBackdropClick}
      role="dialog"
      aria-modal="true"
      aria-label="녹화 재생"
    >
      <div className="relative w-full max-w-3xl mx-4 rounded-2xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-2">
            <VideoCameraIcon className="h-5 w-5 text-indigo-500 dark:text-indigo-400" aria-hidden="true" />
            <h2 className="text-base font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              녹화 재생
            </h2>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark) transition-colors"
            aria-label="닫기"
          >
            <XMarkIcon className="h-5 w-5" />
          </button>
        </div>

        {/* Video area */}
        <div className="p-6 space-y-4">
          <div className="aspect-video rounded-xl overflow-hidden bg-black flex items-center justify-center">
            {isLoading ? (
              <div className="flex flex-col items-center gap-3 text-white">
                <div className="h-8 w-8 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                <p className="text-sm">녹화 파일을 불러오는 중...</p>
              </div>
            ) : error ? (
              <div className="flex flex-col items-center gap-2 text-white/70 px-8 text-center">
                <VideoCameraIcon className="h-10 w-10 opacity-40" aria-hidden="true" />
                <p className="text-sm">{error}</p>
              </div>
            ) : blobUrl ? (
              <video
                src={blobUrl}
                controls
                autoPlay
                className="w-full h-full object-contain"
                aria-label="상담 녹화 영상"
              />
            ) : null}
          </div>

          {/* Recording metadata */}
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            <div>
              <p className="text-xs font-medium text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                녹화 시작
              </p>
              <p className="mt-0.5 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
                {formatDateTime(recording.startedAt)}
              </p>
            </div>
            <div>
              <p className="text-xs font-medium text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                녹화 종료
              </p>
              <p className="mt-0.5 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
                {formatDateTime(recording.stoppedAt)}
              </p>
            </div>
            <div>
              <p className="text-xs font-medium text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                녹화 시간
              </p>
              <p className="mt-0.5 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
                {formatDuration(recording.durationSeconds)}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
