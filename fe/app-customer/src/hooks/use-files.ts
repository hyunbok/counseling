import { useState, useEffect, useRef } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

export interface SharedFile {
  id: string;
  channelId: string;
  uploaderId: string;
  uploaderType: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
}

interface SharedFileListResponse {
  files: SharedFile[];
  hasMore: boolean;
  oldestTimestamp: string | null;
}

export function useFiles(channelId: string, senderName: string) {
  const queryClient = useQueryClient();
  const [liveFiles, setLiveFiles] = useState<SharedFile[]>([]);
  const seenIds = useRef(new Set<string>());
  const abortRef = useRef<AbortController | null>(null);

  // Fetch file list
  const fileListQuery = useQuery<SharedFileListResponse>({
    queryKey: ['files', channelId],
    queryFn: async () => {
      const { data } = await api.get<SharedFileListResponse>(
        `/api/channels/${channelId}/files`,
        { params: { limit: 50 } },
      );
      data.files.forEach((f) => seenIds.current.add(f.id));
      return data;
    },
    enabled: !!channelId,
  });

  // SSE stream for real-time file events
  useEffect(() => {
    if (!channelId) return;

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
    const tenantId = process.env.NEXT_PUBLIC_TENANT_ID ?? 'default';

    (async () => {
      try {
        const response = await fetch(`${baseUrl}/api/channels/${channelId}/files/stream`, {
          headers: {
            Accept: 'text/event-stream',
            'X-Tenant-Id': tenantId,
          },
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
                const file: SharedFile = JSON.parse(line.slice(5).trim());
                if (!seenIds.current.has(file.id)) {
                  seenIds.current.add(file.id);
                  setLiveFiles((prev) => [file, ...prev]);
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

  // Upload mutation
  const uploadMutation = useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('senderType', 'CUSTOMER');
      formData.append('senderId', senderName);

      const { data } = await api.post<SharedFile>(
        `/api/channels/${channelId}/files`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } },
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['files', channelId] });
    },
  });

  const downloadFile = (fileId: string, filename: string) => {
    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
    const tenantId = process.env.NEXT_PUBLIC_TENANT_ID ?? 'default';
    const url = `${baseUrl}/api/channels/${channelId}/files/${fileId}/download`;

    // Use anchor download to trigger browser download with correct filename
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.setAttribute('data-tenant', tenantId);
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Merge history + live files (live files first since they are newer)
  const historyFiles = fileListQuery.data?.files ?? [];
  const historyIds = new Set(historyFiles.map((f) => f.id));
  const uniqueLiveFiles = liveFiles.filter((f) => !historyIds.has(f.id));
  const allFiles = [...uniqueLiveFiles, ...historyFiles];

  return {
    files: allFiles,
    uploadFile: uploadMutation.mutate,
    isUploading: uploadMutation.isPending,
    isLoading: fileListQuery.isLoading,
    uploadError: uploadMutation.error,
    downloadFile,
  };
}
