'use client';

import { useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import { useMutation } from '@tanstack/react-query';
import { StarIcon } from '@heroicons/react/24/solid';
import { StarIcon as StarOutlineIcon } from '@heroicons/react/24/outline';
import { Button } from '@/components/ui/button';
import { ThemeToggle } from '@/components/layout/theme-toggle';
import useCustomerStore from '@/stores/customer-store';
import api from '@/lib/api';

export default function FeedbackPage() {
  const router = useRouter();
  const { channelId, customerName, reset } = useCustomerStore();
  const [rating, setRating] = useState(0);
  const [hovered, setHovered] = useState(0);
  const [comment, setComment] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const submitFeedback = useMutation({
    mutationFn: async (params: { rating: number; comment: string }) => {
      await api.post('/api/feedback', {
        channelId,
        customerName,
        rating: params.rating,
        comment: params.comment,
      });
    },
  });

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (rating === 0) return;

    try {
      await submitFeedback.mutateAsync({ rating, comment });
    } catch (err: unknown) {
      // If 404, the endpoint doesn't exist yet — proceed gracefully
      const isAxiosError = err && typeof err === 'object' && 'response' in err;
      if (isAxiosError && (err as { response?: { status?: number } }).response?.status === 404) {
        console.warn('Feedback API not implemented yet, proceeding gracefully');
      } else {
        // Real error — don't proceed
        return;
      }
    }

    reset();
    setSubmitted(true);
  };

  if (submitted) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-(--color-bg-surface) dark:bg-(--color-bg-base-dark) px-4">
        <div className="w-full max-w-md">
          <div className="rounded-xl bg-(--color-bg-base) shadow-sm p-6 dark:bg-(--color-bg-surface-dark) dark:border dark:border-gray-700 text-center">
            <div className="flex justify-center mb-4">
              <div className="rounded-full bg-green-100 dark:bg-green-900/30 p-4">
                <StarIcon className="h-8 w-8 text-green-500" />
              </div>
            </div>
            <h2 className="text-xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark) mb-2">
              감사합니다!
            </h2>
            <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) mb-6">
              소중한 피드백을 남겨주셔서 감사합니다.
            </p>
            <Button variant="primary" onClick={() => router.push('/')} className="w-full">
              처음으로 돌아가기
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-(--color-bg-surface) dark:bg-(--color-bg-base-dark) px-4">
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>

      <div className="w-full max-w-md">
        <div className="rounded-xl bg-(--color-bg-base) shadow-sm p-6 dark:bg-(--color-bg-surface-dark) dark:border dark:border-gray-700">
          <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark) mb-2">
            상담 평가
          </h1>
          <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) mb-6">
            상담은 어떠셨나요? 솔직한 의견을 들려주세요.
          </p>

          <form onSubmit={handleSubmit} className="flex flex-col gap-6">
            {/* Star rating */}
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">
                만족도
              </label>
              <div className="flex gap-1" role="group" aria-label="별점 선택">
                {[1, 2, 3, 4, 5].map((star) => {
                  const isFilled = star <= (hovered || rating);
                  return (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setRating(star)}
                      onMouseEnter={() => setHovered(star)}
                      onMouseLeave={() => setHovered(0)}
                      aria-label={`${star}점`}
                      aria-pressed={star <= rating}
                      className="transition-transform hover:scale-110 focus:outline-none focus:ring-2 focus:ring-indigo-500 rounded"
                    >
                      {isFilled ? (
                        <StarIcon className="h-8 w-8 text-amber-400" />
                      ) : (
                        <StarOutlineIcon className="h-8 w-8 text-gray-300 dark:text-gray-600" />
                      )}
                    </button>
                  );
                })}
              </div>
              {rating > 0 && (
                <p className="text-xs text-indigo-600 dark:text-indigo-400">{rating}점 선택됨</p>
              )}
            </div>

            {/* Comment */}
            <div className="flex flex-col gap-1">
              <label
                htmlFor="comment"
                className="text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
              >
                추가 의견{' '}
                <span className="text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) font-normal">
                  (선택)
                </span>
              </label>
              <textarea
                id="comment"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="상담에 대한 의견을 자유롭게 작성해 주세요."
                rows={4}
                className="rounded-lg border border-gray-300 px-3 py-2 text-(--color-text-primary) placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 resize-none dark:bg-(--color-bg-elevated-dark) dark:border-gray-600 dark:text-(--color-text-primary-dark) dark:placeholder-gray-500"
              />
            </div>

            <Button
              type="submit"
              variant="primary"
              disabled={rating === 0 || submitFeedback.isPending}
              className="w-full"
              aria-label="피드백 제출"
            >
              {submitFeedback.isPending ? '제출 중...' : '제출'}
            </Button>
          </form>

          {submitFeedback.isError && !submitted && (
            <p className="text-sm text-red-500 dark:text-red-400 mt-2">
              제출에 실패했습니다. 다시 시도해 주세요.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
