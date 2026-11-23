'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import FormField from '../../components/FormField';
import { authFetch } from '../../lib/api';
import { PASSWORD_REGEX } from '../../lib/validators';

export default function ChangePasswordPage() {
  const router = useRouter();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '' });
  const [error, setError] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setFieldError('');
    if (!PASSWORD_REGEX.test(form.newPassword)) {
      setFieldError('Password needs 8+ characters with a letter, number, and symbol.');
      return;
    }
    setLoading(true);
    try {
      await authFetch('/api/auth/change-password', {
        method: 'POST',
        body: JSON.stringify(form),
      });
      router.push('/');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-md mx-auto bg-white border border-slate-200 rounded-xl p-6 space-y-4">
      <h1 className="text-lg font-bold text-slate-900">Update Temporary Password</h1>
      <p className="text-xs text-slate-500">
        For your security, please set a personal password before continuing.
      </p>
      {error && <p className="text-xs text-red-600 bg-red-50 p-3 rounded-lg">{error}</p>}
      <form onSubmit={handleSubmit} className="space-y-4">
        <FormField
          label="Temporary / Current Password"
          name="currentPassword"
          type="password"
          required
          value={form.currentPassword}
          onChange={(e) => setForm({ ...form, currentPassword: e.target.value })}
        />
        <FormField
          label="New Password"
          name="newPassword"
          type="password"
          required
          value={form.newPassword}
          onChange={(e) => setForm({ ...form, newPassword: e.target.value })}
          error={fieldError}
        />
        <button
          type="submit"
          disabled={loading}
          className="w-full py-2.5 text-sm font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800"
        >
          {loading ? 'Saving...' : 'Save New Password'}
        </button>
      </form>
    </div>
  );
}
