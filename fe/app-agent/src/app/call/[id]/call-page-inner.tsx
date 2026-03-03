'use client';

import { useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { RoomAudioRenderer } from '@livekit/components-react';
import '@livekit/components-styles';
import { VideoRoom } from '@/components/call/video-room';
import { ChatPanel } from '@/components/call/chat-panel';
import { NotePanel } from '@/components/call/note-panel';
import { FilePanel } from '@/components/call/file-panel';
import { ToolBar } from '@/components/call/tool-bar';
import useCallStore from '@/stores/call-store';
import useAuthStore from '@/stores/auth-store';
import { useRecording } from '@/hooks/use-recording';
import { useCoBrowse } from '@/hooks/use-cobrowse';
import { useReconnection } from '@/hooks/use-reconnection';
import { ReconnectionOverlay } from '@/components/call/reconnection-overlay';
import api from '@/lib/api';

type Tab = 'chat' | 'note' | 'file' | 'doc';

const tabs: { key: Tab; label: string }[] = [
  { key: 'chat', label: '채팅' },
  { key: 'note', label: '메모' },
  { key: 'file', label: '파일' },
  { key: 'doc', label: '문서' },
];

export function CallPageInner({ channelId }: { channelId: string }) {
  const router = useRouter();
  const { customerName, activeTab, setActiveTab } = useCallStore();
  const agentId = useAuthStore((s) => s.user?.id ?? '');
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const { isRecording, startRecording, stopRecording } = useRecording(channelId);
  const { session: coBrowseSession, requestCoBrowse, endCoBrowse } = useCoBrowse(channelId);
  const { status: connectionStatus, retryCount, elapsedMs: reconnectElapsedMs } = useReconnection();
  const isConnected = connectionStatus === 'connected';
  const isRecordingRef = useRef(isRecording);

  useEffect(() => {
    isRecordingRef.current = isRecording;
  }, [isRecording]);

  // Auto-start recording when connected
  useEffect(() => {
    if (isConnected && !isRecordingRef.current) {
      startRecording();
    }
  }, [isConnected, startRecording]);

  // Elapsed timer (paused during reconnection)
  useEffect(() => {
    if (connectionStatus === 'reconnecting') return;
    const interval = setInterval(() => {
      setElapsedSeconds((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [connectionStatus]);

  // Handle permanent disconnect
  useEffect(() => {
    if (connectionStatus === 'disconnected') {
      if (isRecordingRef.current) stopRecording();
      api.post(`/api/channels/${channelId}/close`).catch((err: unknown) => {
        console.error('[CallPage] Failed to close channel on disconnect:', err);
      });
      useCallStore.getState().reset();
      router.push('/dashboard');
    }
  }, [connectionStatus, stopRecording, channelId, router]);

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
    <div className="relative flex h-screen flex-col bg-gray-900 text-white">
      {/* Top bar */}
      <div className="flex h-14 items-center justify-between px-4 bg-gray-800/80 backdrop-blur-sm border-b border-gray-700">
        <div className="flex items-center gap-3">
          <span className="font-semibold text-white">{customerName ?? '고객'}</span>
          {/* Connection status dot */}
          {connectionStatus === 'reconnecting' ? (
            <span className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-amber-500 animate-pulse" />
              <span className="text-xs text-amber-400">재연결 중</span>
            </span>
          ) : (
            <span
              className={`h-2 w-2 rounded-full ${connectionStatus === 'connected' ? 'bg-green-500' : 'bg-red-500'}`}
              aria-label={connectionStatus === 'connected' ? '연결됨' : '연결 끊김'}
            />
          )}
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
        <VideoRoom
          coBrowseSession={coBrowseSession}
          onEndCoBrowse={() => {
            if (coBrowseSession) endCoBrowse(coBrowseSession.sessionId);
          }}
        />

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
            {activeTab === 'file' && <FilePanel channelId={channelId} />}
            {activeTab === 'doc' && (
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
          onCapture={handleCapture}
          onEndCall={handleEndCall}
          channelId={channelId}
          agentId={agentId}
          coBrowseSession={coBrowseSession}
          onRequestCoBrowse={() => requestCoBrowse()}
          onEndCoBrowse={() => {
            if (coBrowseSession) endCoBrowse(coBrowseSession.sessionId);
          }}
        />
      </div>

      <ReconnectionOverlay status={connectionStatus} retryCount={retryCount} elapsedMs={reconnectElapsedMs} />
    </div>
  );
}
