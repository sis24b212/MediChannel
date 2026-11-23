'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { authFetch } from '../../../../lib/api';
import ConsultationChat from '../../../../components/ConsultationChat';
import FormField from '../../../../components/FormField';
import { ErrorState, LoadingState } from '../../../../components/UiStates';

export default function DoctorConsultationPage() {
  const { appointmentId } = useParams();
  const router = useRouter();
  const [record, setRecord] = useState(null);
  const [diagnosis, setDiagnosis] = useState('');
  const [advice, setAdvice] = useState('');
  const [prescription, setPrescription] = useState('');
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    authFetch(`/api/consultations/${appointmentId}`)
      .then((data) => {
        setRecord(data);
        setDiagnosis(data.diagnosis || '');
        setAdvice(data.advice || '');
        setPrescription(data.prescription || '');
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [appointmentId]);

  async function saveNotes(endConsultation) {
    setError('');
    setMessage('');
    if (!diagnosis.trim()) {
      setError('Diagnosis summary is required.');
      return;
    }
    try {
      await authFetch(`/api/consultations/${appointmentId}/notes?endConsultation=${endConsultation}`, {
        method: 'PUT',
        body: JSON.stringify({ diagnosis, advice, prescription }),
      });
      setMessage(endConsultation ? 'Consultation ended and notes encrypted & saved.' : 'Draft notes saved securely.');
      if (endConsultation) {
        setTimeout(() => router.push('/doctor'), 900);
      }
    } catch (err) {
      setError(err.message);
    }
  }

  if (loading) return <LoadingState message="Opening encrypted consultation room..." />;
  if (error && !record) return <ErrorState message={error} />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">Consultation Room — {record.patientName}</h1>
        <p className="text-xs text-slate-500">
          Ref {record.bookingReference} · Mode: {record.sessionType} · Reason: {record.reasonForVisit}
        </p>
      </div>

      {message && <p className="text-xs text-emerald-700 bg-emerald-50 p-3 rounded-lg">{message}</p>}
      {error && <p className="text-xs text-red-600 bg-red-50 p-3 rounded-lg">{error}</p>}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {record.sessionType === 'ONLINE' && (
          <ConsultationChat
            appointmentId={Number(appointmentId)}
            initialMessages={record.messages || []}
            roomOpen={record.roomOpen}
            currentUserRole="DOCTOR"
          />
        )}

        <div className="bg-white border border-slate-200 rounded-xl p-5 space-y-4">
          <h2 className="text-base font-semibold text-slate-900">Clinical Notes & Prescription (AES-256-GCM Encrypted)</h2>
          <FormField label="Diagnosis Summary" name="diagnosis" rows={3} required value={diagnosis} onChange={(e) => setDiagnosis(e.target.value)} />
          <FormField label="Medical Advice" name="advice" rows={3} value={advice} onChange={(e) => setAdvice(e.target.value)} />
          <FormField label="Prescription" name="prescription" rows={4} value={prescription} onChange={(e) => setPrescription(e.target.value)} placeholder="1. Medicine name — dosage & duration" />

          <div className="flex flex-wrap items-center gap-3 pt-2">
            <button
              type="button"
              onClick={() => saveNotes(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
            >
              Save Draft Notes
            </button>
            <button
              type="button"
              onClick={() => saveNotes(true)}
              className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg hover:bg-teal-800"
            >
              Save & End Consultation
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
