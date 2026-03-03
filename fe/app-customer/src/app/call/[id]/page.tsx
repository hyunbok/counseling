'use client';

import { use, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import dynamic from 'next/dynamic';
import {
  LiveKitRoom,
} from '@livekit/components-react';
import '@livekit/components-styles';
import { DefaultReconnectPolicy } from 'livekit-client';
import useCustomerStore from '@/stores/customer-store';
import api from '@/lib/api';

const roomOptions = {
  reconnectPolicy: new DefaultReconnectPolicy([300, 600, 1200, 2400, 4800, 8000, 10000, 10000, 10000, 10000]),
  disconnectOnPageLeave: false,
};

interface TokenResponse {
  token: string;
  roomName: string;
  identity: string;
  livekitUrl: string;
}

interface CallPageProps {
  params: Promise<{ id: string }>;
}

const CallInner = dynamic(
  () => import('./call-inner').then((m) => m.CallInner),
  {
    ssr: false,
    loading: () => (
      <div className="flex h-screen items-center justify-center bg-gray-900 text-white">
        <div className="text-center">
          <div className="h-12 w-12 mx-auto mb-4 rounded-full border-4 border-gray-600 border-t-indigo-400 animate-spin" />
          <p className="text-gray-300">통화 연결 준비 중...</p>
        </div>
      </div>
    ),
  },
);

export default function CallPage({ params }: CallPageProps) {
  const { id: channelId } = use(params);
  const router = useRouter();
  const { customerName } = useCustomerStore();

  const [tokenData, setTokenData] = useState<TokenResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!customerName) {
      router.replace('/');
      return;
    }

    const fetchToken = async () => {
      try {
        const { data } = await api.get<TokenResponse>(
          `/api/channels/${channelId}/customer-token`,
          { params: { name: customerName } },
        );
        setTokenData(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : '토큰을 가져올 수 없습니다.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchToken();
  }, [channelId, customerName, router]);

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-900 text-white">
        <div className="text-center">
          <div className="h-12 w-12 mx-auto mb-4 rounded-full border-4 border-gray-600 border-t-indigo-400 animate-spin" />
          <p className="text-gray-300">통화 연결 준비 중...</p>
        </div>
      </div>
    );
  }

  if (error || !tokenData) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-900 text-white">
        <div className="text-center max-w-sm">
          <p className="text-red-400 mb-4">{error ?? '연결할 수 없습니다.'}</p>
          <button
            onClick={() => router.push('/')}
            className="text-indigo-400 underline text-sm"
          >
            처음으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen bg-gray-900" data-lk-theme="default">
      <LiveKitRoom
        serverUrl={tokenData.livekitUrl}
        token={tokenData.token}
        connect={true}
        video={true}
        audio={true}
        options={roomOptions}
        style={{ height: '100vh' }}
      >
        <CallInner channelId={channelId} />
      </LiveKitRoom>
    </div>
  );
}
