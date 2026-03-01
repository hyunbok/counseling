'use client';

import { usePathname } from 'next/navigation';
import { Sidebar } from '@/components/layout/sidebar';

interface SidebarLayoutProps {
  children: React.ReactNode;
}

export const SidebarLayout = ({ children }: SidebarLayoutProps) => {
  const pathname = usePathname();

  return (
    <div className="flex min-h-screen bg-(--color-bg-surface) dark:bg-(--color-bg-base-dark)">
      <Sidebar activePath={pathname} />
      <main className="flex-1 overflow-y-auto ml-64">{children}</main>
    </div>
  );
};
