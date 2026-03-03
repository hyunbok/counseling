'use client';

import { ComputerDesktopIcon } from '@heroicons/react/24/outline';
import type { CoBrowsingSession } from '@/hooks/use-cobrowse';

interface CoBrowseToolbarButtonProps {
  session: CoBrowsingSession | null;
  onRequest: () => void;
  onEnd: () => void;
}

export const CoBrowseToolbarButton = ({
  session,
  onRequest,
  onEnd,
}: CoBrowseToolbarButtonProps) => {
  if (!session || session.status === 'ENDED') {
    return (
      <button
        onClick={onRequest}
        className="flex items-center justify-center rounded-full p-3 text-gray-300 hover:bg-gray-700 transition-colors"
        aria-label="공동 브라우징 요청"
        aria-pressed={false}
      >
        <ComputerDesktopIcon className="h-6 w-6" />
      </button>
    );
  }

  if (session.status === 'REQUESTED') {
    return (
      <button
        disabled
        className="flex items-center justify-center rounded-full p-3 text-amber-400 animate-pulse cursor-not-allowed"
        aria-label="공동 브라우징 수락 대기 중"
        aria-pressed={false}
      >
        <ComputerDesktopIcon className="h-6 w-6" />
      </button>
    );
  }

  // ACTIVE
  return (
    <button
      onClick={() => onEnd()}
      className="flex items-center justify-center rounded-full bg-indigo-600 p-3 text-white hover:bg-indigo-700 transition-colors"
      aria-label="공동 브라우징 종료"
      aria-pressed={true}
    >
      <ComputerDesktopIcon className="h-6 w-6" />
    </button>
  );
};
