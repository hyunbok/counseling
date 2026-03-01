'use client';

import useCallStore from '@/stores/call-store';

export const NotePanel = () => {
  const notesDraft = useCallStore((state) => state.notesDraft);
  const setNotesDraft = useCallStore((state) => state.setNotesDraft);

  const handleSave = () => {
    // TODO: POST to API to persist notes
    alert('메모가 저장되었습니다.');
  };

  return (
    <div className="flex flex-col h-full p-3 gap-3">
      <textarea
        value={notesDraft}
        onChange={(e) => setNotesDraft(e.target.value)}
        placeholder="상담 메모를 입력하세요..."
        className="flex-1 resize-none rounded-lg bg-gray-700 border border-gray-600 px-3 py-2 text-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        aria-label="상담 메모"
      />
      <button
        onClick={handleSave}
        className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 transition-colors"
        aria-label="메모 저장"
      >
        메모 저장
      </button>
    </div>
  );
};
