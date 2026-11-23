'use client';

import { useEffect, useState } from 'react';
import { authFetch, publicFetch } from '../../../lib/api';
import FormField from '../../../components/FormField';
import StatusBadge from '../../../components/StatusBadge';
import { ErrorState } from '../../../components/UiStates';

export default function ManagerSessionsPage() {
  const [sessions, setSessions] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    doctorId: '',
    roomId: '',
    sessionDate: '',
    startTime: '16:00',
    endTime: '18:00',
    sessionType: 'PHYSICAL',
    maxPatients: 20,
    minutesPerPatient: 15,
    feeLkr: 3500,
  });

  async function loadAll() {
    try {
      const [sList, dList, rList] = await Promise.all([
        publicFetch('/api/public/sessions'),
        publicFetch('/api/public/doctors'),
        authFetch('/api/manager/rooms'),
      ]);
      setSessions(sList || []);
      setDoctors(dList || []);
      setRooms(rList || []);
    } catch (err) {
      setError(err.message);
    }
  }

  useEffect(() => {
    loadAll();
  }, []);

  async function handleCreate(e) {
    e.preventDefault();
    setError('');
    try {
      await authFetch('/api/manager/sessions', {
        method: 'POST',
        body: JSON.stringify({
          ...form,
          doctorId: Number(form.doctorId),
          roomId: form.sessionType === 'PHYSICAL' ? Number(form.roomId) : null,
          maxPatients: Number(form.maxPatients),
          minutesPerPatient: Number(form.minutesPerPatient),
          feeLkr: Number(form.feeLkr),
        }),
      });
      await loadAll();
    } catch (err) {
      setError(err.message);
    }
  }

  async function delaySession(id, mins) {
    await authFetch(`/api/manager/sessions/${id}/delay`, {
      method: 'POST',
      body: JSON.stringify({ delayMinutes: mins }),
    });
    await loadAll();
  }

  async function cancelSession(id) {
    await authFetch(`/api/manager/sessions/${id}/cancel`, { method: 'POST' });
    await loadAll();
  }

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-bold text-slate-900">Manage Channelling Sessions</h1>
      {error && <ErrorState message={error} />}

      <form onSubmit={handleCreate} className="bg-white border border-slate-200 rounded-xl p-5 grid grid-cols-1 sm:grid-cols-3 gap-4">
        <FormField
          label="Doctor"
          name="doctorId"
          required
          value={form.doctorId}
          onChange={(e) => setForm({ ...form, doctorId: e.target.value })}
          options={[{ value: '', label: 'Select Doctor' }, ...doctors.map((d) => ({ value: d.id, label: `${d.fullName} (${d.specializationName})` }))]}
        />
        <FormField
          label="Session Type"
          name="sessionType"
          required
          value={form.sessionType}
          onChange={(e) => setForm({ ...form, sessionType: e.target.value })}
          options={[
            { value: 'PHYSICAL', label: 'Physical Clinic' },
            { value: 'ONLINE', label: 'Online Consultation' },
          ]}
        />
        {form.sessionType === 'PHYSICAL' && (
          <FormField
            label="Room"
            name="roomId"
            required
            value={form.roomId}
            onChange={(e) => setForm({ ...form, roomId: e.target.value })}
            options={[{ value: '', label: 'Select Room' }, ...rooms.map((r) => ({ value: r.id, label: `${r.roomNumber} (${r.floor})` }))]}
          />
        )}
        <FormField label="Date" name="sessionDate" type="date" required value={form.sessionDate} onChange={(e) => setForm({ ...form, sessionDate: e.target.value })} />
        <FormField label="Start Time" name="startTime" type="time" required value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
        <FormField label="End Time" name="endTime" type="time" required value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
        <FormField label="Max Patients (1–60)" name="maxPatients" type="number" required value={form.maxPatients} onChange={(e) => setForm({ ...form, maxPatients: e.target.value })} />
        <FormField label="Minutes / Patient (5–30)" name="minutesPerPatient" type="number" required value={form.minutesPerPatient} onChange={(e) => setForm({ ...form, minutesPerPatient: e.target.value })} />
        <FormField label="Fee (LKR)" name="feeLkr" type="number" required value={form.feeLkr} onChange={(e) => setForm({ ...form, feeLkr: e.target.value })} />

        <div className="sm:col-span-3">
          <button type="submit" className="px-5 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800">
            Create Session
          </button>
        </div>
      </form>

      <div className="bg-white border border-slate-200 rounded-xl overflow-x-auto">
        <table className="w-full text-left text-xs tabular-nums">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-600">
            <tr>
              <th className="py-3 px-4">Doctor</th>
              <th className="py-3 px-4">Date & Time</th>
              <th className="py-3 px-4">Type / Room</th>
              <th className="py-3 px-4">Delay</th>
              <th className="py-3 px-4">Status</th>
              <th className="py-3 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {sessions.map((s) => (
              <tr key={s.id}>
                <td className="py-3 px-4 font-semibold">{s.doctorName}</td>
                <td className="py-3 px-4">{s.sessionDate} {String(s.startTime).slice(0, 5)}–{String(s.endTime).slice(0, 5)}</td>
                <td className="py-3 px-4">{s.sessionType === 'ONLINE' ? 'Online' : s.roomNumber}</td>
                <td className="py-3 px-4">{s.delayMinutes} min</td>
                <td className="py-3 px-4"><StatusBadge status={s.status} /></td>
                <td className="py-3 px-4 text-right space-x-2">
                  <button type="button" onClick={() => delaySession(s.id, 15)} className="text-amber-700 font-semibold hover:underline">
                    +15m Delay
                  </button>
                  <button type="button" onClick={() => cancelSession(s.id)} className="text-red-600 font-semibold hover:underline">
                    Cancel Session
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
