'use client';

import {
  MicrophoneIcon,
  VideoCameraIcon,
  VideoCameraSlashIcon,
  PhoneXMarkIcon,
} from '@heroicons/react/24/outline';
import { MicrophoneIcon as MicrophoneSolidIcon } from '@heroicons/react/24/solid';
import { useTrackToggle, useDisconnectButton } from '@livekit/components-react';
import { Track } from 'livekit-client';

interface ToolBarProps {
  onLeave: () => void;
}

export function ToolBar({ onLeave }: ToolBarProps) {
  const { enabled: micEnabled, toggle: toggleMic } = useTrackToggle({
    source: Track.Source.Microphone,
  });
  const { enabled: cameraEnabled, toggle: toggleCamera } = useTrackToggle({
    source: Track.Source.Camera,
  });
  const { buttonProps } = useDisconnectButton({ stopTracks: true });

  const handleLeave = () => {
    buttonProps.onClick?.({} as React.MouseEvent<HTMLButtonElement>);
    onLeave();
  };

  return (
    <div className="flex items-center justify-center py-4 bg-gray-900/80">
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
        >
          {cameraEnabled ? (
            <VideoCameraIcon className="h-6 w-6" />
          ) : (
            <VideoCameraSlashIcon className="h-6 w-6" />
          )}
        </button>

        {/* Leave */}
        <button
          onClick={handleLeave}
          className="flex items-center justify-center rounded-full bg-red-600 px-5 py-3 text-white hover:bg-red-700 transition-colors gap-2"
          aria-label="상담 종료"
        >
          <PhoneXMarkIcon className="h-6 w-6" />
          <span className="text-sm font-medium">종료</span>
        </button>
      </div>
    </div>
  );
}
