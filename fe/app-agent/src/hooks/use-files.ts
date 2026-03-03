import { useState, useEffect, useRef } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

export interface SharedFileResponse {
  id: string;
  channelId: string;
  uploaderId: string;
  uploaderType: 'AGENT' | 'CUSTOMER';
  originalFilename: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
}

interface SharedFileListResponse {
  files: SharedFileResponse[];
  hasMore: boolean;
  oldestTimestamp?: string;
}

export function useFiles(channelId: string, agentId: string) {
  const queryClient = useQueryClient();
  const [liveFiles, setLiveFiles] = useState<SharedFileResponse[]>([]);
  const seenIds = useRef(new Set<string>());
  const abortRef = useRef<AbortController | null>(null);

  // Fetch file list
  const fileListQuery = useQuery<SharedFileListResponse>({
    queryKey: ['file-list', channelId],
    queryFn: async () => {
      const { data } = await api.get<SharedFileListResponse>(`/api/channels/${channelId}/files`, {
        params: { limit: 50 },
      });
      data.files.forEach((f) => seenIds.current.add(f.id));
      return data;
    },
    enabled: !!channelId,
  });

  // SSE subscription for real-time file events
  useEffect(() => {
    if (!channelId) return;

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

    let isActive = true;
    const MAX_SEEN_IDS = 5000;

    (async () => {
      try {
        const response = await fetch(`${baseUrl}/api/channels/${channelId}/files/stream`, {
          headers: { Accept: 'text/event-stream' },
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
                const file: SharedFileResponse = JSON.parse(line.slice(5).trim());
                if (!seenIds.current.has(file.id)) {
                  if (seenIds.current.size >= MAX_SEEN_IDS) {
                    const first = seenIds.current.values().next().value;
                    if (first) seenIds.current.delete(first);
                  }
                  seenIds.current.add(file.id);
                  setLiveFiles((prev) => [...prev, file]);
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

  // Upload mutation
  const uploadMutation = useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('senderType', 'AGENT');
      formData.append('senderId', agentId);

      const { data } = await api.post<SharedFileResponse>(
        `/api/channels/${channelId}/files`,
        formData,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['file-list', channelId] });
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: async (fileId: string) => {
      await api.delete(`/api/channels/${channelId}/files/${fileId}`);
      return fileId;
    },
    onSuccess: (deletedId) => {
      seenIds.current.delete(deletedId);
      setLiveFiles((prev) => prev.filter((f) => f.id !== deletedId));
      queryClient.invalidateQueries({ queryKey: ['file-list', channelId] });
    },
  });

  const downloadFile = (fileId: string) => {
    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
    window.open(`${baseUrl}/api/channels/${channelId}/files/${fileId}/download`, '_blank');
  };

  // Deduplicate: base list + live additions
  const baseFiles = fileListQuery.data?.files ?? [];
  const baseIds = new Set(baseFiles.map((f) => f.id));
  const dedupedLive = liveFiles.filter((f) => !baseIds.has(f.id));
  const allFiles = [...baseFiles, ...dedupedLive];

  return {
    files: allFiles,
    uploadFile: uploadMutation.mutate,
    deleteFile: deleteMutation.mutate,
    downloadFile,
    isUploading: uploadMutation.isPending,
    uploadError: uploadMutation.error,
    isLoading: fileListQuery.isLoading,
  };
}

export function formatFileSize(bytes: number): string {
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
  if (bytes >= 1024) {
    return `${Math.round(bytes / 1024)} KB`;
  }
  return `${bytes} B`;
}

export function isAllowedFileType(file: File): boolean {
  const allowed = [
    'application/pdf',
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'text/plain',
  ];
  return allowed.includes(file.type);
}
