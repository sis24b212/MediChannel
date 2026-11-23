
'use client';

import { useCallback, useEffect, useState } from 'react';
import { authFetch } from '../../lib/api';
import FormField from '../../components/FormField';
import { ErrorState, LoadingState } from '../../components/UiStates';

const INITIAL_FORM = {
  fullName: '',
  email: '',
  role: 'DOCTOR',
  temporaryPassword: '',
  specializationId: '',
  slmcRegNo: '',
};

export default function AdminDashboardPage() {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [specializations, setSpecializations] = useState([]);
  const [form, setForm] = useState(INITIAL_FORM);

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [processingId, setProcessingId] = useState(null);

  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const loadAll = useCallback(async (showLoading = true) => {
    if (showLoading) setLoading(true);

    try {
      const [dash, userPage, specs] = await Promise.all([
        authFetch('/api/admin/dashboard'),
        authFetch('/api/admin/users?page=0&size=50'),
        authFetch('/api/admin/specializations'),
      ]);

      setStats(dash ?? null);

      setUsers(
        Array.isArray(userPage?.content)
          ? userPage.content
          : []
      );

      setSpecializations(
        Array.isArray(specs) ? specs : []
      );

      setError('');
      return true;
    } catch (err) {
      setError(err?.message || 'Failed to load dashboard data.');
      return false;
    } finally {
      if (showLoading) setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  function updateForm(field, value) {
    setForm((prev) => ({
      ...prev,
      [field]: value,
      ...(field === 'role'
        ? { specializationId: '', slmcRegNo: '' }
        : {}),
    }));
  }

  async function createStaff(e) {
    e.preventDefault();

    if (submitting) return;

    setError('');
    setMessage('');

    if (!form.fullName.trim() || !form.email.trim()) {
      setError('Full name and email are required.');
      return;
    }

    if (!form.temporaryPassword.trim()) {
      setError('Temporary password is required.');
      return;
    }

    if (
      form.role === 'DOCTOR' &&
      (!form.specializationId || !form.slmcRegNo.trim())
    ) {
      setError(
        'Please select a specialization and enter the SLMC registration number.'
      );
      return;
    }

    setSubmitting(true);

    try {
      const payload = {
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        role: form.role,
        temporaryPassword: form.temporaryPassword,
        ...(form.role === 'DOCTOR'
          ? {
              specializationId: form.specializationId,
              slmcRegNo: form.slmcRegNo.trim(),
            }
          : {}),
      };

      await authFetch('/api/admin/staff', {
        method: 'POST',
        body: JSON.stringify(payload),
      });

      const createdRole = form.role;

      setForm({ ...INITIAL_FORM });

      await loadAll(false);

      setMessage(
        `${createdRole} account created successfully. ` +
        'The user must change their temporary password on first login.'
      );
    } catch (err) {
      setError(err?.message || 'Failed to create staff account.');
    } finally {
      setSubmitting(false);
    }
  }

  async function toggleStatus(id, active) {
    if (processingId !== null) return;

    const confirmed = window.confirm(
      `Are you sure you want to ${
        active ? 'activate' : 'deactivate'
      } this user?`
    );

    if (!confirmed) return;

    setError('');
    setMessage('');
    setProcessingId(id);

    try {
      await authFetch(`/api/admin/users/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ active }),
      });

      await loadAll(false);

      setMessage(
        `User ${active ? 'activated' : 'deactivated'} successfully.`
      );
    } catch (err) {
      setError(err?.message || 'Failed to update user status.');
    } finally {
      setProcessingId(null);
    }
  }

  async function resetPassword(user) {
    if (processingId !== null) return;

    const tempPassword = window.prompt(
      `Enter a new temporary password for ${user.fullName}:`
    );

    if (tempPassword === null) return;

    if (!tempPassword.trim()) {
      setError('Temporary password cannot be empty.');
      return;
    }

    setError('');
    setMessage('');
    setProcessingId(user.id);

    try {
      await authFetch(
        `/api/admin/users/${user.id}/reset-password`,
        {
          method: 'POST',
          body: JSON.stringify({
            temporaryPassword: tempPassword,
          }),
        }
      );

      setMessage(
        `Password reset successfully for ${user.fullName}. ` +
        'The user must change their password on next login.'
      );
    } catch (err) {
      setError(err?.message || 'Failed to reset password.');
    } finally {
      setProcessingId(null);
    }
  }

  if (loading) {
    return (
      <LoadingState message="Loading IT Admin dashboard..." />
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-xl font-bold text-slate-900">
          IT Administration & User Accounts
        </h1>

        <p className="text-xs text-slate-500">
          Create DOCTOR and MANAGER accounts, manage user
          status, and reset staff passwords.
        </p>
      </div>

      {/* Notifications */}
      {error && <ErrorState message={error} />}

      {message && (
        <p
          role="status"
          className="text-xs text-emerald-700 bg-emerald-50 p-3 rounded-lg"
        >
          {message}
        </p>
      )}

      {/* Dashboard Statistics */}
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 tabular-nums">
          {[
            { label: 'Total Users', value: stats.totalUsers },
            { label: 'Doctors', value: stats.doctors },
            { label: 'Managers', value: stats.managers },
            {
              label: 'Audit Events',
              value: stats.totalAuditEvents,
            },
          ].map((item) => (
            <div
              key={item.label}
              className="bg-white border border-slate-200 rounded-xl p-4"
            >
              <span className="text-xs text-slate-500">
                {item.label}
              </span>

              <p className="text-xl font-bold">
                {item.value ?? 0}
              </p>
            </div>
          ))}
        </div>
      )}

      {/* Create Staff Form */}
      <form
        onSubmit={createStaff}
        className="bg-white border border-slate-200 rounded-xl p-5 grid grid-cols-1 sm:grid-cols-4 gap-3 items-end"
      >
        <div className="sm:col-span-4">
          <h2 className="text-sm font-bold text-slate-900">
            Create Staff Account
          </h2>
        </div>

        <FormField
          label="Full Name"
          name="fullName"
          required
          value={form.fullName}
          onChange={(e) =>
            updateForm('fullName', e.target.value)
          }
        />

        <FormField
          label="Email"
          name="email"
          type="email"
          required
          value={form.email}
          onChange={(e) =>
            updateForm('email', e.target.value)
          }
        />

        <FormField
          label="Staff Role"
          name="role"
          value={form.role}
          onChange={(e) =>
            updateForm('role', e.target.value)
          }
          options={[
            { value: 'DOCTOR', label: 'Doctor' },
            { value: 'MANAGER', label: 'Manager' },
          ]}
        />

        <FormField
          label="Temporary Password"
          name="temporaryPassword"
          type="password"
          required
          value={form.temporaryPassword}
          onChange={(e) =>
            updateForm('temporaryPassword', e.target.value)
          }
        />

        {form.role === 'DOCTOR' && (
          <>
            <FormField
              label="Specialization"
              name="specializationId"
              required
              value={form.specializationId}
              onChange={(e) =>
                updateForm('specializationId', e.target.value)
              }
              options={[
                {
                  value: '',
                  label: 'Select Specialization',
                },
                ...specializations.map((s) => ({
                  value: String(s.id),
                  label: s.name,
                })),
              ]}
            />

            <FormField
              label="SLMC Reg. Number"
              name="slmcRegNo"
              placeholder="e.g., SLMC-24810"
              required
              value={form.slmcRegNo}
              onChange={(e) =>
                updateForm('slmcRegNo', e.target.value)
              }
            />
          </>
        )}

        <div className="sm:col-span-4">
          <button
            type="submit"
            disabled={submitting}
            className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {submitting
              ? 'Creating Account...'
              : 'Create Staff Account'}
          </button>
        </div>
      </form>

      {/* User Management Table */}
      <div className="bg-white border border-slate-200 rounded-xl overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-600">
            <tr>
              <th className="py-3 px-4">Name</th>
              <th className="py-3 px-4">Email</th>
              <th className="py-3 px-4">Role</th>
              <th className="py-3 px-4">Status</th>
              <th className="py-3 px-4 text-right">
                Actions
              </th>
            </tr>
          </thead>

          <tbody className="divide-y divide-slate-100">
            {users.length === 0 ? (
              <tr>
                <td
                  colSpan={5}
                  className="py-8 text-center text-slate-500"
                >
                  No users found.
                </td>
              </tr>
            ) : (
              users.map((u) => (
                <tr key={u.id}>
                  <td className="py-3 px-4 font-semibold">
                    {u.fullName}
                  </td>

                  <td className="py-3 px-4">
                    {u.email}
                  </td>

                  <td className="py-3 px-4">
                    {u.role}
                  </td>

                  <td className="py-3 px-4">
                    <span
                      className={
                        u.active
                          ? 'text-emerald-700 font-semibold'
                          : 'text-red-600 font-semibold'
                      }
                    >
                      {u.active
                        ? 'Active'
                        : 'Deactivated'}
                    </span>
                  </td>

                  <td className="py-3 px-4 text-right">
                    <div className="flex justify-end gap-3">
                      <button
                        type="button"
                        disabled={processingId !== null}
                        onClick={() =>
                          toggleStatus(u.id, !u.active)
                        }
                        className={`font-semibold hover:underline disabled:opacity-50 ${
                          u.active
                            ? 'text-red-600'
                            : 'text-emerald-700'
                        }`}
                      >
                        {processingId === u.id
                          ? 'Processing...'
                          : u.active
                            ? 'Deactivate'
                            : 'Activate'}
                      </button>

                      {['DOCTOR', 'MANAGER'].includes(u.role) && (
                        <button
                          type="button"
                          disabled={processingId !== null}
                          onClick={() => resetPassword(u)}
                          className="font-semibold text-blue-700 hover:underline disabled:opacity-50"
                        >
                          Reset Password
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
