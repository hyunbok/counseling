'use client';

import { useEffect, useRef, useState } from 'react';
import useCallStore from '@/stores/call-store';
import { CheckIcon } from '@heroicons/react/24/outline';

interface NotePanelProps {
  channelId: string;
}

export const NotePanel = ({ channelId }: NotePanelProps) => {
  const notesDraft = useCallStore((state) => state.notesDraft);
  const setNotesDraft = useCallStore((state) => state.setNotesDraft);
  const [saved, setSaved] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Auto-save to localStorage on debounce
  useEffect(() => {
    if (timerRef.current) clearTimeout(timerRef.current);

    timerRef.current = setTimeout(() => {
      if (channelId && notesDraft) {
        localStorage.setItem(`notes-${channelId}`, notesDraft);
        setSaved(true);
        setTimeout(() => setSaved(false), 2000);
      }
    }, 500);

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [notesDraft, channelId]);

  // Load saved notes on mount
  useEffect(() => {
    if (!channelId) return;
    const savedNotes = localStorage.getItem(`notes-${channelId}`);
    if (savedNotes && !notesDraft) {
      setNotesDraft(savedNotes);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [channelId]);

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
        {saved && (
          <span className="flex items-center gap-1 text-xs text-green-400">
            <CheckIcon className="h-3.5 w-3.5" />
            자동 저장됨
          </span>
        )}
        <span className="text-xs text-gray-500 ml-auto">
          {notesDraft.length}자
        </span>
      </div>
    </div>
  );
};
