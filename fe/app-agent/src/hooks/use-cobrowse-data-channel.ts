import { useCallback, useRef } from 'react';
import { useRoomContext } from '@livekit/components-react';

const COBROWSE_TOPIC = 'cobrowse';
const POINTER_THROTTLE_MS = 50; // ~20fps

export function useCoBrowseDataChannel() {
  const room = useRoomContext();
  const lastPointerRef = useRef<number>(0);
  const encoder = useRef(new TextEncoder());

  const sendPointer = useCallback(
    (x: number, y: number) => {
      const now = Date.now();
      if (now - lastPointerRef.current < POINTER_THROTTLE_MS) return;
      lastPointerRef.current = now;

      const payload = encoder.current.encode(
        JSON.stringify({ type: 'pointer', x, y }),
      );
      room.localParticipant
        .publishData(payload, { reliable: false, topic: COBROWSE_TOPIC })
        .catch(() => {
          // Ignore publish failures for lossy pointer data
        });
    },
    [room],
  );

  const sendHighlight = useCallback(
    (rect: { x: number; y: number; w: number; h: number }) => {
      const payload = encoder.current.encode(
        JSON.stringify({ type: 'highlight', ...rect }),
      );
      room.localParticipant
        .publishData(payload, { reliable: true, topic: COBROWSE_TOPIC })
        .catch(() => {
          // Ignore publish failures
        });
    },
    [room],
  );

  const clearHighlight = useCallback(() => {
    const payload = encoder.current.encode(
      JSON.stringify({ type: 'clear_highlight' }),
    );
    room.localParticipant
      .publishData(payload, { reliable: true, topic: COBROWSE_TOPIC })
      .catch(() => {
        // Ignore publish failures
      });
  }, [room]);

  return { sendPointer, sendHighlight, clearHighlight };
}
