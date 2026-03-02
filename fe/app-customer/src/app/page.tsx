'use client';

import { useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { ThemeToggle } from '@/components/layout/theme-toggle';
import useCustomerStore from '@/stores/customer-store';
import { useEnterQueue } from '@/hooks/use-queue';

export default function JoinPage() {
  const router = useRouter();
  const { setCustomerInfo, setEntryId, setQueuePosition } = useCustomerStore();
  const [name, setName] = useState('');
  const [contact, setContact] = useState('');
  const enterQueue = useEnterQueue();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !contact.trim()) return;

    try {
      const result = await enterQueue.mutateAsync({
        name: name.trim(),
        contact: contact.trim(),
      });
      setCustomerInfo(name.trim(), contact.trim());
      setEntryId(result.entryId);
      setQueuePosition(result.position);
      router.push('/waiting');
    } catch {
      // Error handled by mutation state
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-(--color-bg-surface) dark:bg-(--color-bg-base-dark) px-4">
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>

      <div className="w-full max-w-md">
        <div className="rounded-xl bg-(--color-bg-base) shadow-sm p-6 dark:bg-(--color-bg-surface-dark) dark:border dark:border-gray-700">
          <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark) mb-2">
            화상 상담 신청
          </h1>
          <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) mb-6">
            아래 정보를 입력하고 상담을 시작하세요.
          </p>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
              <label
                htmlFor="name"
                className="text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
              >
                이름
              </label>
              <input
                id="name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="이름을 입력하세요"
                required
                disabled={enterQueue.isPending}
                className="rounded-lg border border-gray-300 px-3 py-2 text-(--color-text-primary) placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 dark:bg-(--color-bg-elevated-dark) dark:border-gray-600 dark:text-(--color-text-primary-dark) dark:placeholder-gray-500 disabled:opacity-50"
              />
            </div>

            <div className="flex flex-col gap-1">
              <label
                htmlFor="contact"
                className="text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
              >
                연락처
              </label>
              <input
                id="contact"
                type="tel"
                inputMode="tel"
                value={contact}
                onChange={(e) => setContact(e.target.value)}
                placeholder="연락처를 입력하세요"
                required
                disabled={enterQueue.isPending}
                className="rounded-lg border border-gray-300 px-3 py-2 text-(--color-text-primary) placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 dark:bg-(--color-bg-elevated-dark) dark:border-gray-600 dark:text-(--color-text-primary-dark) dark:placeholder-gray-500 disabled:opacity-50"
              />
            </div>

            {enterQueue.isError && (
              <p className="text-sm text-red-500 dark:text-red-400">
                상담 신청에 실패했습니다. 다시 시도해 주세요.
              </p>
            )}

            <Button
              type="submit"
              variant="primary"
              className="w-full mt-2"
              disabled={enterQueue.isPending}
              aria-label="상담 시작하기"
            >
              {enterQueue.isPending ? '처리 중...' : '상담 시작'}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
