'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import NotificationBell from './NotificationBell';

export default function Navbar() {
  const [session, setSession] = useState({ authenticated: false, user: null, wsToken: null });
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    fetch('/api/auth/me', { cache: 'no-store' })
      .then((r) => r.json())
      .then((data) => setSession(data))
      .catch(() => setSession({ authenticated: false, user: null, wsToken: null }));
  }, [pathname]);

  async function handleLogout() {
    await fetch('/api/auth/logout', { method: 'POST' });
    setSession({ authenticated: false, user: null, wsToken: null });
    router.push('/login');
    router.refresh();
  }

  const role = session.user?.role;

  return (
    <header className="no-print bg-white border-b border-slate-200 sticky top-0 z-30">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-6">
        <Link href="/" className="text-lg font-bold tracking-tight text-slate-900 whitespace-nowrap shrink-0">
          MediChannel
        </Link>

        <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-slate-600">
          <Link href="/" className="hover:text-teal-700 whitespace-nowrap">
            Find Doctors
          </Link>
          {role === 'CUSTOMER' && (
            <>
              <Link href="/patient" className="hover:text-teal-700 whitespace-nowrap">
                My Appointments
              </Link>
              <Link href="/patient/profile" className="hover:text-teal-700 whitespace-nowrap">
                Profile
              </Link>
            </>
          )}
          {role === 'DOCTOR' && (
            <Link href="/doctor" className="hover:text-teal-700 whitespace-nowrap">
              Doctor Console
            </Link>
          )}
          {role === 'MANAGER' && (
            <>
              <Link href="/manager" className="hover:text-teal-700 whitespace-nowrap">
                Dashboard
              </Link>
              <Link href="/manager/sessions" className="hover:text-teal-700 whitespace-nowrap">
                Sessions
              </Link>
              <Link href="/manager/reception" className="hover:text-teal-700 whitespace-nowrap">
                Reception
              </Link>
              <Link href="/manager/catalog" className="hover:text-teal-700 whitespace-nowrap">
                Catalog
              </Link>
              <Link href="/manager/notices-feedback" className="hover:text-teal-700 whitespace-nowrap">
                Notices
              </Link>
            </>
          )}
          {role === 'ADMIN' && (
            <>
              <Link href="/admin" className="hover:text-teal-700 whitespace-nowrap">
                Users
              </Link>
              <Link href="/admin/audit-logs" className="hover:text-teal-700 whitespace-nowrap">
                Audit Log
              </Link>
            </>
          )}
        </nav>

        <div className="flex items-center gap-3 shrink-0">
          {session.authenticated ? (
            <>
              <NotificationBell wsToken={session.wsToken} />
              <span className="hidden sm:inline text-xs text-slate-600">
                {session.user.fullName} ({session.user.role})
              </span>
              <button
                type="button"
                onClick={handleLogout}
                className="px-3.5 py-2 text-xs font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50 whitespace-nowrap"
              >
                Sign Out
              </button>
            </>
          ) : (
            <>
              <Link
                href="/login"
                className="text-sm font-medium text-slate-700 hover:text-slate-900 whitespace-nowrap"
              >
                Sign In
              </Link>
              <Link
                href="/register"
                className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800 whitespace-nowrap"
              >
                Register
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
