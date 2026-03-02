'use client';

import { use, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useVideoCall } from '@/hooks/use-video-call';
import { VideoRoom } from '@/components/call/video-room';
import { ChatPanel } from '@/components/call/chat-panel';
import { NotePanel } from '@/components/call/note-panel';
import { ToolBar } from '@/components/call/tool-bar';
import useCallStore from '@/stores/call-store';

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

export default function CallPage({ params }: CallPageProps) {
  const { id } = use(params);
  const router = useRouter();
  const { customerName, activeTab, setActiveTab, reset } = useCallStore();
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  // TODO: Fetch real token from API using channelId
  const token = '';
  const hasToken = token !== '';

  const { isConnected, isCameraOn, isMicOn, isScreenSharing, connect, disconnect, toggleCamera, toggleMic, toggleScreenShare } =
    useVideoCall({ roomName: id, token });

  // Connect on mount once token is available
  useEffect(() => {
    if (!hasToken) return;
    connect();
    return () => {
      disconnect();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasToken]);

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
    await disconnect();
    reset();
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
          <span className="inline-flex items-center gap-1 rounded-full bg-red-600/20 border border-red-500/40 px-2.5 py-0.5 text-xs font-medium text-red-400">
            <span className="h-1.5 w-1.5 rounded-full bg-red-500 animate-pulse" />
            REC
          </span>
        </div>
      </div>

      {/* Main content */}
      <div className="flex flex-1 overflow-hidden">
        {/* Video area */}
        <VideoRoom isConnected={isConnected} isCameraOn={isCameraOn} />

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
            {activeTab === 'chat' && <ChatPanel channelId={id} />}
            {activeTab === 'note' && <NotePanel />}
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
        <ToolBar
          isMicOn={isMicOn}
          isCameraOn={isCameraOn}
          isScreenSharing={isScreenSharing}
          onToggleMic={toggleMic}
          onToggleCamera={toggleCamera}
          onToggleScreenShare={toggleScreenShare}
          onCapture={handleCapture}
          onEndCall={handleEndCall}
        />
      </div>
    </div>
  );
}
