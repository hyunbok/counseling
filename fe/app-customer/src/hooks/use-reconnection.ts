'use client';

import { useEffect, useRef, useState } from 'react';
import { useRoomContext } from '@livekit/components-react';
import { RoomEvent } from 'livekit-client';

export type ConnectionStatus = 'connected' | 'reconnecting' | 'disconnected';

export function useReconnection() {
  const room = useRoomContext();
  const [status, setStatus] = useState<ConnectionStatus>('connected');
  const [retryCount, setRetryCount] = useState(0);
  const [elapsedMs, setElapsedMs] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    const startTimer = () => {
      if (timerRef.current) return;
      timerRef.current = setInterval(() => {
        setElapsedMs((prev) => prev + 100);
      }, 100);
    };

    const stopTimer = () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    };

    const handleReconnecting = () => {
      setStatus('reconnecting');
      setRetryCount((prev) => prev + 1);
      startTimer();
    };

    const handleReconnected = () => {
      setStatus('connected');
      setRetryCount(0);
      setElapsedMs(0);
      stopTimer();
    };

    const handleDisconnected = () => {
      setStatus('disconnected');
      stopTimer();
    };

    room.on(RoomEvent.Reconnecting, handleReconnecting);
    room.on(RoomEvent.SignalReconnecting, handleReconnecting);
    room.on(RoomEvent.Reconnected, handleReconnected);
    room.on(RoomEvent.Disconnected, handleDisconnected);

    return () => {
      room.off(RoomEvent.Reconnecting, handleReconnecting);
      room.off(RoomEvent.SignalReconnecting, handleReconnecting);
      room.off(RoomEvent.Reconnected, handleReconnected);
      room.off(RoomEvent.Disconnected, handleDisconnected);
      stopTimer();
    };
  }, [room]);

  return { status, retryCount, elapsedMs };
}
