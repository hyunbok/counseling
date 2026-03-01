'use client';

import { use, useEffect } from 'react';
import { MicrophoneIcon, VideoCameraIcon, PhoneXMarkIcon, VideoCameraSlashIcon } from '@heroicons/react/24/outline';
import { MicrophoneIcon as MicrophoneSolidIcon } from '@heroicons/react/24/solid';
import { useRouter } from 'next/navigation';
import { useVideoCall } from '@/hooks/use-video-call';
import { Button } from '@/components/ui/button';

interface CallPageProps {
  params: Promise<{ id: string }>;
}

export default function CallPage({ params }: CallPageProps) {
  const { id } = use(params);
  const router = useRouter();

  // TODO: Fetch real token from API using channelId before connecting
  const token = '';
  const hasToken = token !== '';

  const { isConnected, isCameraOn, isMicOn, toggleCamera, toggleMic, connect, disconnect } =
    useVideoCall({ roomName: id, token });

  // Connect to LiveKit room on mount once token is available
  useEffect(() => {
    if (!hasToken) return;
    connect();
    return () => {
      disconnect();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasToken]);

  const handleEndCall = async () => {
    await disconnect();
    router.push('/feedback');
  };

  if (!hasToken) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-900 text-white">
        <div className="text-center">
          <p className="text-amber-400 mb-4">통화 토큰을 가져오는 중...</p>
          <p className="text-sm text-gray-400">
            {/* TODO: Display error if token fetch fails */}
            잠시만 기다려 주세요.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col bg-gray-900 text-white">
      {/* Video area */}
      <div className="flex flex-1 items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="rounded-xl bg-gray-800 w-80 h-56 flex items-center justify-center border border-gray-700">
            {isCameraOn ? (
              <p className="text-gray-400 text-sm">카메라 영상</p>
            ) : (
              <div className="flex flex-col items-center gap-2">
                <VideoCameraSlashIcon className="h-10 w-10 text-gray-500" />
                <p className="text-gray-500 text-sm">카메라 꺼짐</p>
              </div>
            )}
          </div>
          <h2 className="text-lg font-semibold text-white">화상 통화 진행 중</h2>
          <p className="text-sm text-gray-400">
            통화 ID: <span className="font-mono text-indigo-400">{id}</span>
          </p>
          {!isConnected && <p className="text-sm text-amber-400">연결 중...</p>}
        </div>
      </div>

      {/* Controls */}
      <div className="flex items-center justify-center gap-4 py-6 border-t border-gray-800">
        <Button
          variant={isMicOn ? 'ghost' : 'danger'}
          onClick={toggleMic}
          aria-label={isMicOn ? '마이크 끄기' : '마이크 켜기'}
          className="rounded-full p-3 text-white"
        >
          {isMicOn ? (
            <MicrophoneIcon className="h-6 w-6" />
          ) : (
            <MicrophoneSolidIcon className="h-6 w-6" />
          )}
        </Button>

        <Button
          variant={isCameraOn ? 'ghost' : 'danger'}
          onClick={toggleCamera}
          aria-label={isCameraOn ? '카메라 끄기' : '카메라 켜기'}
          className="rounded-full p-3 text-white"
        >
          {isCameraOn ? (
            <VideoCameraIcon className="h-6 w-6" />
          ) : (
            <VideoCameraSlashIcon className="h-6 w-6" />
          )}
        </Button>

        <Button
          variant="danger"
          onClick={handleEndCall}
          aria-label="통화 종료"
          className="rounded-full px-6 py-3"
        >
          <PhoneXMarkIcon className="h-6 w-6 mr-2" />
          통화 종료
        </Button>
      </div>
    </div>
  );
}
