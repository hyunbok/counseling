'use client';

import { use, useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  LiveKitRoom,
  VideoConference,
  RoomAudioRenderer,
} from '@livekit/components-react';
import '@livekit/components-styles';
import useCustomerStore from '@/stores/customer-store';
import api from '@/lib/api';
import { ChatPanel } from '@/components/chat/chat-panel';
import { FilePanel } from '@/components/call/file-panel';
import { CoBrowseRequestDialog } from '@/components/call/cobrowse-request-dialog';
import { CoBrowsePointerOverlay } from '@/components/call/cobrowse-pointer-overlay';
import { CoBrowseHighlightOverlay } from '@/components/call/cobrowse-highlight-overlay';
import { useCoBrowseSession } from '@/hooks/use-cobrowse-session';
import { useCoBrowseDataChannel } from '@/hooks/use-cobrowse-data-channel';

interface TokenResponse {
  token: string;
  roomName: string;
  identity: string;
  livekitUrl: string;
}

interface CallPageProps {
  params: Promise<{ id: string }>;
}

function CallInner({ channelId }: { channelId: string }) {
  const mainContentRef = useRef<HTMLDivElement>(null);
  const { pendingRequest, acceptCoBrowse, declineCoBrowse, isSharing } =
    useCoBrowseSession(channelId);
  const { remotePointer, highlightRect } = useCoBrowseDataChannel();
  const { customerName } = useCustomerStore();

  return (
    <div className="relative h-full" ref={mainContentRef}>
      <VideoConference />
      <RoomAudioRenderer />
      <ChatPanel channelId={channelId} customerName={customerName} />
      <FilePanel channelId={channelId} customerName={customerName} />

      {pendingRequest && (
        <CoBrowseRequestDialog onAccept={acceptCoBrowse} onDecline={declineCoBrowse} />
      )}

      {isSharing && remotePointer && (
        <CoBrowsePointerOverlay
          x={remotePointer.x}
          y={remotePointer.y}
          containerRef={mainContentRef}
        />
      )}

      {isSharing && highlightRect && (
        <CoBrowseHighlightOverlay rect={highlightRect} containerRef={mainContentRef} />
      )}
    </div>
  );
}

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

  const handleDisconnected = () => {
    router.push('/feedback');
  };

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
        onDisconnected={handleDisconnected}
        style={{ height: '100vh' }}
      >
        <CallInner channelId={channelId} />
      </LiveKitRoom>
    </div>
  );
}
