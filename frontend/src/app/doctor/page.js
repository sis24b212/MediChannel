'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { authFetch } from '../../lib/api';
import StatusBadge from '../../components/StatusBadge';
import { EmptyState, ErrorState, LoadingState } from '../../components/UiStates';

export default function DoctorConsolePage() {
  const [sessions, setSessions] = useState([]);
  const [selectedSessionId, setSelectedSessionId] = useState(null);
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadSessions() {
    setLoading(true);
    setError('');
    try {
      const list = await authFetch('/api/doctor/sessions');
      setSessions(list || []);
      if (list && list.length > 0 && !selectedSessionId) {
        setSelectedSessionId(list[0].id);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadPatients(sid) {
    if (!sid) return;
    try {
      const rows = await authFetch(`/api/doctor/sessions/${sid}/patients`);
      setPatients(rows || []);
    } catch (err) {
      setError(err.message);
    }
  }

  useEffect(() => {
    loadSessions();
  }, []);

  useEffect(() => {
    if (selectedSessionId) loadPatients(selectedSessionId);
  }, [selectedSessionId]);

  async function runSessionAction(action) {
    setError('');
    try {
      await authFetch(`/api/doctor/sessions/${selectedSessionId}/${action}`, { method: 'POST' });
      await loadSessions();
      await loadPatients(selectedSessionId);
    } catch (err) {
      setError(err.message);
    }
  }

  async function markStatus(appointmentId, status) {
    setError('');
    try {
      await authFetch(`/api/doctor/appointments/${appointmentId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      });
      await loadPatients(selectedSessionId);
    } catch (err) {
      setError(err.message);
    }
  }

  const activeSession = sessions.find((s) => s.id === selectedSessionId);

  if (loading) return <LoadingState message="Loading your channelling sessions..." />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Doctor Consultation Console</h1>
        <p className="text-xs text-slate-500">Manage live queues, call next patients, and run encrypted online consultations.</p>
      </div>

      {error && <ErrorState message={error} />}

      {sessions.length === 0 ? (
        <EmptyState title="No sessions assigned" description="Sessions scheduled by the Channelling Manager will appear here." />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="space-y-2">
            <h2 className="text-sm font-semibold text-slate-700">My Sessions</h2>
            {sessions.map((s) => (
              <button
                key={s.id}
                type="button"
                onClick={() => setSelectedSessionId(s.id)}
                className={`w-full text-left p-4 rounded-xl border transition-colors ${
                  s.id === selectedSessionId
                    ? 'bg-teal-50 border-teal-600'
                    : 'bg-white border-slate-200 hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold text-slate-900">{s.sessionDate}</span>
                  <StatusBadge status={s.status} />
                </div>
                <p className="text-xs text-slate-600 mt-1 tabular-nums">
                  {String(s.startTime).slice(0, 5)}–{String(s.endTime).slice(0, 5)} · {s.sessionType} · Serving No. {s.currentServingNumber}
                </p>
              </button>
            ))}
          </div>

          <div className="lg:col-span-2 space-y-4">
            {activeSession && (
              <div className="bg-white border border-slate-200 rounded-xl p-5 flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-xs text-slate-500">
                    {activeSession.sessionDate} · {activeSession.sessionType} · {activeSession.roomNumber || 'Online Room'}
                  </p>
                  <p className="text-lg font-bold text-slate-900 tabular-nums">
                    Now Serving: No. {activeSession.currentServingNumber || '—'}
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  <button
                    type="button"
                    onClick={() => runSessionAction('start')}
                    className="px-3.5 py-2 text-xs font-semibold border border-slate-300 rounded-lg hover:bg-slate-50"
                  >
                    Start Session
                  </button>
                  <button
                    type="button"
                    onClick={() => runSessionAction('call-next')}
                    className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800"
                  >
                    Call Next Patient
                  </button>
                  <button
                    type="button"
                    onClick={() => runSessionAction('end')}
                    className="px-3.5 py-2 text-xs font-semibold text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
                  >
                    End Session
                  </button>
                </div>
              </div>
            )}

            <div className="bg-white border border-slate-200 rounded-xl overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 border-b border-slate-200 text-slate-600">
                  <tr>
                    <th className="py-3 px-4">No.</th>
                    <th className="py-3 px-4">Patient</th>
                    <th className="py-3 px-4">Est. Time</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 tabular-nums">
                  {patients.map((p) => (
                    <tr key={p.id}>
                      <td className="py-3 px-4 font-bold text-slate-900">#{p.appointmentNumber}</td>
                      <td className="py-3 px-4">
                        <p className="font-semibold text-slate-900">{p.patientName}</p>
                        <p className="text-slate-500">{p.reasonForVisit}</p>
                      </td>
                      <td className="py-3 px-4">{String(p.estimatedTime).slice(0, 5)}</td>
                      <td className="py-3 px-4"><StatusBadge status={p.status} /></td>
                      <td className="py-3 px-4 text-right space-x-2 whitespace-nowrap">
                        <Link
                          href={`/doctor/consultation/${p.id}`}
                          className="text-teal-700 font-semibold hover:underline"
                        >
                          Open Room / Notes
                        </Link>
                        {p.status !== 'COMPLETED' && p.status !== 'CANCELLED' && (
                          <>
                            <button
                              type="button"
                              onClick={() => markStatus(p.id, 'COMPLETED')}
                              className="text-emerald-700 font-semibold hover:underline"
                            >
                              Complete
                            </button>
                            <button
                              type="button"
                              onClick={() => markStatus(p.id, 'NO_SHOW')}
                              className="text-red-600 font-semibold hover:underline"
                            >
                              No-Show
                            </button>
                          </>
                        )}
                      </td>
                    </tr>
                  ))}
                  {patients.length === 0 && (
                    <tr>
                      <td colSpan={5} className="py-8 text-center text-slate-500">
                        No patients booked in this session yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
