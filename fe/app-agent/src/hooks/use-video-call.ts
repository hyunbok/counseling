import { useState, useCallback } from 'react';

interface UseVideoCallOptions {
  roomName: string;
  token: string;
}

interface VideoCallState {
  isConnected: boolean;
  isCameraOn: boolean;
  isMicOn: boolean;
  isScreenSharing: boolean;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export const useVideoCall = (_options: UseVideoCallOptions) => {
  const [state, setState] = useState<VideoCallState>({
    isConnected: false,
    isCameraOn: true,
    isMicOn: true,
    isScreenSharing: false,
  });

  const connect = useCallback(async () => {
    // TODO: Connect to LiveKit room
    setState((prev) => ({ ...prev, isConnected: true }));
  }, []);

  const disconnect = useCallback(async () => {
    // TODO: Disconnect from LiveKit room
    setState((prev) => ({ ...prev, isConnected: false }));
  }, []);

  const toggleCamera = useCallback(() => {
    // TODO: Toggle camera track
    setState((prev) => ({ ...prev, isCameraOn: !prev.isCameraOn }));
  }, []);

  const toggleMic = useCallback(() => {
    // TODO: Toggle microphone track
    setState((prev) => ({ ...prev, isMicOn: !prev.isMicOn }));
  }, []);

  const toggleScreenShare = useCallback(async () => {
    // TODO: Toggle screen share track
    setState((prev) => ({ ...prev, isScreenSharing: !prev.isScreenSharing }));
  }, []);

  return {
    ...state,
    connect,
    disconnect,
    toggleCamera,
    toggleMic,
    toggleScreenShare,
  };
};
