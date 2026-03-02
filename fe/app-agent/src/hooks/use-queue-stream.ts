import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';

export function useQueueStream() {
  const queryClient = useQueryClient();
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

    (async () => {
      try {
        const response = await fetch(`${baseUrl}/api/queue/stream`, {
          headers: { Accept: 'text/event-stream' },
          signal: controller.signal,
        });
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
              queryClient.invalidateQueries({ queryKey: ['queue'] });
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
  }, [queryClient]);
}
