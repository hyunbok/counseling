import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import useAuthStore from '@/stores/auth-store';

export function useQueueStream() {
  const queryClient = useQueryClient();
  const accessToken = useAuthStore((s) => s.accessToken);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
    const tenantId = process.env.NEXT_PUBLIC_TENANT_ID ?? 'default';

    let isActive = true;

    (async () => {
      try {
        const headers: Record<string, string> = {
          Accept: 'text/event-stream',
          'X-Tenant-Id': tenantId,
        };
        if (accessToken) {
          headers['Authorization'] = `Bearer ${accessToken}`;
        }
        const response = await fetch(`${baseUrl}/api/queue/stream`, {
          headers,
          signal: controller.signal,
        });
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
              queryClient.invalidateQueries({ queryKey: ['queue'] });
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
  }, [queryClient, accessToken]);
}
