'use client';

import { useEffect, useState } from 'react';

interface CoBrowseRequestDialogProps {
  onAccept: () => void;
  onDecline: () => void;
}

export function CoBrowseRequestDialog({ onAccept, onDecline }: CoBrowseRequestDialogProps) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setVisible(true), 10);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      role="dialog"
      aria-modal="true"
      aria-labelledby="cobrowse-dialog-title"
    >
      {/* Backdrop */}
      <div
        className={`absolute inset-0 bg-black transition-opacity duration-200 ${
          visible ? 'opacity-50' : 'opacity-0'
        }`}
        onClick={onDecline}
        aria-hidden="true"
      />

      {/* Dialog panel */}
      <div
        className={`relative z-10 mx-4 w-full max-w-sm rounded-2xl bg-gray-800 border border-gray-700 p-6 shadow-2xl transition-all duration-200 ${
          visible ? 'opacity-100 scale-100' : 'opacity-0 scale-95'
        }`}
      >
        {/* Icon */}
        <div className="flex items-center justify-center mb-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-indigo-900/50 border border-indigo-700">
            <svg
              className="h-6 w-6 text-indigo-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={1.5}
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M9 17.25v1.007a3 3 0 01-.879 2.122L7.5 21h9l-.621-.621A3 3 0 0115 18.257V17.25m6-12V15a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 15V5.25m18 0A2.25 2.25 0 0018.75 3H5.25A2.25 2.25 0 003 5.25m18 0H3"
              />
            </svg>
          </div>
        </div>

        {/* Content */}
        <h2
          id="cobrowse-dialog-title"
          className="text-center text-base font-semibold text-white mb-2"
        >
          Agent is requesting to view your browser screen
        </h2>
        <p className="text-center text-sm text-gray-400 mb-6">
          This will share your current browser tab with the agent to help guide you.
        </p>

        {/* Buttons */}
        <div className="flex gap-3">
          <button
            onClick={onDecline}
            className="flex-1 rounded-xl border border-gray-600 bg-gray-700 px-4 py-2.5 text-sm font-medium text-gray-300 hover:bg-gray-600 transition-colors"
            aria-label="Decline co-browsing request"
          >
            Decline
          </button>
          <button
            onClick={onAccept}
            className="flex-1 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-indigo-500 transition-colors"
            aria-label="Accept co-browsing request"
          >
            Accept
          </button>
        </div>
      </div>
    </div>
  );
}
