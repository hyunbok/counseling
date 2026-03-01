'use client';

import { FormEvent, useState } from 'react';
import { PaperAirplaneIcon } from '@heroicons/react/24/outline';

interface Message {
  id: string;
  sender: 'agent' | 'customer';
  text: string;
  time: string;
}

const INITIAL_MESSAGES: Message[] = [
  { id: '1', sender: 'customer', text: '안녕하세요, 상담 부탁드립니다.', time: '14:01' },
  { id: '2', sender: 'agent', text: '안녕하세요! 무엇을 도와드릴까요?', time: '14:02' },
];

export const ChatPanel = () => {
  const [messages, setMessages] = useState<Message[]>(INITIAL_MESSAGES);
  const [input, setInput] = useState('');

  const handleSend = (e: FormEvent) => {
    e.preventDefault();
    const text = input.trim();
    if (!text) return;
    const now = new Date();
    const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`;
    setMessages((prev) => [
      ...prev,
      { id: String(Date.now()), sender: 'agent', text, time },
    ]);
    setInput('');
  };

  return (
    <div className="flex flex-col h-full">
      {/* Message list */}
      <div className="flex-1 overflow-y-auto p-3 space-y-2">
        {messages.map((msg) => (
          <div
            key={msg.id}
            className={`flex ${msg.sender === 'agent' ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-[80%] rounded-xl px-3 py-2 text-sm ${
                msg.sender === 'agent'
                  ? 'bg-indigo-600 text-white'
                  : 'bg-gray-700 text-gray-100'
              }`}
            >
              <p>{msg.text}</p>
              <p className="text-xs opacity-70 mt-0.5 text-right">{msg.time}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Input */}
      <form
        onSubmit={handleSend}
        className="flex items-center gap-2 border-t border-gray-700 p-3"
      >
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="메시지 입력..."
          className="flex-1 rounded-lg bg-gray-700 border border-gray-600 px-3 py-2 text-sm text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          aria-label="채팅 메시지 입력"
        />
        <button
          type="submit"
          className="flex items-center justify-center rounded-lg bg-indigo-600 p-2 text-white hover:bg-indigo-700 transition-colors disabled:opacity-50"
          disabled={!input.trim()}
          aria-label="메시지 보내기"
        >
          <PaperAirplaneIcon className="h-5 w-5" />
        </button>
      </form>
    </div>
  );
};
