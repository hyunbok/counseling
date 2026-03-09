'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { useMutation } from '@tanstack/react-query';
import useCallStore from '@/stores/call-store';
import { CheckIcon, ArrowUpTrayIcon } from '@heroicons/react/24/outline';
import api from '@/lib/api';

interface NotePanelProps {
  channelId: string;
}

interface CounselNoteResponse {
  id: string;
  channelId: string;
  agentId: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export const NotePanel = ({ channelId }: NotePanelProps) => {
  const notesDraft = useCallStore((state) => state.notesDraft);
  const setNotesDraft = useCallStore((state) => state.setNotesDraft);
  const [localSaved, setLocalSaved] = useState(false);
  const [serverSaved, setServerSaved] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Save to server
  const saveMutation = useMutation({
    mutationFn: async (content: string) => {
      const { data } = await api.post<CounselNoteResponse>(
        `/api/channels/${channelId}/notes`,
        { content },
      );
      return data;
    },
    onSuccess: () => {
      setServerSaved(true);
      setTimeout(() => setServerSaved(false), 2000);
    },
  });

  const handleSaveToServer = useCallback(() => {
    if (notesDraft.trim()) {
      saveMutation.mutate(notesDraft);
    }
  }, [notesDraft, saveMutation]);

  // Load from server on mount, fallback to localStorage
  useEffect(() => {
    if (!channelId) return;

    api
      .get<CounselNoteResponse[]>(`/api/channels/${channelId}/notes`)
      .then(({ data }) => {
        if (data.length > 0 && !notesDraft) {
          setNotesDraft(data[0].content);
        }
      })
      .catch(() => {
        // Fallback to localStorage
        const saved = localStorage.getItem(`notes-${channelId}`);
        if (saved && !notesDraft) {
          setNotesDraft(saved);
        }
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [channelId]);

  // Auto-save to localStorage on debounce
  useEffect(() => {
    if (timerRef.current) clearTimeout(timerRef.current);

    timerRef.current = setTimeout(() => {
      if (channelId && notesDraft) {
        localStorage.setItem(`notes-${channelId}`, notesDraft);
        setLocalSaved(true);
        setTimeout(() => setLocalSaved(false), 2000);
      }
    }, 500);

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [notesDraft, channelId]);

  return (
    <div className="flex flex-col h-full p-3 gap-3">
      <textarea
        value={notesDraft}
        onChange={(e) => setNotesDraft(e.target.value)}
        placeholder="상담 메모를 입력하세요..."
        className="flex-1 resize-none rounded-lg bg-gray-700 border border-gray-600 px-3 py-2 text-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        aria-label="상담 메모"
      />
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          {localSaved && (
            <span className="flex items-center gap-1 text-xs text-gray-400">
              <CheckIcon className="h-3.5 w-3.5" />
              임시 저장
            </span>
          )}
          {serverSaved && (
            <span className="flex items-center gap-1 text-xs text-green-400">
              <CheckIcon className="h-3.5 w-3.5" />
              서버 저장 완료
            </span>
          )}
          {saveMutation.isError && (
            <span className="text-xs text-red-400">저장 실패</span>
          )}
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs text-gray-500">{notesDraft.length}자</span>
          <button
            onClick={handleSaveToServer}
            disabled={saveMutation.isPending || !notesDraft.trim()}
            className="flex items-center gap-1 rounded-lg bg-indigo-600 px-2.5 py-1.5 text-xs font-medium text-white hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            aria-label="메모 저장"
          >
            <ArrowUpTrayIcon className="h-3.5 w-3.5" />
            {saveMutation.isPending ? '저장 중...' : '저장'}
          </button>
        </div>
      </div>
    </div>
  );
};
