'use client';

import { useEffect, useState, RefObject } from 'react';

interface CoBrowsePointerOverlayProps {
  x: number;
  y: number;
  containerRef: RefObject<HTMLDivElement | null>;
}

export function CoBrowsePointerOverlay({ x, y, containerRef }: CoBrowsePointerOverlayProps) {
  const [pixelPos, setPixelPos] = useState<{ left: number; top: number } | null>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const { width, height } = container.getBoundingClientRect();
    setPixelPos({
      left: x * width,
      top: y * height,
    });
  }, [x, y, containerRef]);

  if (!pixelPos) return null;

  return (
    <div
      className="absolute z-50 pointer-events-none"
      style={{
        left: pixelPos.left,
        top: pixelPos.top,
        transform: 'translate(-4px, -4px)',
        transition: 'left 50ms ease-out, top 50ms ease-out',
      }}
      aria-hidden="true"
    >
      {/* Cursor SVG */}
      <svg
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        className="drop-shadow-lg"
        aria-hidden="true"
      >
        <path
          d="M5.5 3.5L19 12L12.5 13.5L9.5 20.5L5.5 3.5Z"
          fill="#3b82f6"
          stroke="white"
          strokeWidth="1.5"
          strokeLinejoin="round"
        />
      </svg>

      {/* Agent label */}
      <span className="absolute left-5 top-0 whitespace-nowrap rounded bg-blue-600 px-1.5 py-0.5 text-xs font-medium text-white shadow">
        Agent
      </span>
    </div>
  );
}
