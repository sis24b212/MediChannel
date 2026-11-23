'use client';

export default function LiveQueueBanner({ currentServingNumber, myNumber, delayMinutes, status }) {
  const ahead = myNumber && currentServingNumber >= 0
    ? Math.max(0, myNumber - currentServingNumber - 1)
    : null;

  return (
    <div className="bg-slate-900 text-white rounded-xl p-5 flex flex-wrap items-center justify-between gap-4">
      <div>
        <p className="text-xs text-slate-400">Live Channelling Queue ({status})</p>
        <p className="text-2xl font-bold tabular-nums mt-1">
          Now Serving: No. {currentServingNumber || '—'}
        </p>
      </div>

      <div className="flex items-center gap-6 text-sm tabular-nums">
        {myNumber && (
          <div>
            <span className="block text-xs text-slate-400">Your Number</span>
            <span className="font-semibold text-teal-300 text-lg">No. {myNumber}</span>
          </div>
        )}
        {ahead !== null && (
          <div>
            <span className="block text-xs text-slate-400">Patients Ahead</span>
            <span className="font-semibold text-white text-lg">{ahead}</span>
          </div>
        )}
        {delayMinutes > 0 && (
          <div>
            <span className="block text-xs text-amber-300">Session Delay</span>
            <span className="font-semibold text-amber-300 text-lg">+{delayMinutes} min</span>
          </div>
        )}
      </div>
    </div>
  );
}
