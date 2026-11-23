'use client';

import { useEffect, useState } from 'react';
import { authFetch } from '../../../lib/api';
import FormField from '../../../components/FormField';
import { ErrorState, LoadingState } from '../../../components/UiStates';
import { PHONE_REGEX } from '../../../lib/validators';

export default function PatientProfilePage() {
  const [profile, setProfile] = useState(null);
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    authFetch('/api/patient/profile')
      .then((data) => {
        setProfile(data);
        setPhone(data.phone || '');
        setAddress(data.address || '');
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  async function handleSave(e) {
    e.preventDefault();
    setError('');
    setMessage('');
    if (!PHONE_REGEX.test(phone.trim())) {
      setError('Phone must follow Sri Lankan format 07XXXXXXXX.');
      return;
    }
    try {
      const updated = await authFetch('/api/patient/profile', {
        method: 'PUT',
        body: JSON.stringify({ phone, address }),
      });
      setProfile(updated);
      setMessage('Profile updated securely.');
    } catch (err) {
      setError(err.message);
    }
  }

  if (loading) return <LoadingState message="Decrypting your profile..." />;
  if (error && !profile) return <ErrorState message={error} />;

  return (
    <div className="max-w-xl mx-auto bg-white border border-slate-200 rounded-xl p-6 space-y-5">
      <div>
        <h1 className="text-xl font-bold text-slate-900">My Patient Profile</h1>
        <p className="text-xs text-slate-500">Stored encrypted in PostgreSQL with AES-256-GCM.</p>
      </div>

      {message && <p className="text-xs text-emerald-700 bg-emerald-50 p-3 rounded-lg">{message}</p>}
      {error && <p className="text-xs text-red-600 bg-red-50 p-3 rounded-lg">{error}</p>}

      <div className="grid grid-cols-2 gap-3 text-sm bg-slate-50 p-4 rounded-lg">
        <div><span className="text-xs text-slate-400 block">Full Name</span><span className="font-medium">{profile.fullName}</span></div>
        <div><span className="text-xs text-slate-400 block">NIC</span><span className="font-mono font-medium">{profile.nic}</span></div>
        <div><span className="text-xs text-slate-400 block">Email</span><span className="font-medium">{profile.email}</span></div>
        <div><span className="text-xs text-slate-400 block">Date of Birth</span><span className="font-medium">{profile.dateOfBirth}</span></div>
      </div>

      <form onSubmit={handleSave} className="space-y-4">
        <FormField label="Mobile Phone (07XXXXXXXX)" name="phone" value={phone} onChange={(e) => setPhone(e.target.value)} required />
        <FormField label="Home Address" name="address" value={address} onChange={(e) => setAddress(e.target.value)} required />
        <button type="submit" className="px-5 py-2 text-sm font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800">
          Save Changes
        </button>
      </form>
    </div>
  );
}
