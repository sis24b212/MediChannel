export default function StatusBadge({ status }) {
  const styles = {
    OPEN: 'text-emerald-700',
    SCHEDULED: 'text-slate-600',
    IN_PROGRESS: 'text-teal-700',
    IN_CONSULTATION: 'text-teal-700',
    ARRIVED: 'text-blue-700',
    BOOKED: 'text-slate-700',
    COMPLETED: 'text-emerald-700',
    PAID: 'text-emerald-700',
    PENDING: 'text-amber-700',
    CANCELLED: 'text-red-700',
    NO_SHOW: 'text-red-700',
  };

  return (
    <span className={`text-xs font-semibold tracking-tight ${styles[status] || 'text-slate-600'}`}>
      {String(status || '').replace('_', ' ')}
    </span>
  );
}
