import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';

export interface ChatMessageItem {
  id: string;
  channelId: string;
  senderType: 'AGENT' | 'CUSTOMER' | 'SYSTEM';
  senderId: string;
  content: string;
  createdAt: string;
}

interface ChatMessageListResponse {
  messages: ChatMessageItem[];
  hasMore: boolean;
  oldestTimestamp: string | null;
}

export function useChatHistory(channelId: string | null) {
  return useQuery<ChatMessageItem[]>({
    queryKey: ['chat-history', channelId],
    queryFn: async () => {
      const { data } = await api.get<ChatMessageListResponse>(
        `/api/channels/${channelId}/chat`,
        { params: { limit: 100 } },
      );
      return data.messages;
    },
    enabled: !!channelId,
  });
}
