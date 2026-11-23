'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { authFetch } from '../../lib/api';
import { createStompClient } from '../../lib/stompClient';
import LiveQueueBanner from '../../components/LiveQueueBanner';
import StatusBadge from '../../components/StatusBadge';
import { EmptyState, ErrorState, LoadingState } from '../../components/UiStates';

export default function PatientDashboardPage() {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [confirmCancelId, setConfirmCancelId] = useState(null);

  async function loadAppointments() {
    setLoading(true);
    setError('');
    try {
      const data = await authFetch('/api/patient/appointments');
      setAppointments(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAppointments();
  }, []);

  // RT-01: Live queue tracking for running sessions
  useEffect(() => {
    const runningSessionIds = [...new Set(
      appointments
        .filter((a) => ['OPEN', 'IN_PROGRESS'].includes(a.sessionStatus) && a.status !== 'CANCELLED')
        .map((a) => a.sessionId)
    )];
    if (runningSessionIds.length === 0) return;

    const client = createStompClient({
      token: null,
      onConnect: (c) => {
        runningSessionIds.forEach((sid) => {
          c.subscribe(`/topic/sessions/${sid}/queue`, (frame) => {
            const q = JSON.parse(frame.body);
            setAppointments((prev) =>
              prev.map((a) =>
                a.sessionId === sid
                  ? {
                      ...a,
                      currentServingNumber: q.currentServingNumber,
                      sessionStatus: q.status,
                      delayMinutes: q.delayMinutes,
                    }
                  : a
              )
            );
          });
        });
      },
    });
    return () => client.deactivate();
  }, [appointments.length]);

  async function handleConfirmCancel(id) {
    setError('');
    try {
      await authFetch(`/api/patient/appointments/${id}/cancel`, { method: 'PATCH' });
      setConfirmCancelId(null);
      await loadAppointments();
    } catch (err) {
      setError(err.message);
      setConfirmCancelId(null);
    }
  }

  const activeRunning = appointments.find(
    (a) => a.sessionStatus === 'IN_PROGRESS' && ['BOOKED', 'ARRIVED', 'IN_CONSULTATION'].includes(a.status)
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">My Appointments</h1>
          <p className="text-xs text-slate-500">Track live channelling queues, join online consultations, and print prescriptions.</p>
        </div>
        <Link
          href="/"
          className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800 whitespace-nowrap"
        >
          Book New Session
        </Link>
      </div>

      {activeRunning && (
        <LiveQueueBanner
          currentServingNumber={activeRunning.currentServingNumber}
          myNumber={activeRunning.appointmentNumber}
          delayMinutes={activeRunning.delayMinutes}
          status={activeRunning.sessionStatus}
        />
      )}

      {error && <ErrorState message={error} onRetry={loadAppointments} />}

      {loading ? (
        <LoadingState message="Loading your appointments..." />
      ) : appointments.length === 0 ? (
        <EmptyState
          title="You have no appointments yet"
          description="Search doctors on the home page to reserve a physical or online consultation."
        />
      ) : (
        <div className="space-y-3">
          {appointments.map((a) => (
            <div key={a.id} className="bg-white border border-slate-200 rounded-xl p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500">
                  <span className="font-mono font-semibold text-slate-800">{a.bookingReference}</span>
                  <span>·</span>
                  <span>No. {a.appointmentNumber}</span>
                  <span>·</span>
                  <StatusBadge status={a.status} />
                  <span>·</span>
                  <span>Payment: {a.paymentStatus}</span>
                </div>
                <h3 className="text-base font-semibold text-slate-900">
                  {a.doctorName} — {a.specializationName}
                </h3>
                <p className="text-xs text-slate-600 tabular-nums">
                  {a.sessionDate} · Est. {String(a.estimatedTime).slice(0, 5)} ·{' '}
                  {a.sessionType === 'ONLINE' ? 'Online Consultation' : `Physical (${a.roomNumber || 'TBA'})`} · LKR {Number(a.feeLkr).toLocaleString()}
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-2 shrink-0">
                {(a.sessionType === 'ONLINE' || a.status === 'COMPLETED') && (
                  <Link
                    href={`/patient/consultation/${a.id}`}
                    className="px-3.5 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800"
                  >
                    {a.status === 'COMPLETED' ? 'Summary & Feedback' : 'Consultation Room'}
                  </Link>
                )}

                {a.status === 'BOOKED' && a.paymentStatus === 'PENDING' && (
                  confirmCancelId === a.id ? (
                    <div className="flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={() => handleConfirmCancel(a.id)}
                        className="px-3 py-1.5 text-xs font-semibold text-white bg-red-600 rounded-lg"
                      >
                        Confirm Cancel
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmCancelId(null)}
                        className="px-2.5 py-1.5 text-xs text-slate-600 border border-slate-300 rounded-lg"
                      >
                        Keep
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setConfirmCancelId(a.id)}
                      className="px-3 py-2 text-xs font-medium text-red-700 border border-red-200 rounded-lg hover:bg-red-50"
                    >
                      Cancel
                    </button>
                  )
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
