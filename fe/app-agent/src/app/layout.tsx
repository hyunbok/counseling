import type { Metadata } from 'next';
import Providers from '@/components/providers';
import './globals.css';

export const metadata: Metadata = {
  title: '상담사 대시보드',
  description: '화상 상담 플랫폼 - 상담사',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <body className="antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
