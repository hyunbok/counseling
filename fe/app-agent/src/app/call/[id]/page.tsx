'use client';

import { use, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { LiveKitRoom, RoomAudioRenderer, useConnectionState } from '@livekit/components-react';
import '@livekit/components-styles';
import { ConnectionState } from 'livekit-client';
import { VideoRoom } from '@/components/call/video-room';
import { ChatPanel } from '@/components/call/chat-panel';
import { NotePanel } from '@/components/call/note-panel';
import { ToolBar } from '@/components/call/tool-bar';
import useCallStore from '@/stores/call-store';
import { useRecording } from '@/hooks/use-recording';
import api from '@/lib/api';

interface CallPageProps {
  params: Promise<{ id: string }>;
}

type Tab = 'chat' | 'note' | 'file' | 'doc';

const tabs: { key: Tab; label: string }[] = [
  { key: 'chat', label: '채팅' },
  { key: 'note', label: '메모' },
  { key: 'file', label: '파일' },
  { key: 'doc', label: '문서' },
];

interface TokenResponse {
  token: string;
  roomName: string;
  identity: string;
  livekitUrl: string;
}

function CallPageInner({ channelId }: { channelId: string }) {
  const router = useRouter();
  const { customerName, activeTab, setActiveTab } = useCallStore();
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const connectionState = useConnectionState();
  const isConnected = connectionState === ConnectionState.Connected;
  const { isRecording, startRecording, stopRecording } = useRecording(channelId);

  // Auto-start recording when connected
  useEffect(() => {
    if (isConnected && !isRecording) {
      startRecording();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isConnected]);

  // Elapsed timer
  useEffect(() => {
    const interval = setInterval(() => {
      setElapsedSeconds((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  const formatElapsed = (seconds: number) => {
    const m = Math.floor(seconds / 60).toString().padStart(2, '0');
    const s = (seconds % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  const handleEndCall = async () => {
    if (isRecording) {
      stopRecording();
    }
    try {
      await api.post(`/api/channels/${channelId}/close`);
    } catch {
      // Proceed even if close fails
    }
    useCallStore.getState().reset();
    router.push('/dashboard');
  };

  const handleCapture = () => {
    // TODO: Implement screen capture
  };

  return (
    <div className="flex h-screen flex-col bg-gray-900 text-white">
      {/* Top bar */}
      <div className="flex h-14 items-center justify-between px-4 bg-gray-800/80 backdrop-blur-sm border-b border-gray-700">
        <div className="flex items-center gap-3">
          <span className="font-semibold text-white">{customerName ?? '고객'}</span>
          {/* Connection status dot */}
          <span
            className={`h-2 w-2 rounded-full ${isConnected ? 'bg-green-500' : 'bg-amber-500'}`}
            aria-label={isConnected ? '연결됨' : '연결 중'}
          />
        </div>
        <div className="flex items-center gap-3">
          {/* Elapsed timer */}
          <span className="font-mono text-sm text-gray-300">{formatElapsed(elapsedSeconds)}</span>
          {/* Recording badge */}
          {isRecording ? (
            <button
              onClick={stopRecording}
              className="inline-flex items-center gap-1 rounded-full bg-red-600/20 border border-red-500/40 px-2.5 py-0.5 text-xs font-medium text-red-400 hover:bg-red-600/30 transition-colors"
              aria-label="녹화 중지"
            >
              <span className="h-1.5 w-1.5 rounded-full bg-red-500 animate-pulse" />
              REC
            </button>
          ) : (
            <button
              onClick={startRecording}
              className="inline-flex items-center gap-1 rounded-full bg-gray-700/50 border border-gray-600 px-2.5 py-0.5 text-xs font-medium text-gray-400 hover:bg-gray-700 transition-colors"
              aria-label="녹화 시작"
            >
              <span className="h-1.5 w-1.5 rounded-full bg-gray-500" />
              REC
            </button>
          )}
        </div>
      </div>

      {/* Main content */}
      <div className="flex flex-1 overflow-hidden">
        {/* Video area */}
        <VideoRoom />

        {/* Side panel */}
        <div className="flex w-80 shrink-0 flex-col bg-gray-800 border-l border-gray-700">
          {/* Tabs */}
          <div className="flex border-b border-gray-700">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`flex-1 py-3 text-sm font-medium transition-colors ${
                  activeTab === tab.key
                    ? 'text-indigo-400 border-b-2 border-indigo-400'
                    : 'text-gray-400 hover:text-gray-200'
                }`}
                aria-selected={activeTab === tab.key}
                role="tab"
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Tab content */}
          <div className="flex-1 overflow-hidden" role="tabpanel">
            {activeTab === 'chat' && <ChatPanel channelId={channelId} />}
            {activeTab === 'note' && <NotePanel channelId={channelId} />}
            {(activeTab === 'file' || activeTab === 'doc') && (
              <div className="flex h-full items-center justify-center">
                <p className="text-sm text-gray-500">준비 중입니다.</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Toolbar (floating pill) */}
      <div className="px-4 py-4 bg-gray-900/80">
        <ToolBar onCapture={handleCapture} onEndCall={handleEndCall} />
      </div>
    </div>
  );
}

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

  const handleDisconnected = () => {
    useCallStore.getState().reset();
    router.push('/dashboard');
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
        onDisconnected={handleDisconnected}
        style={{ height: '100vh' }}
      >
        <RoomAudioRenderer />
        <CallPageInner channelId={channelId} />
      </LiveKitRoom>
    </div>
  );
}
