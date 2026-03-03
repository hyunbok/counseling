'use client';

import { useState, useEffect, useRef } from 'react';
import { useRoomContext } from '@livekit/components-react';
import { RoomEvent } from 'livekit-client';

export interface RemotePointer {
  x: number;
  y: number;
}

export interface HighlightRect {
  x: number;
  y: number;
  w: number;
  h: number;
}

type CoBrowseMessage =
  | { type: 'pointer'; x: number; y: number }
  | { type: 'highlight'; x: number; y: number; w: number; h: number }
  | { type: 'clear_highlight' };

export function useCoBrowseDataChannel() {
  const room = useRoomContext();
  const [remotePointer, setRemotePointer] = useState<RemotePointer | null>(null);
  const [highlightRect, setHighlightRect] = useState<HighlightRect | null>(null);
  const pointerClearTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    const decoder = new TextDecoder();

    const handleDataReceived = (payload: Uint8Array) => {
      try {
        const text = decoder.decode(payload);
        const msg: CoBrowseMessage = JSON.parse(text);

        if (msg.type === 'pointer') {
          if (pointerClearTimerRef.current) {
            clearTimeout(pointerClearTimerRef.current);
          }
          setRemotePointer({ x: msg.x, y: msg.y });
          pointerClearTimerRef.current = setTimeout(() => {
            setRemotePointer(null);
          }, 2000);
        } else if (msg.type === 'highlight') {
          setHighlightRect({ x: msg.x, y: msg.y, w: msg.w, h: msg.h });
        } else if (msg.type === 'clear_highlight') {
          setHighlightRect(null);
        }
      } catch {
        // Skip malformed messages
      }
    };

    room.on(RoomEvent.DataReceived, handleDataReceived);

    return () => {
      room.off(RoomEvent.DataReceived, handleDataReceived);
      if (pointerClearTimerRef.current) {
        clearTimeout(pointerClearTimerRef.current);
      }
    };
  }, [room]);

  return { remotePointer, highlightRect };
}
