'use client';

import { useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import '@livekit/components-styles';
import useCustomerStore from '@/stores/customer-store';
import { VideoRoom } from '@/components/call/video-room';
import { ToolBar } from '@/components/call/tool-bar';
import { ChatPanel } from '@/components/chat/chat-panel';
import { FilePanel } from '@/components/call/file-panel';
import { CoBrowseRequestDialog } from '@/components/call/cobrowse-request-dialog';
import { CoBrowsePointerOverlay } from '@/components/call/cobrowse-pointer-overlay';
import { CoBrowseHighlightOverlay } from '@/components/call/cobrowse-highlight-overlay';
import { useCoBrowseSession } from '@/hooks/use-cobrowse-session';
import { useCoBrowseDataChannel } from '@/hooks/use-cobrowse-data-channel';
import { useReconnection } from '@/hooks/use-reconnection';
import { ReconnectionOverlay } from '@/components/call/reconnection-overlay';

export function CallInner({ channelId }: { channelId: string }) {
  const router = useRouter();
  const mainContentRef = useRef<HTMLDivElement>(null);
  const isLeavingRef = useRef(false);
  const { pendingRequest, acceptCoBrowse, declineCoBrowse, isSharing } =
    useCoBrowseSession(channelId);
  const { remotePointer, highlightRect } = useCoBrowseDataChannel();
  const { customerName } = useCustomerStore();
  const { status: connectionStatus, retryCount, elapsedMs } = useReconnection();

  useEffect(() => {
    if (connectionStatus === 'disconnected' && !isLeavingRef.current) {
      isLeavingRef.current = true;
      router.push('/feedback');
    }
  }, [connectionStatus, router]);

  const handleLeave = () => {
    isLeavingRef.current = true;
    router.push('/feedback');
  };

  return (
    <div className="relative flex h-full flex-col bg-gray-900" ref={mainContentRef}>
      {/* Video area */}
      <div className="flex flex-1 overflow-hidden">
        <VideoRoom />
      </div>

      {/* Bottom toolbar */}
      <ToolBar onLeave={handleLeave} />

      {/* Floating panels */}
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

      <ReconnectionOverlay status={connectionStatus} retryCount={retryCount} elapsedMs={elapsedMs} />
    </div>
  );
}
