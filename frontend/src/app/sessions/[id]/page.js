'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { authFetch, publicFetch } from '../../../lib/api';
import { createStompClient } from '../../../lib/stompClient';
import FormField from '../../../components/FormField';
import StatusBadge from '../../../components/StatusBadge';
import { ErrorState, LoadingState } from '../../../components/UiStates';

export default function SessionDetailsPage() {
  const { id } = useParams();
  const router = useRouter();
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reasonForVisit, setReasonForVisit] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [bookingResult, setBookingResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  async function loadSession() {
    setLoading(true);
    setError('');
    try {
      const data = await publicFetch(`/api/public/sessions/${id}`);
      setSession(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadSession();

    // RT-02: Subscribe to live availability updates for everyone viewing this session
    const client = createStompClient({
      token: null,
      onConnect: (c) => {
        c.subscribe(`/topic/sessions/${id}/availability`, (frame) => {
          const update = JSON.parse(frame.body);
          setSession((prev) =>
            prev
              ? {
                  ...prev,
                  remainingNumbers: update.remainingNumbers,
                  lastIssuedNumber: update.lastIssuedNumber,
                }
              : prev
          );
        });
      },
    });
    return () => client.deactivate();
  }, [id]);

  async function handleBook(e) {
    e.preventDefault();
    setFieldError('');
    setError('');
    if (reasonForVisit.trim().length < 4) {
      setFieldError('Please enter at least 4 characters explaining the reason for your visit.');
      return;
    }

    setSubmitting(true);
    try {
      const created = await authFetch('/api/patient/appointments', {
        method: 'POST',
        body: JSON.stringify({ sessionId: Number(id), reasonForVisit: reasonForVisit.trim() }),
      });
      setBookingResult(created);
    } catch (err) {
      if (err.status === 401) {
        router.push(`/login?redirect=/sessions/${id}`);
        return;
      }
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <LoadingState message="Loading session details..." />;
  if (error && !session) return <ErrorState message={error} onRetry={loadSession} />;

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <Link href="/" className="text-xs font-medium text-teal-700 hover:underline">
        &larr; Back to Session Search
      </Link>

      <div className="bg-white border border-slate-200 rounded-xl p-6 space-y-4">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold text-slate-900">{session.doctorName}</h1>
            <p className="text-sm text-slate-500">
              {session.specializationName} · {session.slmcRegNo}
            </p>
          </div>
          <StatusBadge status={session.status} />
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-3 border-t border-slate-100 text-sm tabular-nums">
          <div>
            <span className="text-xs text-slate-400 block">Date</span>
            <span className="font-medium">{session.sessionDate}</span>
          </div>
          <div>
            <span className="text-xs text-slate-400 block">Time</span>
            <span className="font-medium">
              {String(session.startTime).slice(0, 5)} – {String(session.endTime).slice(0, 5)}
            </span>
          </div>
          <div>
            <span className="text-xs text-slate-400 block">Mode / Room</span>
            <span className="font-medium">
              {session.sessionType === 'ONLINE' ? 'Online Consultation' : session.roomNumber}
            </span>
          </div>
          <div>
            <span className="text-xs text-slate-400 block">Fee</span>
            <span className="font-semibold">LKR {Number(session.feeLkr).toLocaleString()}</span>
          </div>
        </div>

        <div className="bg-slate-50 rounded-lg p-4 flex items-center justify-between text-sm tabular-nums">
          <span className="text-slate-600">Live Numbers Remaining (updates automatically)</span>
          <span className="text-lg font-bold text-teal-700">
            {session.remainingNumbers} / {session.maxPatients}
          </span>
        </div>
      </div>

      {bookingResult ? (
        <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-6 space-y-3">
          <h2 className="text-lg font-bold text-emerald-900">Appointment Confirmed!</h2>
          <div className="grid grid-cols-2 gap-3 text-sm tabular-nums">
            <div>
              <span className="text-xs text-emerald-700 block">Booking Reference</span>
              <span className="font-mono font-semibold">{bookingResult.bookingReference}</span>
            </div>
            <div>
              <span className="text-xs text-emerald-700 block">Appointment Number</span>
              <span className="font-bold text-lg">No. {bookingResult.appointmentNumber}</span>
            </div>
            <div>
              <span className="text-xs text-emerald-700 block">Estimated Time</span>
              <span className="font-semibold">{String(bookingResult.estimatedTime).slice(0, 5)}</span>
            </div>
            <div>
              <span className="text-xs text-emerald-700 block">Room / Mode</span>
              <span className="font-semibold">
                {bookingResult.sessionType === 'ONLINE' ? 'Online Consultation' : bookingResult.roomNumber}
              </span>
            </div>
          </div>
          <Link
            href="/patient"
            className="inline-block mt-2 px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800"
          >
            Go to My Appointments
          </Link>
        </div>
      ) : (
        <form onSubmit={handleBook} className="bg-white border border-slate-200 rounded-xl p-6 space-y-4">
          <h2 className="text-base font-semibold text-slate-900">Reserve Your Number</h2>
          {error && <p className="text-xs text-red-600 bg-red-50 p-3 rounded-lg">{error}</p>}

          <FormField
            label="Reason for Visit (Encrypted at Rest — Visible only to You & Your Doctor)"
            name="reasonForVisit"
            rows={3}
            required
            value={reasonForVisit}
            onChange={(e) => setReasonForVisit(e.target.value)}
            placeholder="Briefly describe your symptoms or purpose of consultation..."
            error={fieldError}
          />

          <p className="text-xs text-slate-500">
            Booking closes {session.sessionType === 'PHYSICAL' ? '1 hour' : '30 minutes'} before the session starts (BR-05).
          </p>

          <button
            type="submit"
            disabled={submitting || session.remainingNumbers <= 0}
            className="px-5 py-2.5 text-sm font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800 disabled:opacity-50"
          >
            {submitting ? 'Confirming...' : session.remainingNumbers <= 0 ? 'Session Full' : 'Confirm Booking'}
          </button>
        </form>
      )}
    </div>
  );
}
