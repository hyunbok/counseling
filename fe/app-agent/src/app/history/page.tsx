'use client';

import { useState } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import { MagnifyingGlassIcon } from '@heroicons/react/24/outline';

interface SessionRecord {
  id: string;
  date: string;
  customerName: string;
  type: string;
  duration: string;
  memoSummary: string;
}

const PLACEHOLDER_SESSIONS: SessionRecord[] = [
  {
    id: '1',
    date: '2026-03-01 14:30',
    customerName: '김민준',
    type: '일반 상담',
    duration: '23분',
    memoSummary: '계약 관련 문의 — 갱신 조건 안내 완료',
  },
  {
    id: '2',
    date: '2026-03-01 11:15',
    customerName: '이서연',
    type: '기술 지원',
    duration: '15분',
    memoSummary: '앱 오류 해결 — 재설치 후 정상 작동',
  },
  {
    id: '3',
    date: '2026-02-28 16:00',
    customerName: '박지훈',
    type: '일반 상담',
    duration: '31분',
    memoSummary: '해지 상담 — 혜택 안내 후 유지',
  },
  {
    id: '4',
    date: '2026-02-28 10:45',
    customerName: '최수아',
    type: '결제 문의',
    duration: '9분',
    memoSummary: '과금 오류 확인 — 환불 처리 예정',
  },
  {
    id: '5',
    date: '2026-02-27 15:20',
    customerName: '정도현',
    type: '일반 상담',
    duration: '27분',
    memoSummary: '서비스 안내 — 요금제 변경 완료',
  },
];

export default function HistoryPage() {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [search, setSearch] = useState('');

  const filtered = PLACEHOLDER_SESSIONS.filter((s) => {
    const matchesSearch =
      !search.trim() ||
      s.customerName.includes(search.trim()) ||
      s.memoSummary.includes(search.trim());
    return matchesSearch;
  });

  return (
    <SidebarLayout>
      <div className="p-6 space-y-6">
        {/* Header */}
        <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
          상담 이력
        </h1>

        {/* Filters */}
        <div className="flex flex-wrap gap-3 items-end">
          <div className="flex flex-col gap-1">
            <label
              htmlFor="start-date"
              className="text-xs font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
            >
              시작일
            </label>
            <input
              id="start-date"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm text-(--color-text-primary) dark:bg-(--color-bg-elevated-dark) dark:border-gray-600 dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label
              htmlFor="end-date"
              className="text-xs font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
            >
              종료일
            </label>
            <input
              id="end-date"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm text-(--color-text-primary) dark:bg-(--color-bg-elevated-dark) dark:border-gray-600 dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div className="relative flex-1 min-w-48">
            <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="고객명 또는 메모 검색"
              className="w-full rounded-lg border border-gray-300 pl-9 pr-3 py-2 text-sm text-(--color-text-primary) dark:bg-(--color-bg-elevated-dark) dark:border-gray-600 dark:text-(--color-text-primary-dark) placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              aria-label="상담 이력 검색"
            />
          </div>
        </div>

        {/* Table */}
        <div className="rounded-xl bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-sm dark:border dark:border-gray-700 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-(--color-bg-surface) dark:bg-(--color-bg-elevated-dark) border-b border-gray-200 dark:border-gray-700">
                <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                  상담일시
                </th>
                <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                  고객명
                </th>
                <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                  유형
                </th>
                <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                  소요시간
                </th>
                <th className="px-4 py-3 text-left font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                  메모 요약
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {filtered.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-4 py-12 text-center text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)"
                  >
                    상담 이력이 없습니다.
                  </td>
                </tr>
              ) : (
                filtered.map((session) => (
                  <tr
                    key={session.id}
                    className="hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark)/50 transition-colors"
                  >
                    <td className="px-4 py-3 text-(--color-text-primary) dark:text-(--color-text-primary-dark) whitespace-nowrap">
                      {session.date}
                    </td>
                    <td className="px-4 py-3 font-medium text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
                      {session.customerName}
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center rounded-full bg-(--color-bg-surface) dark:bg-(--color-bg-elevated-dark) px-2.5 py-0.5 text-xs font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                        {session.type}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                      {session.duration}
                    </td>
                    <td className="px-4 py-3 text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) max-w-xs truncate">
                      {session.memoSummary}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination placeholder */}
        <div className="flex items-center justify-between">
          <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
            총 {filtered.length}건
          </p>
          <div className="flex items-center gap-2">
            <button
              disabled
              className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) disabled:opacity-50"
              aria-label="이전 페이지"
            >
              이전
            </button>
            <span className="text-sm font-medium text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              1
            </span>
            <button
              disabled
              className="rounded-lg border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) disabled:opacity-50"
              aria-label="다음 페이지"
            >
              다음
            </button>
          </div>
        </div>
      </div>
    </SidebarLayout>
  );
}
