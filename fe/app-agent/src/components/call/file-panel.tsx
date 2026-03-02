'use client';

import { useRef, useState } from 'react';
import {
  DocumentIcon,
  ArrowDownTrayIcon,
  PaperClipIcon,
  TrashIcon,
  PhotoIcon,
  DocumentTextIcon,
  TableCellsIcon,
  PresentationChartBarIcon,
} from '@heroicons/react/24/outline';
import { useFiles, SharedFileResponse, formatFileSize, isAllowedFileType } from '@/hooks/use-files';
import useAuthStore from '@/stores/auth-store';

interface FilePanelProps {
  channelId: string;
}

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

function getFileIcon(contentType: string) {
  if (contentType.startsWith('image/')) {
    return <PhotoIcon className="h-5 w-5 text-blue-400 shrink-0" aria-hidden="true" />;
  }
  if (contentType === 'application/pdf') {
    return <DocumentTextIcon className="h-5 w-5 text-red-400 shrink-0" aria-hidden="true" />;
  }
  if (
    contentType.includes('spreadsheet') ||
    contentType === 'application/vnd.ms-excel' ||
    contentType === 'text/csv'
  ) {
    return <TableCellsIcon className="h-5 w-5 text-green-400 shrink-0" aria-hidden="true" />;
  }
  if (contentType.includes('presentation') || contentType === 'application/vnd.ms-powerpoint') {
    return <PresentationChartBarIcon className="h-5 w-5 text-orange-400 shrink-0" aria-hidden="true" />;
  }
  return <DocumentIcon className="h-5 w-5 text-gray-400 shrink-0" aria-hidden="true" />;
}

function formatTime(iso: string): string {
  const d = new Date(iso);
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
}

export const FilePanel = ({ channelId }: FilePanelProps) => {
  const user = useAuthStore((state) => state.user);
  const { files, uploadFile, deleteFile, downloadFile, isUploading, isLoading } = useFiles(
    channelId,
    user?.id ?? '',
  );
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [validationError, setValidationError] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setValidationError(null);

    if (!isAllowedFileType(file)) {
      setValidationError('지원하지 않는 파일 형식입니다. (PDF, 이미지, 문서 파일만 허용)');
      e.target.value = '';
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      setValidationError('파일 크기는 10MB 이하여야 합니다.');
      e.target.value = '';
      return;
    }

    uploadFile(file);
    e.target.value = '';
  };

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <p className="text-sm text-gray-500">파일 목록을 불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center gap-2 px-3 py-2 border-b border-gray-700">
        <span className="text-sm font-medium text-gray-200">파일</span>
        {files.length > 0 && (
          <span className="inline-flex items-center justify-center rounded-full bg-gray-700 px-1.5 py-0.5 text-xs text-gray-300">
            {files.length}
          </span>
        )}
      </div>

      {/* File list */}
      <div className="flex-1 overflow-y-auto p-3 space-y-2">
        {files.length === 0 ? (
          <div className="flex h-full items-center justify-center">
            <p className="text-sm text-gray-500">공유된 파일이 없습니다</p>
          </div>
        ) : (
          files.map((file: SharedFileResponse) => (
            <div
              key={file.id}
              className="flex items-center gap-2 rounded-lg bg-gray-700/50 border border-gray-600 px-3 py-2 hover:bg-gray-700 transition-colors group"
            >
              {getFileIcon(file.contentType)}

              {/* File info */}
              <button
                className="flex-1 min-w-0 text-left"
                onClick={() => downloadFile(file.id)}
                aria-label={`${file.originalFilename} 다운로드`}
              >
                <p className="text-sm text-gray-100 truncate">{file.originalFilename}</p>
                <p className="text-xs text-gray-500 mt-0.5">
                  {formatFileSize(file.fileSize)} &middot;{' '}
                  {file.uploaderType === 'AGENT' ? '상담사' : '고객'} &middot;{' '}
                  {formatTime(file.createdAt)}
                </p>
              </button>

              {/* Actions */}
              <div className="flex items-center gap-1 shrink-0">
                <button
                  onClick={() => downloadFile(file.id)}
                  className="p-1 rounded text-gray-400 hover:text-gray-200 hover:bg-gray-600 transition-colors"
                  aria-label="파일 다운로드"
                >
                  <ArrowDownTrayIcon className="h-4 w-4" />
                </button>
                {file.uploaderId === user?.id && (
                  <button
                    onClick={() => deleteFile(file.id)}
                    className="p-1 rounded text-gray-400 hover:text-red-400 hover:bg-gray-600 transition-colors"
                    aria-label="파일 삭제"
                  >
                    <TrashIcon className="h-4 w-4" />
                  </button>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {/* Upload area */}
      <div className="border-t border-gray-700 p-3 space-y-2">
        {validationError && (
          <p className="text-xs text-red-400" role="alert">
            {validationError}
          </p>
        )}
        {isUploading && (
          <p className="text-xs text-gray-400">업로드 중...</p>
        )}
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          onChange={handleFileChange}
          accept=".pdf,.jpg,.jpeg,.png,.gif,.webp,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt"
          aria-label="파일 선택"
        />
        <button
          type="button"
          onClick={handleUploadClick}
          disabled={isUploading}
          className="flex w-full items-center justify-center gap-2 rounded-lg border border-dashed border-gray-600 px-3 py-2 text-sm text-gray-400 hover:border-gray-500 hover:text-gray-300 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          aria-label="파일 업로드"
        >
          <PaperClipIcon className="h-4 w-4" />
          <span>파일 첨부</span>
        </button>
        <p className="text-center text-xs text-gray-600">최대 10MB &middot; PDF, 이미지, 문서</p>
      </div>
    </div>
  );
};
