'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useRoomContext } from '@livekit/components-react';
import { Track } from 'livekit-client';
import api from '@/lib/api';

export interface CoBrowseSession {
  sessionId: string;
  channelId: string;
  initiatedBy: string;
  status: 'REQUESTED' | 'ACTIVE' | 'ENDED';
  startedAt: string | null;
  endedAt: string | null;
  durationSeconds: number | null;
}

export function useCoBrowseSession(channelId: string) {
  const room = useRoomContext();
  const [pendingRequest, setPendingRequest] = useState<CoBrowseSession | null>(null);
  const [isSharing, setIsSharing] = useState(false);
  const abortRef = useRef<AbortController | null>(null);
  const localTrackRef = useRef<MediaStreamTrack | null>(null);
  const sessionIdRef = useRef<string | null>(null);

  // Initial state query
  useQuery<CoBrowseSession | null>({
    queryKey: ['co-browsing-active', channelId],
    queryFn: async () => {
      try {
        const { data } = await api.get<CoBrowseSession>(
          `/api/channels/${channelId}/co-browsing/active`,
        );
        if (data.status === 'REQUESTED') {
          setPendingRequest(data);
          sessionIdRef.current = data.sessionId;
        } else if (data.status === 'ACTIVE') {
          setIsSharing(true);
          sessionIdRef.current = data.sessionId;
        }
        return data;
      } catch {
        return null;
      }
    },
    enabled: !!channelId,
  });

  // SSE stream for session updates
  useEffect(() => {
    if (!channelId) return;

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
    const tenantId = process.env.NEXT_PUBLIC_TENANT_ID ?? 'default';

    (async () => {
      try {
        const response = await fetch(
          `${baseUrl}/api/channels/${channelId}/co-browsing/stream`,
          {
            headers: {
              Accept: 'text/event-stream',
              'X-Tenant-Id': tenantId,
            },
            signal: controller.signal,
          },
        );

        if (!response.ok || !response.body) return;

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() ?? '';

          for (const line of lines) {
            if (line.startsWith('data:')) {
              try {
                const session: CoBrowseSession = JSON.parse(line.slice(5).trim());
                if (session.status === 'REQUESTED') {
                  setPendingRequest(session);
                  sessionIdRef.current = session.sessionId;
                } else if (session.status === 'ACTIVE') {
                  setIsSharing(true);
                  setPendingRequest(null);
                  sessionIdRef.current = session.sessionId;
                } else if (session.status === 'ENDED') {
                  setPendingRequest(null);
                  setIsSharing(false);
                  sessionIdRef.current = null;
                  stopLocalTrack();
                }
              } catch {
                // Skip malformed
              }
            }
          }
        }
      } catch {
        // Silently handle abort
      }
    })();

    return () => {
      controller.abort();
    };
  }, [channelId]);

  const stopLocalTrack = useCallback(() => {
    if (localTrackRef.current) {
      localTrackRef.current.stop();
      localTrackRef.current = null;
    }
  }, []);

  const endCoBrowse = useCallback(async () => {
    const sessionId = sessionIdRef.current;
    if (!sessionId) return;

    try {
      await api.post(`/api/channels/${channelId}/co-browsing/${sessionId}/end`);
    } catch {
      // Ignore errors
    }
    stopLocalTrack();
    setIsSharing(false);
    sessionIdRef.current = null;
  }, [channelId, stopLocalTrack]);

  const acceptCoBrowse = useCallback(async () => {
    const sessionId = sessionIdRef.current;
    if (!sessionId) return;

    try {
      const stream = await navigator.mediaDevices.getDisplayMedia({
        video: { displaySurface: 'browser' },
        audio: false,
      });

      const videoTrack = stream.getVideoTracks()[0];
      if (!videoTrack) {
        stream.getTracks().forEach((t) => t.stop());
        return;
      }

      localTrackRef.current = videoTrack;

      await room.localParticipant.publishTrack(videoTrack, {
        name: 'cobrowse',
        source: Track.Source.ScreenShare,
      });

      await api.post(`/api/channels/${channelId}/co-browsing/${sessionId}/start`);

      setIsSharing(true);
      setPendingRequest(null);

      videoTrack.onended = () => {
        endCoBrowse();
      };
    } catch {
      // User cancelled or error
      stopLocalTrack();
    }
  }, [channelId, room, endCoBrowse, stopLocalTrack]);

  const declineCoBrowse = useCallback(async () => {
    const sessionId = sessionIdRef.current;
    if (!sessionId) return;

    try {
      await api.post(`/api/channels/${channelId}/co-browsing/${sessionId}/end`);
    } catch {
      // Ignore errors
    }
    setPendingRequest(null);
    sessionIdRef.current = null;
  }, [channelId]);

  return {
    pendingRequest,
    acceptCoBrowse,
    declineCoBrowse,
    isSharing,
    endCoBrowse,
  };
}
