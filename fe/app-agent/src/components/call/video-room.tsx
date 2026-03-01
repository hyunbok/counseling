'use client';

import { VideoCameraSlashIcon } from '@heroicons/react/24/outline';

interface VideoRoomProps {
  isConnected: boolean;
  isCameraOn: boolean;
}

export const VideoRoom = ({ isConnected, isCameraOn }: VideoRoomProps) => {
  return (
    <div className="relative flex flex-1 items-center justify-center bg-gray-950">
      {/* Remote video (main area) */}
      <div className="flex h-full w-full items-center justify-center">
        {isConnected ? (
          <div className="flex flex-col items-center gap-3">
            <div className="h-64 w-96 rounded-xl bg-gray-800 flex items-center justify-center border border-gray-700">
              <p className="text-gray-400 text-sm">고객 영상</p>
            </div>
            <p className="text-sm text-gray-400">연결됨</p>
          </div>
        ) : (
          <div className="flex flex-col items-center gap-3">
            <div className="h-64 w-96 rounded-xl bg-gray-800 flex items-center justify-center border border-gray-700">
              <p className="text-amber-400 text-sm">연결 중...</p>
            </div>
          </div>
        )}
      </div>

      {/* Local PiP (bottom-right corner) */}
      <div className="absolute bottom-4 right-4 h-28 w-44 rounded-xl bg-gray-800 border border-gray-700 flex items-center justify-center overflow-hidden shadow-lg">
        {isCameraOn ? (
          <p className="text-gray-500 text-xs">내 카메라</p>
        ) : (
          <div className="flex flex-col items-center gap-1">
            <VideoCameraSlashIcon className="h-6 w-6 text-gray-500" />
            <p className="text-gray-500 text-xs">카메라 꺼짐</p>
          </div>
        )}
      </div>
    </div>
  );
};
