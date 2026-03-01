'use client';

import { useEffect, useRef } from 'react';
import { XMarkIcon } from '@heroicons/react/24/outline';
import { Button } from '@/components/ui/button';

interface CreateModalProps {
  title: string;
  open: boolean;
  onClose: () => void;
  onSubmit: () => void;
  isPending?: boolean;
  submitLabel?: string;
  children: React.ReactNode;
}

export const CreateModal = ({
  title,
  open,
  onClose,
  onSubmit,
  isPending = false,
  submitLabel = '저장',
  children,
}: CreateModalProps) => {
  const dialogRef = useRef<HTMLDivElement>(null);

  // Escape key to close
  useEffect(() => {
    if (!open) return;
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [open, onClose]);

  // Focus first focusable element when modal opens
  useEffect(() => {
    if (!open || !dialogRef.current) return;
    const focusable = dialogRef.current.querySelector<HTMLElement>(
      'input, select, textarea, button:not([aria-label="닫기"])',
    );
    focusable?.focus();
  }, [open]);

  if (!open) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
      aria-describedby="modal-description"
      className="fixed inset-0 z-50 flex items-center justify-center"
    >
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/50" onClick={onClose} aria-hidden="true" />

      {/* Panel */}
      <div
        ref={dialogRef}
        className="relative w-full max-w-md rounded-[--radius-card] bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-xl mx-4"
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-200 dark:border-gray-700 px-6 py-4">
          <h2
            id="modal-title"
            className="text-base font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)"
          >
            {title}
          </h2>
          <button
            onClick={onClose}
            aria-label="닫기"
            className="rounded-lg p-1 text-(--color-text-tertiary) hover:bg-(--color-bg-surface) hover:text-(--color-text-secondary) dark:hover:bg-(--color-bg-elevated-dark) dark:hover:text-(--color-text-secondary-dark) transition-colors"
          >
            <XMarkIcon className="h-5 w-5" />
          </button>
        </div>

        {/* Body */}
        <div className="px-6 py-5 space-y-4">
          <p id="modal-description" className="sr-only">
            {title} 양식입니다. 필드를 채운 후 저장 버튼을 누르세요.
          </p>
          {children}
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-2 border-t border-gray-200 dark:border-gray-700 px-6 py-4">
          <Button variant="secondary" onClick={onClose} disabled={isPending}>
            취소
          </Button>
          <Button onClick={onSubmit} disabled={isPending}>
            {isPending ? '저장 중...' : submitLabel}
          </Button>
        </div>
      </div>
    </div>
  );
};
