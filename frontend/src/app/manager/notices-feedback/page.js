'use client';

import { useEffect, useState } from 'react';
import { authFetch } from '../../../lib/api';

export default function ManagerNoticesFeedbackPage() {
  const [notices, setNotices] = useState([]);
  const [feedback, setFeedback] = useState([]);
  const [form, setForm] = useState({ title: '', content: '', priority: 'NORMAL' });

  async function load() {
    const [nList, fPage] = await Promise.all([
      authFetch('/api/manager/notices'),
      authFetch('/api/manager/feedback?page=0&size=20'),
    ]);
    setNotices(nList || []);
    setFeedback(fPage?.content || []);
  }

  useEffect(() => {
    load();
  }, []);

  async function publishNotice(e) {
    e.preventDefault();
    await authFetch('/api/manager/notices', { method: 'POST', body: JSON.stringify(form) });
    setForm({ title: '', content: '', priority: 'NORMAL' });
    await load();
  }

  async function moderate(id, moderationStatus) {
    await authFetch(`/api/manager/feedback/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ moderationStatus }),
    });
    await load();
  }

  return (
    <div className="space-y-8">
      <section className="bg-white border border-slate-200 rounded-xl p-5 space-y-4">
        <h1 className="text-lg font-bold text-slate-900">Publish Hospital Notice</h1>
        <form onSubmit={publishNotice} className="space-y-3">
          <input required placeholder="Notice title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <textarea required rows={3} placeholder="Notice content..." value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <div className="flex items-center gap-3">
            <select value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })} className="rounded-lg border border-slate-300 px-3 py-2 text-sm bg-white">
              <option value="NORMAL">Normal</option>
              <option value="IMPORTANT">Important</option>
              <option value="URGENT">Urgent</option>
            </select>
            <button type="submit" className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg">
              Publish Notice
            </button>
          </div>
        </form>
      </section>

      <section className="bg-white border border-slate-200 rounded-xl p-5 space-y-3">
        <h2 className="text-base font-semibold text-slate-900">Moderate Patient Feedback</h2>
        {feedback.map((f) => (
          <div key={f.id} className="py-3 border-b border-slate-100 flex flex-wrap items-center justify-between gap-3 text-xs">
            <div>
              <p className="font-semibold text-slate-900">Rating: {f.rating}/5 · Status: {f.moderationStatus}</p>
              <p className="text-slate-600 mt-0.5">{f.comment}</p>
            </div>
            <div className="space-x-2">
              <button type="button" onClick={() => moderate(f.id, 'APPROVED')} className="text-emerald-700 font-semibold hover:underline">Approve</button>
              <button type="button" onClick={() => moderate(f.id, 'HIDDEN')} className="text-amber-700 font-semibold hover:underline">Hide</button>
              <button type="button" onClick={() => moderate(f.id, 'DELETED')} className="text-red-600 font-semibold hover:underline">Delete</button>
            </div>
          </div>
        ))}
        {notices.length > 0 && (
          <p className="text-xs text-slate-400 pt-2">Total published notices: {notices.length}</p>
        )}
      </section>
    </div>
  );
}
