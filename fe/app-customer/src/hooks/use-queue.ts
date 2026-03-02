import { useMutation } from '@tanstack/react-query';
import { useEffect, useRef, useState, useCallback } from 'react';
import api from '@/lib/api';

interface EnterQueueResponse {
  entryId: string;
  position: number;
  queueSize: number;
}

interface PositionUpdate {
  entryId: string;
  position: number;
  queueSize: number;
  channelId: string | null;
  timestamp: string;
}

export const useEnterQueue = () => {
  return useMutation({
    mutationFn: async (params: { name: string; contact: string }) => {
      const { data } = await api.post<EnterQueueResponse>('/api/queue/enter', params);
      return data;
    },
  });
};

export const useLeaveQueue = () => {
  return useMutation({
    mutationFn: async (entryId: string) => {
      await api.delete(`/api/queue/${entryId}`);
    },
  });
};

export const useQueuePositionStream = (entryId: string | null) => {
  const [position, setPosition] = useState<number | null>(null);
  const [queueSize, setQueueSize] = useState<number | null>(null);
  const [channelId, setChannelId] = useState<string | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const connect = useCallback(async () => {
    if (!entryId) return;

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
    const tenantId = process.env.NEXT_PUBLIC_TENANT_ID ?? 'default';

    try {
      const response = await fetch(`${baseUrl}/api/queue/position/${entryId}/stream`, {
        headers: {
          Accept: 'text/event-stream',
          'X-Tenant-Id': tenantId,
        },
        signal: controller.signal,
      });

      if (!response.ok) {
        throw new Error(`SSE connection failed: ${response.status}`);
      }

      setIsConnected(true);
      setError(null);

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) throw new Error('No readable stream');

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
              const data: PositionUpdate = JSON.parse(line.slice(5).trim());
              setPosition(data.position);
              setQueueSize(data.queueSize);
              if (data.channelId) {
                setChannelId(data.channelId);
              }
            } catch {
              // Skip malformed data
            }
          }
        }
      }
    } catch (err) {
      if (err instanceof Error && err.name === 'AbortError') return;
      setError(err instanceof Error ? err.message : 'Connection failed');
    } finally {
      setIsConnected(false);
    }
  }, [entryId]);

  useEffect(() => {
    connect();
    return () => {
      abortRef.current?.abort();
    };
  }, [connect]);

  return { position, queueSize, channelId, isConnected, error, reconnect: connect };
};
