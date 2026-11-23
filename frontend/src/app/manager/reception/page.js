'use client';

import { useEffect, useState } from 'react';
import { authFetch } from '../../../lib/api';
import StatusBadge from '../../../components/StatusBadge';
import { ErrorState } from '../../../components/UiStates';

export default function ManagerReceptionPage() {
  const [appointments, setAppointments] = useState([]);
  const [walkIn, setWalkIn] = useState({ sessionId: '', patientNic: '', reasonForVisit: 'Walk-in reception consultation' });
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function loadToday() {
    try {
      const page = await authFetch('/api/manager/reception/appointments?page=0&size=50');
      setAppointments(page?.content || []);
    } catch (err) {
      setError(err.message);
    }
  }

  useEffect(() => {
    loadToday();
  }, []);

  async function markArrived(id) {
    await authFetch(`/api/manager/reception/appointments/${id}/arrive`, { method: 'PATCH' });
    await loadToday();
  }

  async function markPaid(id, method) {
    await authFetch(`/api/manager/reception/appointments/${id}/payment`, {
      method: 'PATCH',
      body: JSON.stringify({ paymentStatus: 'PAID', paymentMethod: method }),
    });
    await loadToday();
  }

  async function submitWalkIn(e) {
    e.preventDefault();
    setError('');
    setNotice('');
    try {
      const res = await authFetch('/api/manager/reception/walk-in', {
        method: 'POST',
        body: JSON.stringify({ ...walkIn, sessionId: Number(walkIn.sessionId) }),
      });
      setNotice(`Walk-in booked! Reference: ${res.bookingReference} (No. ${res.appointmentNumber})`);
      await loadToday();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-bold text-slate-900">Reception Check-In, Payments & Walk-In Desk</h1>
      {error && <ErrorState message={error} />}
      {notice && <p className="text-xs text-emerald-700 bg-emerald-50 p-3 rounded-lg">{notice}</p>}

      <form onSubmit={submitWalkIn} className="bg-white border border-slate-200 rounded-xl p-5 flex flex-wrap items-end gap-3">
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Session ID</label>
          <input type="number" required value={walkIn.sessionId} onChange={(e) => setWalkIn({ ...walkIn, sessionId: e.target.value })} className="rounded-lg border border-slate-300 px-3 py-2 text-sm w-32" />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Patient NIC</label>
          <input type="text" required value={walkIn.patientNic} onChange={(e) => setWalkIn({ ...walkIn, patientNic: e.target.value })} placeholder="199512345678" className="rounded-lg border border-slate-300 px-3 py-2 text-sm w-48" />
        </div>
        <button type="submit" className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800">
          Book Walk-In Patient
        </button>
      </form>

      <div className="bg-white border border-slate-200 rounded-xl overflow-x-auto">
        <table className="w-full text-left text-xs tabular-nums">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-600">
            <tr>
              <th className="py-3 px-4">Ref / No.</th>
              <th className="py-3 px-4">Patient</th>
              <th className="py-3 px-4">Doctor</th>
              <th className="py-3 px-4">Status</th>
              <th className="py-3 px-4">Payment</th>
              <th className="py-3 px-4 text-right">Reception Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {appointments.map((a) => (
              <tr key={a.id}>
                <td className="py-3 px-4 font-mono">{a.bookingReference} (#{a.appointmentNumber})</td>
                <td className="py-3 px-4 font-semibold">{a.patientName}</td>
                <td className="py-3 px-4">{a.doctorName} ({a.sessionType})</td>
                <td className="py-3 px-4"><StatusBadge status={a.status} /></td>
                <td className="py-3 px-4">{a.paymentStatus} {a.paymentMethod ? `(${a.paymentMethod})` : ''}</td>
                <td className="py-3 px-4 text-right space-x-2 whitespace-nowrap">
                  {a.sessionType === 'PHYSICAL' && a.status === 'BOOKED' && (
                    <button type="button" onClick={() => markArrived(a.id)} className="text-blue-700 font-semibold hover:underline">
                      Mark Arrived
                    </button>
                  )}
                  {a.paymentStatus !== 'PAID' && (
                    <>
                      <button type="button" onClick={() => markPaid(a.id, 'CASH')} className="text-emerald-700 font-semibold hover:underline">
                        Pay Cash
                      </button>
                      <button type="button" onClick={() => markPaid(a.id, 'CARD')} className="text-teal-700 font-semibold hover:underline">
                        Pay Card
                      </button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
