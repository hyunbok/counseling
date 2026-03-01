'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { ThemeToggle } from '@/components/layout/theme-toggle';
import { useLogin } from '@/hooks/use-auth';

export default function LoginPage() {
  const router = useRouter();
  const login = useLogin();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) return;
    login.mutate(
      { username: username.trim(), password: password.trim() },
      {
        onSuccess: () => {
          router.push('/dashboard');
        },
      },
    );
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-(--color-bg-surface) dark:bg-(--color-bg-base-dark) px-4">
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>

      <div className="w-full max-w-md">
        <div className="rounded-xl bg-(--color-bg-base) shadow-sm p-6 dark:bg-(--color-bg-surface-dark) dark:border dark:border-gray-700">
          <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark) mb-2">
            상담사 로그인
          </h1>
          <p className="text-sm text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) mb-6">
            계정 정보를 입력하여 로그인하세요.
          </p>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
              <label
                htmlFor="username"
                className="text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
              >
                아이디
              </label>
              <input
                id="username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="아이디를 입력하세요"
                required
                autoComplete="username"
                className="rounded-lg border border-gray-300 px-3 py-2 text-(--color-text-primary) placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 dark:bg-(--color-bg-elevated-dark) dark:border-gray-600 dark:text-(--color-text-primary-dark) dark:placeholder-gray-500"
              />
            </div>

            <div className="flex flex-col gap-1">
              <label
                htmlFor="password"
                className="text-sm font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)"
              >
                비밀번호
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="비밀번호를 입력하세요"
                required
                autoComplete="current-password"
                className="rounded-lg border border-gray-300 px-3 py-2 text-(--color-text-primary) placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 dark:bg-(--color-bg-elevated-dark) dark:border-gray-600 dark:text-(--color-text-primary-dark) dark:placeholder-gray-500"
              />
            </div>

            {login.isError && (
              <p className="text-sm text-red-500 dark:text-red-400">
                로그인에 실패했습니다. 아이디와 비밀번호를 확인해 주세요.
              </p>
            )}

            <Button
              type="submit"
              variant="primary"
              className="w-full mt-2"
              disabled={login.isPending}
              aria-label="로그인"
            >
              {login.isPending ? '로그인 중...' : '로그인'}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
