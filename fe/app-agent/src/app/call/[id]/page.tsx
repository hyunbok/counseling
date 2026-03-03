'use client';

import { use, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import dynamic from 'next/dynamic';
import { LiveKitRoom, RoomAudioRenderer } from '@livekit/components-react';
import '@livekit/components-styles';
import { DefaultReconnectPolicy } from 'livekit-client';
import useCallStore from '@/stores/call-store';
import api from '@/lib/api';

interface CallPageProps {
  params: Promise<{ id: string }>;
}

interface TokenResponse {
  token: string;
  roomName: string;
  identity: string;
  livekitUrl: string;
}

const roomOptions = {
  reconnectPolicy: new DefaultReconnectPolicy([300, 600, 1200, 2400, 4800, 8000, 10000, 10000, 10000, 10000]),
  disconnectOnPageLeave: false,
};

const CallPageInner = dynamic(
  () => import('./call-page-inner').then((m) => m.CallPageInner),
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
  const { agentToken, livekitUrl } = useCallStore();

  const [tokenData, setTokenData] = useState<TokenResponse | null>(
    agentToken && livekitUrl ? { token: agentToken, livekitUrl, roomName: channelId, identity: '' } : null,
  );
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(!agentToken || !livekitUrl);

  useEffect(() => {
    if (agentToken && livekitUrl) {
      setIsLoading(false);
      return;
    }

    const fetchToken = async () => {
      try {
        const { data } = await api.get<TokenResponse>(`/api/channels/${channelId}/token`);
        setTokenData(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : '토큰을 가져올 수 없습니다.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchToken();
  }, [channelId, agentToken, livekitUrl]);

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
            onClick={() => router.push('/dashboard')}
            className="text-indigo-400 underline text-sm"
          >
            대시보드로 돌아가기
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
        <RoomAudioRenderer />
        <CallPageInner channelId={channelId} />
      </LiveKitRoom>
    </div>
  );
}
