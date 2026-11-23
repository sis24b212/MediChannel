'use client';

import { useEffect, useState } from 'react';
import { authFetch, publicFetch } from '../../../lib/api';

export default function ManagerCatalogPage() {
  const [specs, setSpecs] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [specForm, setSpecForm] = useState({ name: '', description: '' });
  const [roomForm, setRoomForm] = useState({ roomNumber: '', floor: '', wing: '' });

  async function load() {
    const [sp, rm] = await Promise.all([
      publicFetch('/api/public/specializations'),
      authFetch('/api/manager/rooms'),
    ]);
    setSpecs(sp || []);
    setRooms(rm || []);
  }

  useEffect(() => {
    load();
  }, []);

  async function addSpec(e) {
    e.preventDefault();
    await authFetch('/api/manager/specializations', { method: 'POST', body: JSON.stringify(specForm) });
    setSpecForm({ name: '', description: '' });
    await load();
  }

  async function addRoom(e) {
    e.preventDefault();
    await authFetch('/api/manager/rooms', { method: 'POST', body: JSON.stringify(roomForm) });
    setRoomForm({ roomNumber: '', floor: '', wing: '' });
    await load();
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div className="bg-white border border-slate-200 rounded-xl p-5 space-y-4">
        <h2 className="text-base font-semibold text-slate-900">Specializations</h2>
        <form onSubmit={addSpec} className="space-y-2">
          <input required placeholder="Specialization name" value={specForm.name} onChange={(e) => setSpecForm({ ...specForm, name: e.target.value })} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <input placeholder="Description" value={specForm.description} onChange={(e) => setSpecForm({ ...specForm, description: e.target.value })} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <button type="submit" className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg">Add Specialization</button>
        </form>
        <ul className="divide-y divide-slate-100 text-xs">
          {specs.map((s) => (
            <li key={s.id} className="py-2 font-medium text-slate-800">{s.name}</li>
          ))}
        </ul>
      </div>

      <div className="bg-white border border-slate-200 rounded-xl p-5 space-y-4">
        <h2 className="text-base font-semibold text-slate-900">Channelling Rooms</h2>
        <form onSubmit={addRoom} className="space-y-2">
          <input required placeholder="Room Number (e.g., Room 302)" value={roomForm.roomNumber} onChange={(e) => setRoomForm({ ...roomForm, roomNumber: e.target.value })} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <input required placeholder="Floor (e.g., 3rd Floor)" value={roomForm.floor} onChange={(e) => setRoomForm({ ...roomForm, floor: e.target.value })} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <input placeholder="Wing" value={roomForm.wing} onChange={(e) => setRoomForm({ ...roomForm, wing: e.target.value })} className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <button type="submit" className="px-4 py-2 text-xs font-semibold text-white bg-teal-700 rounded-lg">Add Room</button>
        </form>
        <ul className="divide-y divide-slate-100 text-xs">
          {rooms.map((r) => (
            <li key={r.id} className="py-2 font-medium text-slate-800">{r.roomNumber} — {r.floor} {r.wing}</li>
          ))}
        </ul>
      </div>
    </div>
  );
}
