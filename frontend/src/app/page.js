'use client';

import { useEffect, useState } from 'react';
import { publicFetch } from '../lib/api';
import SessionCard from '../components/SessionCard';
import { EmptyState, ErrorState, LoadingState } from '../components/UiStates';

export default function HomePage() {
  const [specializations, setSpecializations] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [notices, setNotices] = useState([]);
  const [filters, setFilters] = useState({
    doctorName: '',
    specializationId: '',
    date: '',
    sessionType: '',
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadInitial() {
    setLoading(true);
    setError('');
    try {
      const [specs, activeNotices, foundSessions] = await Promise.all([
        publicFetch('/api/public/specializations'),
        publicFetch('/api/public/notices'),
        publicFetch('/api/public/sessions'),
      ]);
      setSpecializations(specs || []);
      setNotices(activeNotices || []);
      setSessions(foundSessions || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadInitial();
  }, []);

  async function handleSearch(e) {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      if (filters.doctorName) params.set('doctorName', filters.doctorName);
      if (filters.specializationId) params.set('specializationId', filters.specializationId);
      if (filters.date) params.set('date', filters.date);
      if (filters.sessionType) params.set('sessionType', filters.sessionType);
      const data = await publicFetch(`/api/public/sessions?${params.toString()}`);
      setSessions(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-8">
      <section className="bg-white border border-slate-200 rounded-2xl p-6 sm:p-8 space-y-6">
        <div className="max-w-2xl space-y-2">
          <p className="text-xs font-semibold text-teal-700">Lanka Care Hospital, Colombo</p>
          <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">
            Book Specialist Channelling & Online Consultations
          </h1>
          <p className="text-sm text-slate-600">
            Search visiting consultants across 15 specializations, view live numbers remaining, and track your queue in real time.
          </p>
        </div>

        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          <input
            type="text"
            placeholder="Doctor name..."
            value={filters.doctorName}
            onChange={(e) => setFilters({ ...filters, doctorName: e.target.value })}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
          <select
            value={filters.specializationId}
            onChange={(e) => setFilters({ ...filters, specializationId: e.target.value })}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm bg-white"
          >
            <option value="">All Specializations</option>
            {specializations.map((sp) => (
              <option key={sp.id} value={sp.id}>
                {sp.name}
              </option>
            ))}
          </select>
          <input
            type="date"
            value={filters.date}
            onChange={(e) => setFilters({ ...filters, date: e.target.value })}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
          />
          <select
            value={filters.sessionType}
            onChange={(e) => setFilters({ ...filters, sessionType: e.target.value })}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm bg-white"
          >
            <option value="">All Modes</option>
            <option value="PHYSICAL">Physical Clinic</option>
            <option value="ONLINE">Online Consultation</option>
          </select>
          <button
            type="submit"
            className="px-4 py-2 text-sm font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800"
          >
            Search Sessions
          </button>
        </form>
      </section>

      {notices.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-base font-semibold text-slate-900">Hospital Notices</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {notices.map((n) => (
              <div key={n.id} className="bg-white border border-slate-200 rounded-xl p-4 space-y-1">
                <div className="flex items-center justify-between gap-2">
                  <h3 className="text-sm font-semibold text-slate-900">{n.title}</h3>
                  <span className="text-xs font-medium text-teal-700">{n.priority}</span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">{n.content}</p>
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">Available Channelling Sessions</h2>
          <span className="text-xs text-slate-500 tabular-nums">{sessions.length} sessions</span>
        </div>

        {loading ? (
          <LoadingState message="Loading available channelling sessions..." />
        ) : error ? (
          <ErrorState message={error} onRetry={loadInitial} />
        ) : sessions.length === 0 ? (
          <EmptyState
            title="No matching channelling sessions"
            description="Try clearing a filter or selecting another date."
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {sessions.map((s) => (
              <SessionCard key={s.id} session={s} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
