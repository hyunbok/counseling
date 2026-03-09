'use client';

import {
  useLocalParticipant,
  useRemoteParticipants,
  VideoTrack,
  AudioTrack,
} from '@livekit/components-react';
import { Track } from 'livekit-client';
import { VideoCameraSlashIcon } from '@heroicons/react/24/outline';

export function VideoRoom() {
  const { localParticipant, isCameraEnabled } = useLocalParticipant();
  const remoteParticipants = useRemoteParticipants();
  const remote = remoteParticipants[0]; // 1:1 call

  const remoteCameraPublication = remote?.getTrackPublication(Track.Source.Camera);
  const remoteAudioPublication = remote?.getTrackPublication(Track.Source.Microphone);
  const localCameraPublication = localParticipant.getTrackPublication(Track.Source.Camera);

  return (
    <div className="relative flex flex-1 items-center justify-center bg-gray-950">
      {/* Local (customer) video — main view */}
      <div className="flex h-full w-full items-center justify-center">
        {isCameraEnabled && localCameraPublication?.track ? (
          <VideoTrack
            trackRef={{
              participant: localParticipant,
              publication: localCameraPublication,
              source: Track.Source.Camera,
            }}
            className="h-full w-full object-contain"
          />
        ) : (
          <div className="flex flex-col items-center gap-3">
            <div className="h-64 w-96 rounded-xl bg-gray-800 flex items-center justify-center border border-gray-700">
              <div className="flex flex-col items-center gap-2">
                <VideoCameraSlashIcon className="h-10 w-10 text-gray-500" />
                <p className="text-gray-400 text-sm">카메라 꺼짐</p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Remote (agent) audio */}
      {remote && remoteAudioPublication?.track && (
        <AudioTrack
          trackRef={{
            participant: remote,
            publication: remoteAudioPublication,
            source: Track.Source.Microphone,
          }}
        />
      )}

      {/* Remote (agent) video — PIP */}
      <div className="absolute bottom-4 right-4 h-28 w-44 rounded-xl bg-gray-800 border border-gray-700 overflow-hidden shadow-lg">
        {remote && remoteCameraPublication?.track ? (
          <VideoTrack
            trackRef={{
              participant: remote,
              publication: remoteCameraPublication,
              source: Track.Source.Camera,
            }}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="flex h-full flex-col items-center justify-center gap-1">
            <VideoCameraSlashIcon className="h-6 w-6 text-gray-500" />
            <p className="text-gray-500 text-xs">
              {remote ? '카메라 꺼짐' : '상담원 대기 중...'}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
