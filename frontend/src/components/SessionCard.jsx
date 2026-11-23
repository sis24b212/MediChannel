import Link from 'next/link';
import StatusBadge from './StatusBadge';

export default function SessionCard({ session }) {
  const isFull = session.remainingNumbers <= 0;

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-5 flex flex-col justify-between gap-4">
      <div className="space-y-2">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h3 className="text-base font-semibold text-slate-900">{session.doctorName}</h3>
            <p className="text-xs text-slate-500">
              {session.specializationName} · {session.slmcRegNo}
            </p>
          </div>
          <StatusBadge status={session.status} />
        </div>

        <div className="grid grid-cols-2 gap-2 pt-2 text-xs text-slate-600 tabular-nums">
          <div>
            <span className="text-slate-400 block">Date & Time</span>
            <span className="font-medium text-slate-800">
              {session.sessionDate} · {String(session.startTime).slice(0, 5)}–{String(session.endTime).slice(0, 5)}
            </span>
          </div>
          <div>
            <span className="text-slate-400 block">Mode & Room</span>
            <span className="font-medium text-slate-800">
              {session.sessionType === 'ONLINE'
                ? 'Online Consultation'
                : `${session.roomNumber || 'TBA'} (${session.roomFloor || ''})`}
            </span>
          </div>
          <div>
            <span className="text-slate-400 block">Channelling Fee</span>
            <span className="font-semibold text-slate-900">LKR {Number(session.feeLkr).toLocaleString()}</span>
          </div>
          <div>
            <span className="text-slate-400 block">Numbers Left</span>
            <span className={`font-semibold ${isFull ? 'text-red-600' : 'text-teal-700'}`}>
              {session.remainingNumbers} of {session.maxPatients}
            </span>
          </div>
        </div>
      </div>

      <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
        <span className="text-xs text-slate-500 tabular-nums">
          {session.delayMinutes > 0 ? `Delayed +${session.delayMinutes} min` : 'On schedule'}
        </span>
        <Link
          href={`/sessions/${session.id}`}
          className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800 whitespace-nowrap"
        >
          {isFull ? 'View Session' : 'Book Appointment'}
        </Link>
      </div>
    </div>
  );
}
