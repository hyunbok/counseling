'use client';

import { useRef, useState, useEffect, ChangeEvent } from 'react';
import {
  FolderOpenIcon,
  XMarkIcon,
  ArrowUpTrayIcon,
  ArrowDownTrayIcon,
  DocumentIcon,
} from '@heroicons/react/24/outline';
import { useFiles, type SharedFile } from '@/hooks/use-files';

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
const ALLOWED_TYPES = [
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'text/plain',
];

interface FilePanelProps {
  channelId: string;
  customerName: string;
}

export function FilePanel({ channelId, customerName }: FilePanelProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [newCount, setNewCount] = useState(0);
  const [validationError, setValidationError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const prevCountRef = useRef(0);
  const { files, uploadFile, isUploading, isLoading, downloadFile } = useFiles(
    channelId,
    customerName,
  );

  // Track new files when panel is closed
  useEffect(() => {
    if (!isOpen && files.length > prevCountRef.current) {
      setNewCount((prev) => prev + (files.length - prevCountRef.current));
    }
    prevCountRef.current = files.length;
  }, [files.length, isOpen]);

  const handleToggle = () => {
    setIsOpen((prev) => !prev);
    if (!isOpen) setNewCount(0);
  };

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setValidationError(null);

    if (file.size > MAX_FILE_SIZE) {
      setValidationError('파일 크기는 10MB 이하여야 합니다.');
      e.target.value = '';
      return;
    }

    if (!ALLOWED_TYPES.includes(file.type)) {
      setValidationError('지원하지 않는 파일 형식입니다.');
      e.target.value = '';
      return;
    }

    uploadFile(file);
    e.target.value = '';
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <>
      {/* Toggle button — positioned to the left of the chat button */}
      <button
        onClick={handleToggle}
        className="fixed bottom-20 left-20 z-50 rounded-full bg-teal-600 p-3 text-white shadow-lg hover:bg-teal-700 transition-colors"
        aria-label={isOpen ? '파일 닫기' : '파일 열기'}
      >
        {isOpen ? (
          <XMarkIcon className="h-6 w-6" />
        ) : (
          <div className="relative">
            <FolderOpenIcon className="h-6 w-6" />
            {newCount > 0 && (
              <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center">
                {newCount > 9 ? '9+' : newCount}
              </span>
            )}
          </div>
        )}
      </button>

      {/* File panel */}
      {isOpen && (
        <div className="fixed bottom-32 left-4 z-40 w-80 h-96 sm:w-96 flex flex-col rounded-xl bg-gray-800 border border-gray-700 shadow-2xl overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-700">
            <h3 className="text-sm font-semibold text-white">파일</h3>
            <div className="flex items-center gap-2">
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={isUploading}
                className="flex items-center gap-1 rounded-lg bg-teal-600 px-2.5 py-1.5 text-xs font-medium text-white hover:bg-teal-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                aria-label="파일 업로드"
              >
                <ArrowUpTrayIcon className="h-3.5 w-3.5" />
                파일 업로드
              </button>
              <button
                onClick={handleToggle}
                className="text-gray-400 hover:text-white"
                aria-label="파일 닫기"
              >
                <XMarkIcon className="h-5 w-5" />
              </button>
            </div>
          </div>

          {/* Validation error */}
          {validationError && (
            <div className="px-4 py-2 bg-red-900/40 border-b border-red-700/50">
              <p className="text-xs text-red-400">{validationError}</p>
            </div>
          )}

          {/* Uploading indicator */}
          {isUploading && (
            <div className="px-4 py-2 bg-teal-900/40 border-b border-teal-700/50">
              <p className="text-xs text-teal-400">업로드 중...</p>
            </div>
          )}

          {/* File list */}
          <div className="flex-1 overflow-y-auto px-4 py-3 space-y-2">
            {isLoading && (
              <p className="text-center text-gray-500 text-xs">파일 로딩 중...</p>
            )}
            {!isLoading && files.length === 0 && (
              <div className="flex h-full flex-col items-center justify-center gap-2 text-gray-500">
                <FolderOpenIcon className="h-10 w-10" />
                <p className="text-sm">공유된 파일이 없습니다</p>
              </div>
            )}
            {files.map((file) => (
              <FileItem
                key={file.id}
                file={file}
                onDownload={downloadFile}
                formatFileSize={formatFileSize}
                formatTime={formatTime}
              />
            ))}
          </div>

          {/* Hidden file input */}
          <input
            ref={fileInputRef}
            type="file"
            className="hidden"
            onChange={handleFileChange}
            accept={ALLOWED_TYPES.join(',')}
            aria-label="파일 선택"
          />
        </div>
      )}
    </>
  );
}

function FileItem({
  file,
  onDownload,
  formatFileSize,
  formatTime,
}: {
  file: SharedFile;
  onDownload: (fileId: string, filename: string) => void;
  formatFileSize: (bytes: number) => string;
  formatTime: (dateStr: string) => string;
}) {
  const isOwn = file.uploaderType === 'CUSTOMER';

  return (
    <div
      className={`flex items-center gap-3 rounded-lg px-3 py-2.5 ${
        isOwn ? 'bg-teal-900/30 border border-teal-700/40' : 'bg-gray-700/60 border border-gray-600/40'
      }`}
    >
      <DocumentIcon className="h-8 w-8 shrink-0 text-gray-400" />
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-white truncate" title={file.originalFilename}>
          {file.originalFilename}
        </p>
        <p className="text-xs text-gray-400 mt-0.5">
          {formatFileSize(file.fileSize)} · {formatTime(file.createdAt)}
          {isOwn && <span className="ml-1 text-teal-400">(나)</span>}
        </p>
      </div>
      <button
        onClick={() => onDownload(file.id, file.originalFilename)}
        className="shrink-0 rounded-lg p-1.5 text-gray-400 hover:text-white hover:bg-gray-600 transition-colors"
        aria-label={`${file.originalFilename} 다운로드`}
      >
        <ArrowDownTrayIcon className="h-4 w-4" />
      </button>
    </div>
  );
}
