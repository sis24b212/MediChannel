'use client';

import { useEffect, useState } from 'react';
import { authFetch } from '../../lib/api';
import FormField from '../../components/FormField';
import { ErrorState, LoadingState } from '../../components/UiStates';

export default function AdminDashboardPage() {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState({ fullName: '', email: '', role: 'DOCTOR', temporaryPassword: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  async function loadAll() {
    setLoading(true);
    setError('');
    try {
      const [dash, userPage] = await Promise.all([
        authFetch('/api/admin/dashboard'),
        authFetch('/api/admin/users?page=0&size=50'),
      ]);
      setStats(dash);
      setUsers(userPage?.content || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAll();
  }, []);

  async function createStaff(e) {
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      await authFetch('/api/admin/staff', { method: 'POST', body: JSON.stringify(form) });
      setMessage(`Created ${form.role} account. User must change temporary password on first login.`);
      setForm({ fullName: '', email: '', role: 'DOCTOR', temporaryPassword: '' });
      await loadAll();
    } catch (err) {
      setError(err.message);
    }
  }

  async function toggleStatus(id, active) {
    setError('');
    try {
      await authFetch(`/api/admin/users/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ active }),
      });
      await loadAll();
    } catch (err) {
      setError(err.message);
    }
  }

  if (loading) return <LoadingState message="Loading IT Admin dashboard..." />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">IT Administration & User Accounts</h1>
        <p className="text-xs text-slate-500">Create DOCTOR and MANAGER accounts, manage status, and reset staff passwords.</p>
      </div>

      {error && <ErrorState message={error} />}
      {message && <p className="text-xs text-emerald-700 bg-emerald-50 p-3 rounded-lg">{message}</p>}

      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 tabular-nums">
          <div className="bg-white border border-slate-200 rounded-xl p-4"><span className="text-xs text-slate-500">Total Users</span><p className="text-xl font-bold">{stats.totalUsers}</p></div>
          <div className="bg-white border border-slate-200 rounded-xl p-4"><span className="text-xs text-slate-500">Doctors</span><p className="text-xl font-bold">{stats.doctors}</p></div>
          <div className="bg-white border border-slate-200 rounded-xl p-4"><span className="text-xs text-slate-500">Managers</span><p className="text-xl font-bold">{stats.managers}</p></div>
          <div className="bg-white border border-slate-200 rounded-xl p-4"><span className="text-xs text-slate-500">Audit Events</span><p className="text-xl font-bold">{stats.totalAuditEvents}</p></div>
        </div>
      )}

      <form onSubmit={createStaff} className="bg-white border border-slate-200 rounded-xl p-5 grid grid-cols-1 sm:grid-cols-4 gap-3 items-end">
        <FormField label="Full Name" name="fullName" required value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        <FormField label="Email" name="email" type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <FormField
          label="Staff Role"
          name="role"
          value={form.role}
          onChange={(e) => setForm({ ...form, role: e.target.value })}
          options={[
            { value: 'DOCTOR', label: 'Doctor' },
            { value: 'MANAGER', label: 'Manager' },
          ]}
        />
        <FormField label="Temporary Password" name="temporaryPassword" type="password" required value={form.temporaryPassword} onChange={(e) => setForm({ ...form, temporaryPassword: e.target.value })} />
        <div className="sm:col-span-4">
          <button type="submit" className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800">
            Create Staff Account
          </button>
        </div>
      </form>

      <div className="bg-white border border-slate-200 rounded-xl overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-600">
            <tr>
              <th className="py-3 px-4">Name</th>
              <th className="py-3 px-4">Email</th>
              <th className="py-3 px-4">Role</th>
              <th className="py-3 px-4">Status</th>
              <th className="py-3 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {users.map((u) => (
              <tr key={u.id}>
                <td className="py-3 px-4 font-semibold">{u.fullName}</td>
                <td className="py-3 px-4">{u.email}</td>
                <td className="py-3 px-4">{u.role}</td>
                <td className="py-3 px-4">{u.active ? 'Active' : 'Deactivated'}</td>
                <td className="py-3 px-4 text-right">
                  <button
                    type="button"
                    onClick={() => toggleStatus(u.id, !u.active)}
                    className={`font-semibold hover:underline ${u.active ? 'text-red-600' : 'text-emerald-700'}`}
                  >
                    {u.active ? 'Deactivate' : 'Activate'}
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
