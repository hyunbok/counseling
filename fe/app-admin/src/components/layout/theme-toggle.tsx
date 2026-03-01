'use client';

import { useEffect, useRef, useState } from 'react';
import { SunIcon, MoonIcon } from '@heroicons/react/24/outline';

export const ThemeToggle = () => {
  const [isDark, setIsDark] = useState(false);
  const initialized = useRef(false);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    const stored = localStorage.getItem('theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const shouldBeDark = stored === 'dark' || (!stored && prefersDark);
    document.documentElement.classList.toggle('dark', shouldBeDark);
    // Schedule state update outside the effect body to avoid cascading renders
    queueMicrotask(() => setIsDark(shouldBeDark));
  }, []);

  const toggle = () => {
    const next = !isDark;
    setIsDark(next);
    localStorage.setItem('theme', next ? 'dark' : 'light');
    document.documentElement.classList.toggle('dark', next);
  };

  return (
    <button
      onClick={toggle}
      aria-label={isDark ? '라이트 모드로 전환' : '다크 모드로 전환'}
      className="rounded-lg p-2 text-(--color-text-tertiary) hover:bg-(--color-bg-surface) hover:text-(--color-text-secondary) dark:text-(--color-text-tertiary-dark) dark:hover:bg-(--color-bg-elevated-dark) dark:hover:text-(--color-text-secondary-dark) transition-colors"
    >
      {isDark ? <SunIcon className="h-5 w-5" /> : <MoonIcon className="h-5 w-5" />}
    </button>
  );
};
