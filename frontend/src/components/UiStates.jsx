export function LoadingState({ message = 'Loading data...' }) {
  return (
    <div className="bg-white border border-slate-200 rounded-xl p-8 text-center text-sm text-slate-500">
      {message}
    </div>
  );
}

export function EmptyState({ title = 'No records found', description = 'Try adjusting your filters or check back later.' }) {
  return (
    <div className="bg-white border border-slate-200 rounded-xl p-8 text-center space-y-1">
      <p className="text-sm font-semibold text-slate-800">{title}</p>
      <p className="text-xs text-slate-500">{description}</p>
    </div>
  );
}

export function ErrorState({ message = 'Something went wrong.', onRetry }) {
  return (
    <div className="bg-red-50 border border-red-200 rounded-xl p-5 flex items-center justify-between gap-4">
      <p className="text-sm text-red-700">{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="px-3 py-1.5 text-xs font-semibold text-red-800 border border-red-300 rounded-lg hover:bg-red-100"
        >
          Retry
        </button>
      )}
    </div>
  );
}
