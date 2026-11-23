'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { authFetch } from '../../../../lib/api';
import ConsultationChat from '../../../../components/ConsultationChat';
import FormField from '../../../../components/FormField';
import { ErrorState, LoadingState } from '../../../../components/UiStates';

export default function PatientConsultationPage() {
  const { appointmentId } = useParams();
  const [record, setRecord] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [feedbackSaved, setFeedbackSaved] = useState(false);

  async function loadRecord() {
    setLoading(true);
    setError('');
    try {
      const data = await authFetch(`/api/consultations/${appointmentId}`);
      setRecord(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadRecord();
  }, [appointmentId]);

  async function handleFeedback(e) {
    e.preventDefault();
    try {
      await authFetch('/api/patient/feedback', {
        method: 'POST',
        body: JSON.stringify({ appointmentId: Number(appointmentId), rating: Number(rating), comment }),
      });
      setFeedbackSaved(true);
    } catch (err) {
      setError(err.message);
    }
  }

  if (loading) return <LoadingState message="Opening private consultation record..." />;
  if (error && !record) return <ErrorState message={error} onRetry={loadRecord} />;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Consultation Summary & Online Room</h1>
          <p className="text-xs text-slate-500">
            {record.doctorName} ({record.specialization}) · Ref {record.bookingReference} · {record.sessionDate}
          </p>
        </div>
        <button
          type="button"
          onClick={() => window.print()}
          className="no-print px-4 py-2 text-xs font-semibold text-slate-800 border border-slate-300 rounded-lg hover:bg-slate-50"
        >
          Print Summary & Prescription
        </button>
      </div>

      {/* Printable Consultation Summary & Prescription */}
      <div className="bg-white border border-slate-200 rounded-xl p-6 space-y-4">
        <div className="border-b border-slate-200 pb-3 flex justify-between text-xs text-slate-600">
          <div>
            <p className="font-bold text-slate-900 text-sm">Lanka Care Hospital, Colombo</p>
            <p>Consultant: {record.doctorName} ({record.doctorSlmc})</p>
          </div>
          <div className="text-right">
            <p>Patient: {record.patientName}</p>
            <p>Date: {record.sessionDate}</p>
          </div>
        </div>

        <div className="space-y-3 text-sm">
          <div>
            <h2 className="text-xs font-semibold text-slate-500">Reason for Visit</h2>
            <p className="mt-0.5 text-slate-900">{record.reasonForVisit}</p>
          </div>
          <div>
            <h2 className="text-xs font-semibold text-slate-500">Diagnosis Summary</h2>
            <p className="mt-0.5 text-slate-900 whitespace-pre-wrap">{record.diagnosis || 'Pending consultant notes.'}</p>
          </div>
          <div>
            <h2 className="text-xs font-semibold text-slate-500">Medical Advice</h2>
            <p className="mt-0.5 text-slate-900 whitespace-pre-wrap">{record.advice || '—'}</p>
          </div>
          <div>
            <h2 className="text-xs font-semibold text-slate-500">Prescription</h2>
            <pre className="mt-1 p-3 bg-slate-50 border border-slate-200 rounded-lg font-mono text-xs whitespace-pre-wrap">
              {record.prescription || 'No prescription issued yet.'}
            </pre>
          </div>
        </div>
      </div>

      {record.sessionType === 'ONLINE' && (
        <div className="no-print">
          <ConsultationChat
            appointmentId={Number(appointmentId)}
            initialMessages={record.messages || []}
            roomOpen={record.roomOpen}
            currentUserRole="CUSTOMER"
          />
        </div>
      )}

      {record.appointmentStatus === 'COMPLETED' && (
        <div className="no-print bg-white border border-slate-200 rounded-xl p-6 space-y-4">
          <h2 className="text-base font-semibold text-slate-900">Rate Your Consultation</h2>
          {feedbackSaved ? (
            <p className="text-sm text-emerald-700 font-medium">Thank you! Your feedback has been submitted for approval.</p>
          ) : (
            <form onSubmit={handleFeedback} className="space-y-3">
              <FormField
                label="Rating (1 to 5)"
                name="rating"
                value={rating}
                onChange={(e) => setRating(e.target.value)}
                options={[5, 4, 3, 2, 1].map((n) => ({ value: n, label: `${n} Stars` }))}
              />
              <FormField
                label="Comment"
                name="comment"
                rows={3}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Share your experience with the doctor and channelling process..."
              />
              <button type="submit" className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg">
                Submit Feedback
              </button>
            </form>
          )}
        </div>
      )}
    </div>
  );
}
