import { useState, useEffect, useRef } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

export type CoBrowsingStatus = 'REQUESTED' | 'ACTIVE' | 'ENDED';

export interface CoBrowsingSession {
  sessionId: string;
  channelId: string;
  initiatedBy: string;
  status: CoBrowsingStatus;
  startedAt: string | null;
  endedAt: string | null;
  durationSeconds: number | null;
}

export function useCoBrowse(channelId: string) {
  const queryClient = useQueryClient();
  const [liveSession, setLiveSession] = useState<CoBrowsingSession | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  // Fetch initial active session
  const activeQuery = useQuery<CoBrowsingSession | null>({
    queryKey: ['cobrowse-active', channelId],
    queryFn: async () => {
      try {
        const { data } = await api.get<CoBrowsingSession>(
          `/api/channels/${channelId}/co-browsing/active`,
        );
        return data;
      } catch (err: unknown) {
        const axiosErr = err as { response?: { status?: number } };
        if (axiosErr?.response?.status === 404) return null;
        throw err;
      }
    },
    enabled: !!channelId,
  });

  // SSE subscription for live session updates
  useEffect(() => {
    if (!channelId) return;

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
    let isActive = true;

    (async () => {
      try {
        const response = await fetch(
          `${baseUrl}/api/channels/${channelId}/co-browsing/stream`,
          {
            headers: { Accept: 'text/event-stream', 'X-Tenant-Id': 'default' },
            signal: controller.signal,
          },
        );

        if (!response.ok || !response.body) return;

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (isActive) {
          const { done, value } = await reader.read();
          if (done || !isActive) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() ?? '';

          for (const line of lines) {
            if (line.startsWith('data:')) {
              try {
                const session: CoBrowsingSession = JSON.parse(line.slice(5).trim());
                setLiveSession(session);
                queryClient.invalidateQueries({ queryKey: ['cobrowse-active', channelId] });
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
      isActive = false;
      controller.abort();
    };
  }, [channelId, queryClient]);

  // Request co-browsing session
  const requestMutation = useMutation({
    mutationFn: async () => {
      const { data } = await api.post<CoBrowsingSession>(
        `/api/channels/${channelId}/co-browsing`,
      );
      return data;
    },
    onSuccess: (data) => {
      setLiveSession(data);
      queryClient.invalidateQueries({ queryKey: ['cobrowse-active', channelId] });
    },
  });

  // End co-browsing session
  const endMutation = useMutation({
    mutationFn: async (sessionId: string) => {
      const { data } = await api.post<CoBrowsingSession>(
        `/api/channels/${channelId}/co-browsing/${sessionId}/end`,
      );
      return data;
    },
    onSuccess: (data) => {
      setLiveSession(data);
      queryClient.invalidateQueries({ queryKey: ['cobrowse-active', channelId] });
    },
  });

  // Latest SSE wins; fall back to initial query data
  const session = liveSession ?? activeQuery.data ?? null;
  const isActive = session?.status === 'ACTIVE';

  return {
    session,
    requestCoBrowse: requestMutation.mutate,
    endCoBrowse: endMutation.mutate,
    isRequesting: requestMutation.isPending,
    isActive,
  };
}
