'use client';

import Link from 'next/link';
import {
  HomeIcon,
  BuildingOfficeIcon,
  UserGroupIcon,
  UsersIcon,
  ChartBarIcon,
  ChatBubbleLeftRightIcon,
} from '@heroicons/react/24/outline';
import { ThemeToggle } from '@/components/layout/theme-toggle';
import useAuthStore from '@/stores/auth-store';
import type { AdminRole } from '@/types';

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  roles: AdminRole[];
}

const allNavItems: NavItem[] = [
  { label: '대시보드', href: '/dashboard', icon: HomeIcon, roles: ['SUPER_ADMIN', 'COMPANY_ADMIN', 'GROUP_ADMIN'] },
  { label: '테넌트 관리', href: '/tenants', icon: BuildingOfficeIcon, roles: ['SUPER_ADMIN'] },
  { label: '그룹 관리', href: '/groups', icon: UserGroupIcon, roles: ['SUPER_ADMIN', 'COMPANY_ADMIN'] },
  { label: '상담사 관리', href: '/agents', icon: UsersIcon, roles: ['SUPER_ADMIN', 'COMPANY_ADMIN', 'GROUP_ADMIN'] },
  { label: '모니터링', href: '/monitoring', icon: ChartBarIcon, roles: ['SUPER_ADMIN', 'COMPANY_ADMIN', 'GROUP_ADMIN'] },
  {
    label: '피드백',
    href: '/feedbacks',
    icon: ChatBubbleLeftRightIcon,
    roles: ['SUPER_ADMIN', 'COMPANY_ADMIN', 'GROUP_ADMIN'],
  },
];

const roleBadgeLabel: Record<AdminRole, string> = {
  SUPER_ADMIN: '시스템 관리자',
  COMPANY_ADMIN: '회사 관리자',
  GROUP_ADMIN: '그룹 관리자',
};

interface SidebarProps {
  activePath: string;
}

export const Sidebar = ({ activePath }: SidebarProps) => {
  const user = useAuthStore((state) => state.user);

  const navItems = user ? allNavItems.filter((item) => item.roles.includes(user.role)) : [];

  return (
    <aside className="flex w-64 flex-col fixed inset-y-0 left-0 bg-(--color-bg-base) dark:bg-(--color-bg-surface-dark) border-r border-gray-200 dark:border-gray-700 z-10">
      {/* Brand */}
      <div className="flex h-16 items-center px-6 border-b border-gray-200 dark:border-gray-700">
        <span className="text-lg font-semibold text-(--color-primary) dark:text-(--color-primary-dark)">
          관리자 콘솔
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
                      ? 'bg-(--color-primary)/10 dark:bg-(--color-primary-dark)/10 text-(--color-primary) dark:text-(--color-primary-dark)'
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

      {/* User info + theme toggle */}
      <div className="border-t border-gray-200 dark:border-gray-700 px-4 py-4 space-y-3">
        {user && (
          <div className="flex flex-col gap-0.5">
            <span className="text-sm font-medium text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
              {user.name}
            </span>
            <span className="text-xs text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">
              {roleBadgeLabel[user.role]}
            </span>
          </div>
        )}
        <ThemeToggle />
      </div>
    </aside>
  );
};
