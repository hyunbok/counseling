'use client';

import Link from 'next/link';
import { HomeIcon, ClockIcon } from '@heroicons/react/24/outline';
import { ThemeToggle } from '@/components/layout/theme-toggle';
import useAuthStore from '@/stores/auth-store';

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
}

const navItems: NavItem[] = [
  { label: '대시보드', href: '/dashboard', icon: HomeIcon },
  { label: '상담 이력', href: '/history', icon: ClockIcon },
];

interface SidebarProps {
  activePath: string;
}

export const Sidebar = ({ activePath }: SidebarProps) => {
  const user = useAuthStore((state) => state.user);

  return (
    <aside className="flex w-64 flex-col fixed inset-y-0 left-0 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) border-r border-gray-200 dark:border-gray-700 z-10">
      {/* Brand */}
      <div className="flex h-16 items-center px-6 border-b border-gray-200 dark:border-gray-700">
        <span className="text-lg font-semibold text-indigo-600 dark:text-indigo-400">
          상담사 플랫폼
        </span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto px-3 py-4">
        <ul className="space-y-1">
          {navItems.map((item) => {
            const isActive = activePath === item.href;
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className={`flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-indigo-50 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300'
                      : 'text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark) hover:text-(--color-text-primary) dark:hover:text-(--color-text-primary-dark)'
                  }`}
                  aria-current={isActive ? 'page' : undefined}
                >
                  <item.icon className="h-5 w-5 shrink-0" />
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>

      {/* Agent info + theme toggle */}
      <div className="border-t border-gray-200 dark:border-gray-700 px-4 py-4 space-y-3">
        {user && (
          <div className="flex flex-col gap-0.5">
            <span className="text-sm font-medium text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              {user.name}
            </span>
            {user.groupName && (
              <span className="text-xs text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                {user.groupName}
              </span>
            )}
          </div>
        )}
        <ThemeToggle />
      </div>
    </aside>
  );
};
