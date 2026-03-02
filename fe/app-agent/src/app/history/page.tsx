'use client';

import { useState, useCallback, useMemo } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { HistoryFiltersBar } from '@/components/history/history-filters';
import { HistoryTable } from '@/components/history/history-table';
import { RecordingPlaybackModal } from '@/components/history/recording-playback-modal';
import { useHistory, HistoryFilters } from '@/hooks/use-history';
import { useHistoryDetail } from '@/hooks/use-history-detail';

export default function HistoryPage() {
  const [filters, setFilters] = useState<HistoryFilters>({});
  const [selectedChannelId, setSelectedChannelId] = useState<string | null>(null);

  const {
    data,
    isLoading,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useHistory(filters);

  const { data: detail } = useHistoryDetail(selectedChannelId);

  const items = useMemo(
    () => data?.pages.flatMap((page) => page.items) ?? [],
    [data],
  );

  const handleLoadMore = useCallback(() => {
    fetchNextPage();
  }, [fetchNextPage]);

  const handleRowClick = useCallback((channelId: string) => {
    setSelectedChannelId(channelId);
  }, []);

  const handleCloseModal = useCallback(() => {
    setSelectedChannelId(null);
  }, []);

  const activeRecording = detail?.recording ?? null;

  return (
    <SidebarLayout>
      <div className="p-6 space-y-6">
        {/* Header */}
        <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
          상담 이력
        </h1>

        {/* Filters */}
        <HistoryFiltersBar filters={filters} onChange={setFilters} />

        {/* Table */}
        <HistoryTable
          items={items}
          isLoading={isLoading}
          isFetchingNextPage={isFetchingNextPage}
          hasNextPage={!!hasNextPage}
          onLoadMore={handleLoadMore}
          onRowClick={handleRowClick}
        />

        {/* Recording playback modal — shown when a channel is selected and it has recordings */}
        {selectedChannelId && activeRecording && (
          <RecordingPlaybackModal
            channelId={selectedChannelId}
            recording={activeRecording}
            onClose={handleCloseModal}
          />
        )}

        {/* Detail panel shown for channel with no recording: brief loading indicator */}
        {selectedChannelId && detail && !activeRecording && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={handleCloseModal}
            role="dialog"
            aria-modal="true"
            aria-label="상담 이력 상세"
          >
            <div
              className="w-full max-w-lg mx-4 rounded-2xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-xl p-6 space-y-4"
              onClick={(e) => e.stopPropagation()}
            >
              <h2 className="text-base font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
                상담 상세
              </h2>
              <div className="space-y-2">
                <p className="text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                  <span className="font-medium">고객:</span> {detail.customerName ?? '-'}
                </p>
                <p className="text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                  <span className="font-medium">상담사:</span> {detail.agentName ?? '-'}
                </p>
                {detail.feedback && (
                  <p className="text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                    <span className="font-medium">평점:</span> {detail.feedback.rating}
                    {detail.feedback.comment && ` — ${detail.feedback.comment}`}
                  </p>
                )}
                {detail.counselNote && (
                  <div>
                    <p className="text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) mb-1">
                      상담 노트
                    </p>
                    <p className="text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) rounded-lg bg-(--color-bg-surface) dark:bg-(--color-bg-elevated-dark) px-3 py-2">
                      {detail.counselNote.content}
                    </p>
                  </div>
                )}
                {!detail.recording && (
                  <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                    녹화 파일이 없습니다.
                  </p>
                )}
              </div>
              <button
                onClick={handleCloseModal}
                className="w-full rounded-lg border border-gray-300 dark:border-gray-600 px-4 py-2 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark) transition-colors"
                aria-label="닫기"
              >
                닫기
              </button>
            </div>
          </div>
        )}
      </div>
    </SidebarLayout>
  );
}
