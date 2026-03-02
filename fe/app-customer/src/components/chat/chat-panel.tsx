'use client';

import { useState, useRef, useEffect, FormEvent } from 'react';
import {
  ChatBubbleLeftRightIcon,
  XMarkIcon,
  PaperAirplaneIcon,
} from '@heroicons/react/24/outline';
import { useChat, type ChatMessage } from '@/hooks/use-chat';

interface ChatPanelProps {
  channelId: string;
  customerName: string;
}

export function ChatPanel({ channelId, customerName }: ChatPanelProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [input, setInput] = useState('');
  const [unread, setUnread] = useState(0);
  const { messages, sendMessage, isSending, isLoading } = useChat(channelId, customerName);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const prevCountRef = useRef(0);

  // Track unread messages when panel is closed
  useEffect(() => {
    if (!isOpen && messages.length > prevCountRef.current) {
      setUnread((prev) => prev + (messages.length - prevCountRef.current));
    }
    prevCountRef.current = messages.length;
  }, [messages.length, isOpen]);

  // Auto-scroll on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length]);

  // Clear unread when opening
  const handleToggle = () => {
    setIsOpen((prev) => !prev);
    if (!isOpen) setUnread(0);
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isSending) return;
    sendMessage(input);
    setInput('');
  };

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <>
      {/* Toggle button */}
      <button
        onClick={handleToggle}
        className="fixed bottom-24 right-4 z-50 rounded-full bg-indigo-600 p-3 text-white shadow-lg hover:bg-indigo-700 transition-colors"
        aria-label={isOpen ? '채팅 닫기' : '채팅 열기'}
      >
        {isOpen ? (
          <XMarkIcon className="h-6 w-6" />
        ) : (
          <div className="relative">
            <ChatBubbleLeftRightIcon className="h-6 w-6" />
            {unread > 0 && (
              <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center">
                {unread > 9 ? '9+' : unread}
              </span>
            )}
          </div>
        )}
      </button>

      {/* Chat panel */}
      {isOpen && (
        <div className="fixed bottom-24 right-4 z-40 w-80 h-96 sm:w-96 flex flex-col rounded-xl bg-gray-800 border border-gray-700 shadow-2xl overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-700">
            <h3 className="text-sm font-semibold text-white">채팅</h3>
            <button
              onClick={handleToggle}
              className="text-gray-400 hover:text-white"
              aria-label="채팅 닫기"
            >
              <XMarkIcon className="h-5 w-5" />
            </button>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
            {isLoading && (
              <p className="text-center text-gray-500 text-xs">메시지 로딩 중...</p>
            )}
            {messages.map((msg) => (
              <MessageBubble
                key={msg.id}
                message={msg}
                isOwn={msg.senderType === 'CUSTOMER'}
                formatTime={formatTime}
              />
            ))}
            <div ref={messagesEndRef} />
          </div>

          {/* Input */}
          <form
            onSubmit={handleSubmit}
            className="flex items-center gap-2 px-3 py-2 border-t border-gray-700"
          >
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="메시지를 입력하세요..."
              className="flex-1 rounded-lg bg-gray-700 px-3 py-2 text-sm text-white placeholder-gray-400 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              disabled={isSending}
            />
            <button
              type="submit"
              disabled={!input.trim() || isSending}
              className="rounded-lg bg-indigo-600 p-2 text-white hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              aria-label="전송"
            >
              <PaperAirplaneIcon className="h-4 w-4" />
            </button>
          </form>
        </div>
      )}
    </>
  );
}

function MessageBubble({
  message,
  isOwn,
  formatTime,
}: {
  message: ChatMessage;
  isOwn: boolean;
  formatTime: (d: string) => string;
}) {
  return (
    <div className={`flex flex-col ${isOwn ? 'items-end' : 'items-start'}`}>
      <div className={`flex items-end gap-1 ${isOwn ? 'flex-row-reverse' : 'flex-row'}`}>
        <div
          className={`max-w-[70%] rounded-lg px-3 py-2 text-sm ${
            isOwn ? 'bg-indigo-600 text-white' : 'bg-gray-700 text-gray-100'
          }`}
        >
          {!isOwn && (
            <p className="text-xs text-gray-400 mb-1 font-medium">{message.senderId}</p>
          )}
          <p className="break-words">{message.content}</p>
        </div>
      </div>
      <span className="text-xs text-gray-500 mt-1 px-1">{formatTime(message.createdAt)}</span>
    </div>
  );
}
