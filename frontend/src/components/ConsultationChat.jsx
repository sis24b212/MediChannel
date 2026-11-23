'use client';

import { useEffect, useRef, useState } from 'react';
import { createStompClient } from '../lib/stompClient';
import { authFetch } from '../lib/api';

/**
 * Private real-time doctor-patient consultation chat (RT-03, BR-09).
 * Includes connection status, typing indicator, 1000-char limit, and read-only lock after end.
 */
export default function ConsultationChat({
  appointmentId,
  initialMessages = [],
  roomOpen = false,
  currentUserRole,
}) {
  const [messages, setMessages] = useState(initialMessages);
  const [input, setInput] = useState('');
  const [connected, setConnected] = useState(false);
  const [otherTyping, setOtherTyping] = useState(false);
  const [isRoomOpen, setIsRoomOpen] = useState(roomOpen);
  const [error, setError] = useState('');
  const stompRef = useRef(null);

  useEffect(() => {
    setMessages(initialMessages);
  }, [initialMessages]);

  useEffect(() => {
    setIsRoomOpen(roomOpen);
  }, [roomOpen]);

  useEffect(() => {
    let active = true;
    fetch('/api/auth/me', { cache: 'no-store' })
      .then((r) => r.json())
      .then((me) => {
        if (!active || !me.wsToken) return;
        stompRef.current = createStompClient({
          token: me.wsToken,
          onConnect: (client) => {
            setConnected(true);
            client.subscribe(`/topic/consultations/${appointmentId}/chat`, (frame) => {
              const incoming = JSON.parse(frame.body);
              setMessages((prev) => (prev.some((m) => m.id === incoming.id) ? prev : [...prev, incoming]));
            });
            client.subscribe(`/topic/consultations/${appointmentId}/typing`, (frame) => {
              const data = JSON.parse(frame.body);
              if (data.senderRole !== currentUserRole) {
                setOtherTyping(Boolean(data.typing));
              }
            });
            client.subscribe(`/topic/consultations/${appointmentId}/status`, (frame) => {
              const data = JSON.parse(frame.body);
              if (data.roomOpen === false) {
                setIsRoomOpen(false);
              }
            });
          },
          onDisconnect: () => setConnected(false),
        });
      });

    return () => {
      active = false;
      if (stompRef.current) {
        stompRef.current.deactivate();
      }
    };
  }, [appointmentId, currentUserRole]);

  function emitTyping(isTyping) {
    if (stompRef.current && connected && isRoomOpen) {
      stompRef.current.publish({
        destination: `/app/consultations/${appointmentId}/typing`,
        body: JSON.stringify({ typing: isTyping }),
      });
    }
  }

  async function handleSend(e) {
    e.preventDefault();
    setError('');
    const trimmed = input.trim();
    if (!trimmed) return;
    if (trimmed.length > 1000) {
      setError('Chat messages cannot exceed 1000 characters.');
      return;
    }

    try {
      if (stompRef.current && connected) {
        stompRef.current.publish({
          destination: `/app/consultations/${appointmentId}/chat`,
          body: JSON.stringify({ messageText: trimmed }),
        });
      } else {
        const saved = await authFetch(`/api/consultations/${appointmentId}/messages`, {
          method: 'POST',
          body: JSON.stringify({ messageText: trimmed }),
        });
        setMessages((prev) => (prev.some((m) => m.id === saved.id) ? prev : [...prev, saved]));
      }
      setInput('');
      emitTyping(false);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="bg-white border border-slate-200 rounded-xl flex flex-col h-[460px]">
      <div className="px-4 py-3 border-b border-slate-200 flex items-center justify-between">
        <div>
          <h3 className="text-sm font-semibold text-slate-900">Encrypted Online Consultation Chat</h3>
          <p className="text-xs text-slate-500">
            {isRoomOpen ? 'AES-256-GCM Encrypted · Private Doctor-Patient Room' : 'Consultation Ended · Read-Only Transcript'}
          </p>
        </div>
        <span className={`text-xs font-medium ${connected ? 'text-emerald-700' : 'text-amber-700'}`}>
          {connected ? 'Live Connected' : 'Reconnecting...'}
        </span>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-50">
        {messages.length === 0 ? (
          <p className="text-xs text-slate-500 text-center py-8">No messages yet.</p>
        ) : (
          messages.map((m) => {
            const mine = m.senderRole === currentUserRole;
            return (
              <div key={m.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`max-w-[78%] rounded-xl px-3.5 py-2.5 text-sm ${
                    mine
                      ? 'bg-teal-700 text-white'
                      : 'bg-white border border-slate-200 text-slate-900'
                  }`}
                >
                  <p className={`text-[11px] font-medium mb-0.5 ${mine ? 'text-teal-100' : 'text-slate-500'}`}>
                    {m.senderName} ({m.senderRole})
                  </p>
                  <p className="whitespace-pre-wrap break-words">{m.messageText}</p>
                </div>
              </div>
            );
          })
        )}
        {otherTyping && <p className="text-xs text-slate-500 italic">Other participant is typing...</p>}
      </div>

      {error && <p className="px-4 py-1.5 text-xs text-red-600 bg-red-50">{error}</p>}

      <form onSubmit={handleSend} className="p-3 border-t border-slate-200 flex items-center gap-2">
        <input
          type="text"
          value={input}
          maxLength={1000}
          disabled={!isRoomOpen}
          onChange={(e) => {
            setInput(e.target.value);
            emitTyping(e.target.value.length > 0);
          }}
          placeholder={isRoomOpen ? 'Type a secure message (max 1000 chars)...' : 'Consultation room is read-only.'}
          className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100"
        />
        <button
          type="submit"
          disabled={!isRoomOpen || !input.trim()}
          className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800 disabled:opacity-50"
        >
          Send
        </button>
      </form>
    </div>
  );
}
