'use client';

import { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { HomeIcon, ClockIcon, ArrowRightStartOnRectangleIcon } from '@heroicons/react/24/outline';
import { ThemeToggle } from '@/components/layout/theme-toggle';
import useAuthStore from '@/stores/auth-store';
import { useAgentStatus, useUpdateAgentStatus } from '@/hooks/use-agent-status';

type AgentStatus = 'ONLINE' | 'AWAY' | 'WRAP_UP';

const statusConfig: Record<AgentStatus, { label: string; dotClass: string }> = {
  ONLINE: { label: '온라인', dotClass: 'bg-green-500' },
  AWAY: { label: '자리비움', dotClass: 'bg-amber-500' },
  WRAP_UP: { label: '마무리 중', dotClass: 'bg-purple-500' },
};

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
  const logout = useAuthStore((state) => state.logout);
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const { data: agentStatusData } = useAgentStatus();
  const updateStatus = useUpdateAgentStatus();
  const currentStatus = (agentStatusData?.status as AgentStatus | undefined) ?? 'ONLINE';

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    };
    if (menuOpen) document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [menuOpen]);

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

      {/* Agent info + context menu + logout */}
      <div className="border-t border-gray-200 dark:border-gray-700 px-3 py-3 space-y-2" ref={menuRef}>
        <div className="relative">
          {menuOpen && (
            <div className="absolute bottom-full left-0 right-0 mb-1 rounded-lg border border-gray-200 dark:border-gray-700 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) shadow-lg overflow-hidden z-20">
              <div className="px-3 py-2 space-y-0.5">
                {(['ONLINE', 'AWAY', 'WRAP_UP'] as AgentStatus[]).map((status) => {
                  const config = statusConfig[status];
                  const isActive = currentStatus === status;
                  return (
                    <button
                      key={status}
                      onClick={() => { updateStatus.mutate(status); setMenuOpen(false); }}
                      className={`w-full flex items-center gap-2.5 rounded-md px-2.5 py-1.5 text-sm transition-colors ${
                        isActive
                          ? 'bg-gray-100 dark:bg-gray-700 text-(--color-text-primary) dark:text-(--color-text-primary-dark) font-medium'
                          : 'text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) hover:bg-gray-50 dark:hover:bg-gray-700/50'
                      }`}
                    >
                      <span className={`h-2 w-2 rounded-full shrink-0 ${config.dotClass}`} />
                      {config.label}
                    </button>
                  );
                })}
              </div>
              <div className="border-t border-gray-200 dark:border-gray-700">
                <div className="px-3 py-2">
                  <ThemeToggle />
                </div>
                <button
                  onClick={() => { setMenuOpen(false); router.push('/profile'); }}
                  className="w-full text-left px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark) transition-colors"
                >
                  프로필 설정
                </button>
              </div>
            </div>
          )}
          {user && (
            <div className="flex items-center">
              <button
                onClick={() => setMenuOpen((v) => !v)}
                className="flex-1 flex items-center gap-3 rounded-lg px-3 py-2 hover:bg-(--color-bg-surface) dark:hover:bg-(--color-bg-elevated-dark) transition-colors text-left min-w-0"
              >
                <div className="relative shrink-0">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-100 dark:bg-indigo-900/40 text-indigo-600 dark:text-indigo-400 text-sm font-semibold">
                    {user.name?.charAt(0) ?? '?'}
                  </div>
                  <span className={`absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full border-2 border-white dark:border-gray-800 ${statusConfig[currentStatus].dotClass}`} />
                </div>
                <div className="flex flex-col gap-0 min-w-0">
                  <span className="text-sm font-medium text-(--color-text-primary) dark:text-(--color-text-primary-dark) truncate">
                    {user.name}
                  </span>
                  {user.groupName && (
                    <span className="text-xs text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) truncate">
                      {user.groupName}
                    </span>
                  )}
                </div>
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="ml-auto shrink-0 text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
                  <path d="m18 15-6-6-6 6"/>
                </svg>
              </button>
              <button
                onClick={() => { logout(); router.push('/login'); }}
                title="로그아웃"
                className="shrink-0 p-1.5 rounded-md text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark) hover:text-red-600 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
              >
                <ArrowRightStartOnRectangleIcon className="h-5 w-5" />
              </button>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
};
