'use client';

import { useEffect, useState } from 'react';
import { authFetch } from '../../lib/api';
import StatusBadge from '../../components/StatusBadge';
import { ErrorState, LoadingState } from '../../components/UiStates';

export default function ManagerDashboardPage() {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadReport() {
    setLoading(true);
    setError('');
    try {
      const data = await authFetch('/api/manager/reports/dashboard');
      setReport(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadReport();
  }, []);

  if (loading) return <LoadingState message="Calculating hospital reports from PostgreSQL..." />;
  if (error) return <ErrorState message={error} onRetry={loadReport} />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Channelling Centre Manager Dashboard</h1>
        <p className="text-xs text-slate-500">Live operational status and backend SQL/JPA analytics. Medical notes remain strictly private.</p>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 tabular-nums">
        <div className="bg-white border border-slate-200 rounded-xl p-4">
          <span className="text-xs text-slate-500">Paid Revenue (LKR)</span>
          <p className="text-xl font-bold text-slate-900 mt-1">
            LKR {Number(report.totalRevenueLkr || 0).toLocaleString()}
          </p>
        </div>
        <div className="bg-white border border-slate-200 rounded-xl p-4">
          <span className="text-xs text-slate-500">Physical Appointments</span>
          <p className="text-xl font-bold text-slate-900 mt-1">{report.physicalVsOnline?.PHYSICAL || 0}</p>
        </div>
        <div className="bg-white border border-slate-200 rounded-xl p-4">
          <span className="text-xs text-slate-500">Online Consultations</span>
          <p className="text-xl font-bold text-teal-700 mt-1">{report.physicalVsOnline?.ONLINE || 0}</p>
        </div>
        <div className="bg-white border border-slate-200 rounded-xl p-4">
          <span className="text-xs text-slate-500">No-Show Rate</span>
          <p className="text-xl font-bold text-slate-900 mt-1">{report.noShowRatePercent}%</p>
        </div>
      </div>

      <div className="bg-white border border-slate-200 rounded-xl p-5 space-y-3">
        <h2 className="text-base font-semibold text-slate-900">Today&apos;s Channelling Sessions</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs tabular-nums">
            <thead className="border-b border-slate-200 text-slate-500">
              <tr>
                <th className="py-2 pr-4">Doctor</th>
                <th className="py-2 pr-4">Mode / Room</th>
                <th className="py-2 pr-4">Time</th>
                <th className="py-2 pr-4">Booked</th>
                <th className="py-2 pr-4">Serving</th>
                <th className="py-2">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {(report.todaySessions || []).map((s) => (
                <tr key={s.id}>
                  <td className="py-2.5 pr-4 font-semibold text-slate-900">{s.doctorName}</td>
                  <td className="py-2.5 pr-4">{s.sessionType === 'ONLINE' ? 'Online' : s.roomNumber}</td>
                  <td className="py-2.5 pr-4">{String(s.startTime).slice(0, 5)}–{String(s.endTime).slice(0, 5)}</td>
                  <td className="py-2.5 pr-4">{s.lastIssuedNumber} / {s.maxPatients}</td>
                  <td className="py-2.5 pr-4">No. {s.currentServingNumber}</td>
                  <td className="py-2.5"><StatusBadge status={s.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white border border-slate-200 rounded-xl p-5 space-y-2">
          <h2 className="text-sm font-semibold text-slate-900">Top Doctors by Appointments</h2>
          {(report.topDoctors || []).map((d) => (
            <div key={d.doctorId} className="flex justify-between text-xs py-1.5 border-b border-slate-100 tabular-nums">
              <span>{d.doctorName}</span>
              <span className="font-semibold">{d.appointmentCount} bookings</span>
            </div>
          ))}
        </div>

        <div className="bg-white border border-slate-200 rounded-xl p-5 space-y-2">
          <h2 className="text-sm font-semibold text-slate-900">Average Doctor Rating (Approved Feedback)</h2>
          {(report.averageRatings || []).map((r) => (
            <div key={r.doctorId} className="flex justify-between text-xs py-1.5 border-b border-slate-100 tabular-nums">
              <span>{r.doctorName}</span>
              <span className="font-semibold">{r.averageRating} / 5 ({r.reviewCount} reviews)</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
