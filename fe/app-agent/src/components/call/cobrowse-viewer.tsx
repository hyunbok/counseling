'use client';

import { useRef, useState, useCallback } from 'react';
import { useRemoteParticipants, VideoTrack } from '@livekit/components-react';
import { Track } from 'livekit-client';
import { useCoBrowseDataChannel } from '@/hooks/use-cobrowse-data-channel';

interface HighlightRect {
  startX: number;
  startY: number;
  endX: number;
  endY: number;
}

interface CoBrowseViewerProps {
  onEnd: () => void;
}

export const CoBrowseViewer = ({ onEnd }: CoBrowseViewerProps) => {
  const remoteParticipants = useRemoteParticipants();
  const remote = remoteParticipants[0];
  const { sendPointer, sendHighlight, clearHighlight } = useCoBrowseDataChannel();

  const containerRef = useRef<HTMLDivElement>(null);
  const [isHighlighting, setIsHighlighting] = useState(false);
  const [pendingRect, setPendingRect] = useState<HighlightRect | null>(null);

  // Find the screen share track — prefer one named 'cobrowse'
  const screenSharePublication = (() => {
    if (!remote) return undefined;
    const publications = Array.from(remote.trackPublications.values());
    const named = publications.find(
      (p) => p.trackName === 'cobrowse' && p.source === Track.Source.ScreenShare,
    );
    return named ?? remote.getTrackPublication(Track.Source.ScreenShare);
  })();

  const getNormalizedCoords = useCallback(
    (clientX: number, clientY: number): { x: number; y: number } | null => {
      const el = containerRef.current;
      if (!el) return null;
      const rect = el.getBoundingClientRect();
      return {
        x: (clientX - rect.left) / rect.width,
        y: (clientY - rect.top) / rect.height,
      };
    },
    [],
  );

  const handleMouseMove = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      const coords = getNormalizedCoords(e.clientX, e.clientY);
      if (!coords) return;
      sendPointer(coords.x, coords.y);

      if (isHighlighting && pendingRect) {
        const el = containerRef.current;
        if (!el) return;
        const rect = el.getBoundingClientRect();
        setPendingRect((prev) =>
          prev
            ? {
                ...prev,
                endX: (e.clientX - rect.left) / rect.width,
                endY: (e.clientY - rect.top) / rect.height,
              }
            : null,
        );
      }
    },
    [getNormalizedCoords, sendPointer, isHighlighting, pendingRect],
  );

  const handleMouseDown = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      if (!isHighlighting) return;
      const el = containerRef.current;
      if (!el) return;
      const rect = el.getBoundingClientRect();
      const x = (e.clientX - rect.left) / rect.width;
      const y = (e.clientY - rect.top) / rect.height;
      setPendingRect({ startX: x, startY: y, endX: x, endY: y });
    },
    [isHighlighting],
  );

  const handleMouseUp = useCallback(() => {
    if (!isHighlighting || !pendingRect) return;
    const x = Math.min(pendingRect.startX, pendingRect.endX);
    const y = Math.min(pendingRect.startY, pendingRect.endY);
    const w = Math.abs(pendingRect.endX - pendingRect.startX);
    const h = Math.abs(pendingRect.endY - pendingRect.startY);
    if (w > 0.01 && h > 0.01) {
      sendHighlight({ x, y, w, h });
    }
    setPendingRect(null);
  }, [isHighlighting, pendingRect, sendHighlight]);

  const handleClearHighlight = useCallback(() => {
    clearHighlight();
    setPendingRect(null);
  }, [clearHighlight]);

  // Compute pending rect display coords
  const displayRect = pendingRect
    ? {
        left: `${Math.min(pendingRect.startX, pendingRect.endX) * 100}%`,
        top: `${Math.min(pendingRect.startY, pendingRect.endY) * 100}%`,
        width: `${Math.abs(pendingRect.endX - pendingRect.startX) * 100}%`,
        height: `${Math.abs(pendingRect.endY - pendingRect.startY) * 100}%`,
      }
    : null;

  return (
    <div className="relative flex h-full w-full flex-col bg-gray-950">
      {/* Video area */}
      <div
        ref={containerRef}
        className="relative flex-1 cursor-crosshair overflow-hidden"
        onMouseMove={handleMouseMove}
        onMouseDown={handleMouseDown}
        onMouseUp={handleMouseUp}
        role="presentation"
        aria-label="고객 화면 공유 뷰어"
      >
        {remote && screenSharePublication?.track ? (
          <VideoTrack
            trackRef={{
              participant: remote,
              publication: screenSharePublication,
              source: Track.Source.ScreenShare,
            }}
            className="h-full w-full object-contain"
          />
        ) : (
          <div className="flex h-full items-center justify-center">
            <p className="text-gray-400 text-sm">화면 공유 대기 중...</p>
          </div>
        )}

        {/* Pending highlight rect preview */}
        {displayRect && (
          <div
            className="pointer-events-none absolute border-2 border-yellow-400 bg-yellow-400/10"
            style={displayRect}
            aria-hidden="true"
          />
        )}
      </div>

      {/* Overlay toolbar */}
      <div className="flex items-center justify-center gap-3 bg-gray-900/90 px-4 py-2">
        <button
          onClick={() => {
            setIsHighlighting((prev) => !prev);
            setPendingRect(null);
          }}
          className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
            isHighlighting
              ? 'bg-yellow-500 text-gray-900 hover:bg-yellow-400'
              : 'bg-gray-700 text-gray-200 hover:bg-gray-600'
          }`}
          aria-pressed={isHighlighting}
          aria-label={isHighlighting ? '하이라이트 모드 끄기' : '하이라이트 모드 켜기'}
        >
          하이라이트
        </button>

        <button
          onClick={handleClearHighlight}
          className="rounded-lg bg-gray-700 px-3 py-1.5 text-sm font-medium text-gray-200 hover:bg-gray-600 transition-colors"
          aria-label="하이라이트 지우기"
        >
          지우기
        </button>

        <button
          onClick={onEnd}
          className="rounded-lg bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700 transition-colors"
          aria-label="공동 브라우징 종료"
        >
          종료
        </button>
      </div>
    </div>
  );
};
