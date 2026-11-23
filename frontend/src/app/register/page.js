'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import FormField from '../../components/FormField';
import { authAction } from '../../lib/api';
import { validateRegistration } from '../../lib/validators';

export default function RegisterPage() {
  const router = useRouter();
  const [form, setForm] = useState({
    fullName: '',
    nic: '',
    dateOfBirth: '',
    gender: 'MALE',
    phone: '',
    email: '',
    address: '',
    password: '',
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);

  function updateField(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setServerError('');
    const errs = validateRegistration(form);
    setFieldErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setLoading(true);
    try {
      await authAction('/api/auth/register', form);
      router.push('/patient');
      router.refresh();
    } catch (err) {
      setServerError(err.message);
      if (err.fieldErrors) setFieldErrors(err.fieldErrors);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-xl mx-auto bg-white border border-slate-200 rounded-xl p-6 sm:p-8 space-y-5">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Patient Registration</h1>
        <p className="text-xs text-slate-500 mt-1">
          All personal details are encrypted at rest with AES-256-GCM.
        </p>
      </div>

      {serverError && (
        <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-xs text-red-700">
          {serverError}
        </div>
      )}

      <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="sm:col-span-2">
          <FormField label="Full Name" name="fullName" required value={form.fullName} onChange={updateField} error={fieldErrors.fullName} />
        </div>
        <FormField label="Sri Lankan NIC" name="nic" required value={form.nic} onChange={updateField} error={fieldErrors.nic} placeholder="199512345678 or 985621478V" />
        <FormField label="Date of Birth" name="dateOfBirth" type="date" required value={form.dateOfBirth} onChange={updateField} error={fieldErrors.dateOfBirth} />
        <FormField
          label="Gender"
          name="gender"
          required
          value={form.gender}
          onChange={updateField}
          error={fieldErrors.gender}
          options={[
            { value: 'MALE', label: 'Male' },
            { value: 'FEMALE', label: 'Female' },
            { value: 'OTHER', label: 'Other' },
          ]}
        />
        <FormField label="Mobile Phone" name="phone" required value={form.phone} onChange={updateField} error={fieldErrors.phone} placeholder="0771234567" />
        <div className="sm:col-span-2">
          <FormField label="Email Address" name="email" type="email" required value={form.email} onChange={updateField} error={fieldErrors.email} />
        </div>
        <div className="sm:col-span-2">
          <FormField label="Home Address" name="address" required value={form.address} onChange={updateField} error={fieldErrors.address} />
        </div>
        <div className="sm:col-span-2">
          <FormField label="Password (8+ chars, letter, number, symbol)" name="password" type="password" required value={form.password} onChange={updateField} error={fieldErrors.password} />
        </div>

        <div className="sm:col-span-2 pt-2">
          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 text-sm font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800 disabled:opacity-50"
          >
            {loading ? 'Creating Account...' : 'Register Patient Account'}
          </button>
        </div>
      </form>

      <p className="text-xs text-slate-600 text-center">
        Already registered?{' '}
        <Link href="/login" className="font-semibold text-teal-700 hover:underline">
          Sign in here
        </Link>
      </p>
    </div>
  );
}
