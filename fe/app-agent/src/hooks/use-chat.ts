import { useState, useEffect, useCallback, useRef } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import api from '@/lib/api';

export interface ChatMessage {
  id: string;
  channelId: string;
  senderType: 'CUSTOMER' | 'AGENT';
  senderId: string;
  content: string;
  createdAt: string;
}

interface ChatHistoryResponse {
  messages: ChatMessage[];
  hasMore: boolean;
  oldestTimestamp: string | null;
}

export function useChat(channelId: string, agentId: string) {
  const [liveMessages, setLiveMessages] = useState<ChatMessage[]>([]);
  const seenIds = useRef(new Set<string>());
  const abortRef = useRef<AbortController | null>(null);

  // Fetch message history
  const history = useQuery<ChatHistoryResponse>({
    queryKey: ['chat-history', channelId],
    queryFn: async () => {
      const { data } = await api.get<ChatHistoryResponse>(`/api/channels/${channelId}/chat`, {
        params: { limit: 50 },
      });
      data.messages.forEach((m) => seenIds.current.add(m.id));
      return data;
    },
    enabled: !!channelId,
  });

  // Subscribe to SSE stream
  useEffect(() => {
    if (!channelId) return;

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

    (async () => {
      try {
        const response = await fetch(`${baseUrl}/api/channels/${channelId}/chat/stream`, {
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
              try {
                const msg: ChatMessage = JSON.parse(line.slice(5).trim());
                if (!seenIds.current.has(msg.id)) {
                  seenIds.current.add(msg.id);
                  setLiveMessages((prev) => [...prev, msg]);
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

  // Send message — senderType is 'AGENT' for agent app
  const sendMutation = useMutation({
    mutationFn: async (content: string) => {
      const { data } = await api.post<ChatMessage>(`/api/channels/${channelId}/chat`, {
        senderType: 'AGENT',
        senderId: agentId,
        content,
      });
      return data;
    },
  });

  const sendMessage = useCallback(
    (content: string) => {
      if (content.trim()) {
        sendMutation.mutate(content.trim());
      }
    },
    [sendMutation],
  );

  const allMessages = [...(history.data?.messages ?? []), ...liveMessages];

  return {
    messages: allMessages,
    sendMessage,
    isSending: sendMutation.isPending,
    isLoading: history.isLoading,
  };
}
