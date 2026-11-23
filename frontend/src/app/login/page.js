'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import FormField from '../../components/FormField';
import { authAction } from '../../lib/api';
import { validateLogin } from '../../lib/validators';

export default function LoginPage() {
  const router = useRouter();
  const [form, setForm] = useState({ email: '', password: '' });
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setServerError('');
    const errs = validateLogin(form);
    setFieldErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setLoading(true);
    try {
      const user = await authAction('/api/auth/login', form);
      if (user.mustChangePassword) {
        router.push('/change-password');
        return;
      }
      const roleRoutes = {
        CUSTOMER: '/patient',
        DOCTOR: '/doctor',
        MANAGER: '/manager',
        ADMIN: '/admin',
      };
      router.push(roleRoutes[user.role] || '/');
      router.refresh();
    } catch (err) {
      setServerError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-md mx-auto bg-white border border-slate-200 rounded-xl p-6 sm:p-8 space-y-5">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Sign In to MediChannel</h1>
        <p className="text-xs text-slate-500 mt-1">Lanka Care Hospital Secure Portal</p>
      </div>

      {serverError && (
        <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-xs text-red-700">
          {serverError}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <FormField
          label="Email Address"
          name="email"
          type="email"
          required
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          error={fieldErrors.email}
          placeholder="you@example.lk"
        />
        <FormField
          label="Password"
          name="password"
          type="password"
          required
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
          error={fieldErrors.password}
        />

        <button
          type="submit"
          disabled={loading}
          className="w-full py-2.5 text-sm font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800 disabled:opacity-50"
        >
          {loading ? 'Signing in...' : 'Sign In'}
        </button>
      </form>

      <p className="text-xs text-slate-600 text-center">
        New patient?{' '}
        <Link href="/register" className="font-semibold text-teal-700 hover:underline">
          Create a patient account
        </Link>
      </p>
    </div>
  );
}
