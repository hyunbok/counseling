'use client';

import {
  MicrophoneIcon,
  VideoCameraIcon,
  VideoCameraSlashIcon,
  ComputerDesktopIcon,
  CameraIcon,
  PhoneXMarkIcon,
} from '@heroicons/react/24/outline';
import { MicrophoneIcon as MicrophoneSolidIcon } from '@heroicons/react/24/solid';
import { useTrackToggle } from '@livekit/components-react';
import { Track } from 'livekit-client';
import { CoBrowseToolbarButton } from '@/components/call/cobrowse-toolbar-button';
import type { CoBrowsingSession } from '@/hooks/use-cobrowse';

interface ToolBarProps {
  onCapture: () => void;
  onEndCall: () => void;
  channelId?: string;
  agentId?: string;
  coBrowseSession?: CoBrowsingSession | null;
  onRequestCoBrowse?: () => void;
  onEndCoBrowse?: () => void;
}

export const ToolBar = ({
  onCapture,
  onEndCall,
  channelId,
  agentId,
  coBrowseSession,
  onRequestCoBrowse,
  onEndCoBrowse,
}: ToolBarProps) => {
  const { enabled: micEnabled, toggle: toggleMic } = useTrackToggle({
    source: Track.Source.Microphone,
  });
  const { enabled: cameraEnabled, toggle: toggleCamera } = useTrackToggle({
    source: Track.Source.Camera,
  });
  const { enabled: screenEnabled, toggle: toggleScreen } = useTrackToggle({
    source: Track.Source.ScreenShare,
  });

  return (
    <div className="flex items-center justify-center">
      <div className="flex items-center gap-3 rounded-full bg-gray-800/90 backdrop-blur-sm px-6 py-3 shadow-lg">
        {/* Mic */}
        <button
          onClick={() => toggleMic()}
          className={`flex items-center justify-center rounded-full p-3 transition-colors ${
            micEnabled
              ? 'text-gray-300 hover:bg-gray-700'
              : 'bg-red-600 text-white hover:bg-red-700'
          }`}
          aria-label={micEnabled ? '마이크 끄기' : '마이크 켜기'}
          aria-pressed={!micEnabled}
        >
          {micEnabled ? (
            <MicrophoneIcon className="h-6 w-6" />
          ) : (
            <MicrophoneSolidIcon className="h-6 w-6" />
          )}
        </button>

        {/* Camera */}
        <button
          onClick={() => toggleCamera()}
          className={`flex items-center justify-center rounded-full p-3 transition-colors ${
            cameraEnabled
              ? 'text-gray-300 hover:bg-gray-700'
              : 'bg-red-600 text-white hover:bg-red-700'
          }`}
          aria-label={cameraEnabled ? '카메라 끄기' : '카메라 켜기'}
          aria-pressed={!cameraEnabled}
        >
          {cameraEnabled ? (
            <VideoCameraIcon className="h-6 w-6" />
          ) : (
            <VideoCameraSlashIcon className="h-6 w-6" />
          )}
        </button>

        {/* Screen share */}
        <button
          onClick={() => toggleScreen()}
          className={`flex items-center justify-center rounded-full p-3 transition-colors ${
            screenEnabled
              ? 'bg-indigo-600 text-white hover:bg-indigo-700'
              : 'text-gray-300 hover:bg-gray-700'
          }`}
          aria-label={screenEnabled ? '화면 공유 중지' : '화면 공유'}
          aria-pressed={screenEnabled}
        >
          <ComputerDesktopIcon className="h-6 w-6" />
        </button>

        {/* Co-browse */}
        {channelId && agentId && onRequestCoBrowse && onEndCoBrowse && (
          <CoBrowseToolbarButton
            channelId={channelId}
            agentId={agentId}
            session={coBrowseSession ?? null}
            onRequest={onRequestCoBrowse}
            onEnd={onEndCoBrowse}
          />
        )}

        {/* Capture */}
        <button
          onClick={onCapture}
          className="flex items-center justify-center rounded-full p-3 text-gray-300 hover:bg-gray-700 transition-colors"
          aria-label="화면 캡처"
        >
          <CameraIcon className="h-6 w-6" />
        </button>

        {/* End call */}
        <button
          onClick={onEndCall}
          className="flex items-center justify-center rounded-full bg-red-600 px-5 py-3 text-white hover:bg-red-700 transition-colors gap-2"
          aria-label="통화 종료"
        >
          <PhoneXMarkIcon className="h-6 w-6" />
          <span className="text-sm font-medium">종료</span>
        </button>
      </div>
    </div>
  );
};
