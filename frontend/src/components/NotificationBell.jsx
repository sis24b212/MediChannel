'use client';

import { useEffect, useState } from 'react';
import { authFetch } from '../lib/api';
import { createStompClient } from '../lib/stompClient';

export default function NotificationBell({ wsToken }) {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);

  async function loadNotifications() {
    try {
      const page = await authFetch('/api/notifications?page=0&size=10');
      setItems(page?.content || []);
      const countRes = await authFetch('/api/notifications/unread-count');
      setUnread(countRes?.unreadCount || 0);
    } catch {
      // ignore if guest
    }
  }

  useEffect(() => {
    if (!wsToken) return;
    loadNotifications();

    const client = createStompClient({
      token: wsToken,
      onConnect: (c) => {
        c.subscribe('/user/queue/notifications', (frame) => {
          const notif = JSON.parse(frame.body);
          setItems((prev) => [notif, ...prev]);
          setUnread((u) => u + 1);
        });
      },
    });
    return () => client.deactivate();
  }, [wsToken]);

  async function markRead(id) {
    await authFetch(`/api/notifications/${id}/read`, { method: 'PATCH' });
    setItems((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
    setUnread((u) => Math.max(0, u - 1));
  }

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="px-3 py-1.5 text-xs font-medium border border-slate-300 rounded-lg hover:bg-slate-50 tabular-nums"
      >
        Alerts {unread > 0 ? `(${unread})` : ''}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-80 bg-white border border-slate-200 rounded-xl shadow-lg z-40 p-3 space-y-2 max-h-96 overflow-y-auto">
          <div className="flex items-center justify-between pb-2 border-b border-slate-100">
            <span className="text-xs font-semibold text-slate-900">Notifications</span>
            <button type="button" onClick={() => setOpen(false)} className="text-xs text-slate-500">
              Close
            </button>
          </div>
          {items.length === 0 ? (
            <p className="text-xs text-slate-500 py-4 text-center">No notifications yet.</p>
          ) : (
            items.map((n) => (
              <div
                key={n.id}
                className={`p-2.5 rounded-lg text-xs border ${
                  n.read ? 'bg-white border-slate-100 text-slate-600' : 'bg-teal-50 border-teal-200 text-slate-900'
                }`}
              >
                <p className="font-semibold">{n.title}</p>
                <p className="mt-0.5">{n.message}</p>
                {!n.read && (
                  <button
                    type="button"
                    onClick={() => markRead(n.id)}
                    className="mt-1.5 text-[11px] font-semibold text-teal-700 hover:underline"
                  >
                    Mark as read
                  </button>
                )}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
