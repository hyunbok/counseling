import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
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

  // Fetch message history on mount
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
    refetchInterval: 3000,
  });

  // Subscribe to SSE stream
  useEffect(() => {
    if (!channelId) return;

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
    const tenantId = process.env.NEXT_PUBLIC_TENANT_ID ?? 'default';

    let isActive = true;
    const MAX_SEEN_IDS = 5000;

    (async () => {
      try {
        const response = await fetch(`${baseUrl}/api/channels/${channelId}/chat/stream`, {
          headers: { Accept: 'text/event-stream', 'X-Tenant-Id': tenantId },
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
              try {
                const msg: ChatMessage = JSON.parse(line.slice(5).trim());
                if (!seenIds.current.has(msg.id)) {
                  if (seenIds.current.size >= MAX_SEEN_IDS) {
                    const first = seenIds.current.values().next().value;
                    if (first) seenIds.current.delete(first);
                  }
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
      isActive = false;
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
    onSuccess: (msg) => {
      if (!seenIds.current.has(msg.id)) {
        seenIds.current.add(msg.id);
        setLiveMessages((prev) => [...prev, msg]);
      }
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

  // Merge history + live messages with deduplication, sorted by time
  const allMessages = useMemo(() => {
    const map = new Map<string, ChatMessage>();
    for (const m of history.data?.messages ?? []) {
      map.set(m.id, m);
    }
    for (const m of liveMessages) {
      map.set(m.id, m);
    }
    return Array.from(map.values()).sort(
      (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
    );
  }, [history.data?.messages, liveMessages]);

  return {
    messages: allMessages,
    sendMessage,
    isSending: sendMutation.isPending,
    isLoading: history.isLoading,
  };
}
