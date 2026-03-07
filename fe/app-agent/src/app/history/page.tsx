'use client';

import { useState, useCallback } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { HistoryFiltersBar } from '@/components/history/history-filters';
import { HistoryTable } from '@/components/history/history-table';
import { RecordingPlaybackModal } from '@/components/history/recording-playback-modal';
import { useHistory, HistoryFilters } from '@/hooks/use-history';
import { useHistoryDetail } from '@/hooks/use-history-detail';
import { useChatHistory } from '@/hooks/use-chat-history';

export default function HistoryPage() {
  const [filters, setFilters] = useState<HistoryFilters>({});
  const [selectedChannelId, setSelectedChannelId] = useState<string | null>(null);

  const { data, isLoading } = useHistory(filters);

  const { data: detail } = useHistoryDetail(selectedChannelId);
  const { data: chatMessages } = useChatHistory(selectedChannelId);

  const items = data?.items ?? [];
  const page = data?.page ?? 0;
  const totalPages = data?.totalPages ?? 0;
  const totalCount = data?.totalCount ?? 0;

  const handlePageChange = useCallback((newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage }));
  }, []);

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
          page={page}
          totalPages={totalPages}
          totalCount={totalCount}
          onPageChange={handlePageChange}
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

        {/* Detail modal */}
        {selectedChannelId && detail && !activeRecording && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={handleCloseModal}
            role="dialog"
            aria-modal="true"
            aria-label="상담 이력 상세"
          >
            <div
              className="w-full max-w-xl mx-4 rounded-2xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-2xl overflow-hidden"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Header */}
              <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
                <h2 className="text-base font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
                  상담 이력 상세
                </h2>
                <div className="flex items-center gap-3">
                  <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium ${
                    detail.status === 'CLOSED' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                    : detail.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'
                    : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
                  }`}>
                    {detail.status === 'CLOSED' ? '완료' : detail.status === 'IN_PROGRESS' ? '진행 중' : detail.status}
                  </span>
                  <button
                    onClick={handleCloseModal}
                    className="flex items-center justify-center w-8 h-8 rounded-lg text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                    aria-label="닫기"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
                  </button>
                </div>
              </div>

              {/* Body */}
              <div className="px-6 py-5 space-y-5 max-h-[70vh] overflow-y-auto">
                {/* 상담 정보 */}
                <div className="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
                  <div className="px-4 py-2.5 bg-gray-50 dark:bg-gray-800/50 border-b border-gray-200 dark:border-gray-700">
                    <h3 className="text-xs font-semibold uppercase tracking-wider text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">상담 정보</h3>
                  </div>
                  <div className="divide-y divide-gray-100 dark:divide-gray-700/50">
                    {[
                      ['상담사', detail.agentName ?? '-'],
                      ['그룹', detail.groupName ?? '-'],
                      ['시작', detail.startedAt ? new Date(detail.startedAt).toLocaleString('ko-KR') : '-'],
                      ['종료', detail.endedAt ? new Date(detail.endedAt).toLocaleString('ko-KR') : '-'],
                      ['소요 시간', detail.durationSeconds != null ? `${Math.floor(detail.durationSeconds / 60)}분 ${detail.durationSeconds % 60}초` : '-'],
                    ].map(([label, value]) => (
                      <div key={label} className="flex items-center justify-between px-4 py-2.5 text-sm">
                        <span className="text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">{label}</span>
                        <span className="text-(--color-text-primary) dark:text-(--color-text-primary-dark) font-medium">{value}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* 고객 정보 */}
                <div className="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
                  <div className="px-4 py-2.5 bg-gray-50 dark:bg-gray-800/50 border-b border-gray-200 dark:border-gray-700">
                    <h3 className="text-xs font-semibold uppercase tracking-wider text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">고객 정보</h3>
                  </div>
                  <div className="divide-y divide-gray-100 dark:divide-gray-700/50">
                    {[
                      ['이름', detail.customerName ?? '-'],
                      ['연락처', detail.customerContact ?? '-'],
                      ...(detail.customerDevice ? [
                        ['디바이스', detail.customerDevice.deviceType ?? '-'],
                        ['브랜드', detail.customerDevice.deviceBrand ?? '-'],
                        ['OS', [detail.customerDevice.osName, detail.customerDevice.osVersion].filter(Boolean).join(' ') || '-'],
                        ['브라우저', [detail.customerDevice.browserName, detail.customerDevice.browserVersion].filter(Boolean).join(' ') || '-'],
                      ] : []),
                    ].map(([label, value]) => (
                      <div key={label} className="flex items-center justify-between px-4 py-2.5 text-sm">
                        <span className="text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">{label}</span>
                        <span className="text-(--color-text-primary) dark:text-(--color-text-primary-dark) font-medium">{value}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* 녹화 */}
                <div className="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
                  <div className="px-4 py-2.5 bg-gray-50 dark:bg-gray-800/50 border-b border-gray-200 dark:border-gray-700">
                    <h3 className="text-xs font-semibold uppercase tracking-wider text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">녹화</h3>
                  </div>
                  <div className="px-4 py-3 text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                    {detail.recording ? (
                      <span className="inline-flex items-center gap-1.5">
                        <span className="w-2 h-2 rounded-full bg-green-500" />
                        녹화 완료
                      </span>
                    ) : (
                      <span className="text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">녹화 파일이 없습니다.</span>
                    )}
                  </div>
                </div>

                {/* 고객 피드백 */}
                {detail.feedback && (
                  <div className="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
                    <div className="px-4 py-2.5 bg-gray-50 dark:bg-gray-800/50 border-b border-gray-200 dark:border-gray-700">
                      <h3 className="text-xs font-semibold uppercase tracking-wider text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">고객 피드백</h3>
                    </div>
                    <div className="px-4 py-3 space-y-2">
                      <div className="flex items-center gap-1">
                        {[1, 2, 3, 4, 5].map((i) => (
                          <svg key={i} xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill={i <= detail.feedback!.rating ? '#f59e0b' : 'none'} stroke={i <= detail.feedback!.rating ? '#f59e0b' : '#d1d5db'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                        ))}
                        <span className="ml-1.5 text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">{detail.feedback.rating}/5</span>
                      </div>
                      {detail.feedback.comment && (
                        <p className="text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) leading-relaxed">
                          &ldquo;{detail.feedback.comment}&rdquo;
                        </p>
                      )}
                    </div>
                  </div>
                )}

                {/* 상담 노트 */}
                {detail.counselNote && (
                  <div className="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
                    <div className="px-4 py-2.5 bg-gray-50 dark:bg-gray-800/50 border-b border-gray-200 dark:border-gray-700">
                      <h3 className="text-xs font-semibold uppercase tracking-wider text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">상담 노트</h3>
                    </div>
                    <div className="px-4 py-3">
                      <p className="text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) whitespace-pre-wrap leading-relaxed">{detail.counselNote.content}</p>
                    </div>
                  </div>
                )}

                {/* 채팅 이력 */}
                <div className="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
                  <div className="px-4 py-2.5 bg-gray-50 dark:bg-gray-800/50 border-b border-gray-200 dark:border-gray-700">
                    <h3 className="text-xs font-semibold uppercase tracking-wider text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                      채팅 이력
                      {chatMessages && chatMessages.length > 0 && (
                        <span className="ml-1.5 text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) font-normal normal-case tracking-normal">({chatMessages.length})</span>
                      )}
                    </h3>
                  </div>
                  <div className="max-h-64 overflow-y-auto">
                    {!chatMessages || chatMessages.length === 0 ? (
                      <div className="px-4 py-3 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                        채팅 내역이 없습니다.
                      </div>
                    ) : (
                      <div className="px-4 py-3 space-y-2.5">
                        {chatMessages.map((msg) => (
                          <div
                            key={msg.id}
                            className={`flex flex-col ${msg.senderType === 'AGENT' ? 'items-end' : 'items-start'}`}
                          >
                            <div className={`max-w-[80%] rounded-xl px-3 py-2 text-sm ${
                              msg.senderType === 'AGENT'
                                ? 'bg-indigo-100 dark:bg-indigo-900/30 text-indigo-900 dark:text-indigo-200'
                                : msg.senderType === 'SYSTEM'
                                  ? 'bg-gray-100 dark:bg-gray-700 text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) text-center w-full max-w-full text-xs'
                                  : 'bg-gray-100 dark:bg-gray-700 text-(--color-text-primary) dark:text-(--color-text-primary-dark)'
                            }`}>
                              {msg.senderType !== 'SYSTEM' && (
                                <p className="text-xs font-medium mb-0.5 opacity-70">
                                  {msg.senderType === 'AGENT' ? '상담사' : '고객'}
                                </p>
                              )}
                              <p className="whitespace-pre-wrap">{msg.content}</p>
                            </div>
                            <span className="text-[10px] mt-0.5 text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                              {new Date(msg.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </SidebarLayout>
  );
}
