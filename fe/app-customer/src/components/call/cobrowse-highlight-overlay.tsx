'use client';

import { useEffect, useState, RefObject } from 'react';

interface HighlightRect {
  x: number;
  y: number;
  w: number;
  h: number;
}

interface CoBrowseHighlightOverlayProps {
  rect: HighlightRect;
  containerRef: RefObject<HTMLDivElement | null>;
}

export function CoBrowseHighlightOverlay({ rect, containerRef }: CoBrowseHighlightOverlayProps) {
  const [pixelRect, setPixelRect] = useState<{
    left: number;
    top: number;
    width: number;
    height: number;
  } | null>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const { width, height } = container.getBoundingClientRect();
    setPixelRect({
      left: rect.x * width,
      top: rect.y * height,
      width: rect.w * width,
      height: rect.h * height,
    });
  }, [rect, containerRef]);

  if (!pixelRect) return null;

  return (
    <div
      className="absolute z-40 pointer-events-none animate-pulse border-2 border-blue-500 bg-blue-500/20 rounded"
      style={{
        left: pixelRect.left,
        top: pixelRect.top,
        width: pixelRect.width,
        height: pixelRect.height,
      }}
      aria-hidden="true"
    />
  );
}
