'use client';

import { useEffect, useState } from 'react';
import { authFetch } from '../../../lib/api';
import { ErrorState, LoadingState } from '../../../components/UiStates';

export default function AdminAuditLogsPage() {
  const [logs, setLogs] = useState([]);
  const [filter, setFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadLogs(actionFilter = '') {
    setLoading(true);
    setError('');
    try {
      const q = actionFilter ? `&action=${encodeURIComponent(actionFilter)}` : '';
      const page = await authFetch(`/api/admin/audit-logs?page=0&size=50${q}`);
      setLogs(page?.content || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadLogs();
  }, []);

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Immutable Security Audit Log</h1>
          <p className="text-xs text-slate-500">Tracks logins, staff changes, session cancellations, and consultation record openings. Zero medical data stored.</p>
        </div>
        <div className="flex items-center gap-2">
          <input
            type="text"
            placeholder="Filter by action..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs"
          />
          <button
            type="button"
            onClick={() => loadLogs(filter)}
            className="px-3.5 py-1.5 text-xs font-semibold text-white bg-teal-700 rounded-lg"
          >
            Filter
          </button>
        </div>
      </div>

      {error && <ErrorState message={error} />}
      {loading ? (
        <LoadingState message="Loading audit trail..." />
      ) : (
        <div className="bg-white border border-slate-200 rounded-xl overflow-x-auto">
          <table className="w-full text-left text-xs tabular-nums">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-600">
              <tr>
                <th className="py-3 px-4">Timestamp</th>
                <th className="py-3 px-4">Role</th>
                <th className="py-3 px-4">Action</th>
                <th className="py-3 px-4">Resource</th>
                <th className="py-3 px-4">IP Address</th>
                <th className="py-3 px-4">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {logs.map((l) => (
                <tr key={l.id}>
                  <td className="py-2.5 px-4 font-mono">{String(l.createdAt).replace('T', ' ').slice(0, 19)}</td>
                  <td className="py-2.5 px-4 font-semibold">{l.userRole}</td>
                  <td className="py-2.5 px-4 font-mono text-teal-800">{l.action}</td>
                  <td className="py-2.5 px-4">{l.resourceType} #{l.resourceId}</td>
                  <td className="py-2.5 px-4 font-mono">{l.ipAddress}</td>
                  <td className="py-2.5 px-4 text-slate-600">{l.details}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
