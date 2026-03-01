'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import axios from 'axios';
import { useLogin } from '@/hooks/use-auth';
import { Button } from '@/components/ui/button';

const inputClass =
  'w-full rounded-[--radius-input] border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-(--color-primary)';

function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error) && error.response?.status === 401) {
    return '아이디 또는 비밀번호가 올바르지 않습니다.';
  }
  return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
}

export default function LoginPage() {
  const router = useRouter();
  const { mutate: login, isPending, error } = useLogin();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    login(
      { username, password },
      {
        onSuccess: () => {
          router.push('/dashboard');
        },
      },
    );
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-(--color-bg-surface) dark:bg-(--color-bg-base-dark)">
      <div className="w-full max-w-sm bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) rounded-[--radius-card] shadow-md p-8 space-y-6">
        <div className="text-center space-y-1">
          <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
            관리자 콘솔
          </h1>
          <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
            관리자 계정으로 로그인하세요
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1">
            <label
              htmlFor="username"
              className="block text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
            >
              아이디
            </label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoComplete="username"
              className={inputClass}
              placeholder="관리자 아이디"
            />
          </div>

          <div className="space-y-1">
            <label
              htmlFor="password"
              className="block text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
            >
              비밀번호
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              className={inputClass}
              placeholder="비밀번호"
            />
          </div>

          {error && (
            <p role="alert" className="text-sm text-(--color-error)">
              {getErrorMessage(error)}
            </p>
          )}

          <Button type="submit" className="w-full" disabled={isPending}>
            {isPending ? '로그인 중...' : '로그인'}
          </Button>
        </form>
      </div>
    </div>
  );
}
