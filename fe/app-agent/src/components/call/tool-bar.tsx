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

interface ToolBarProps {
  isMicOn: boolean;
  isCameraOn: boolean;
  isScreenSharing: boolean;
  onToggleMic: () => void;
  onToggleCamera: () => void;
  onToggleScreenShare: () => void;
  onCapture: () => void;
  onEndCall: () => void;
}

export const ToolBar = ({
  isMicOn,
  isCameraOn,
  isScreenSharing,
  onToggleMic,
  onToggleCamera,
  onToggleScreenShare,
  onCapture,
  onEndCall,
}: ToolBarProps) => {
  return (
    <div className="flex items-center justify-center">
      <div className="flex items-center gap-3 rounded-full bg-gray-800/90 backdrop-blur-sm px-6 py-3 shadow-lg">
        {/* Mic */}
        <button
          onClick={onToggleMic}
          className={`flex items-center justify-center rounded-full p-3 transition-colors ${
            isMicOn
              ? 'text-gray-300 hover:bg-gray-700'
              : 'bg-red-600 text-white hover:bg-red-700'
          }`}
          aria-label={isMicOn ? '마이크 끄기' : '마이크 켜기'}
          aria-pressed={!isMicOn}
        >
          {isMicOn ? (
            <MicrophoneIcon className="h-6 w-6" />
          ) : (
            <MicrophoneSolidIcon className="h-6 w-6" />
          )}
        </button>

        {/* Camera */}
        <button
          onClick={onToggleCamera}
          className={`flex items-center justify-center rounded-full p-3 transition-colors ${
            isCameraOn
              ? 'text-gray-300 hover:bg-gray-700'
              : 'bg-red-600 text-white hover:bg-red-700'
          }`}
          aria-label={isCameraOn ? '카메라 끄기' : '카메라 켜기'}
          aria-pressed={!isCameraOn}
        >
          {isCameraOn ? (
            <VideoCameraIcon className="h-6 w-6" />
          ) : (
            <VideoCameraSlashIcon className="h-6 w-6" />
          )}
        </button>

        {/* Screen share */}
        <button
          onClick={onToggleScreenShare}
          className={`flex items-center justify-center rounded-full p-3 transition-colors ${
            isScreenSharing
              ? 'bg-indigo-600 text-white hover:bg-indigo-700'
              : 'text-gray-300 hover:bg-gray-700'
          }`}
          aria-label={isScreenSharing ? '화면 공유 중지' : '화면 공유'}
          aria-pressed={isScreenSharing}
        >
          <ComputerDesktopIcon className="h-6 w-6" />
        </button>

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
