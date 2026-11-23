/**
 * Single centralized API helper for MediChannel Next.js frontend (Point 7).
 * - Public GET endpoints call NEXT_PUBLIC_API_URL directly.
 * - Authenticated requests go through /api/proxy/* so the HTTP-only JWT cookie
 *   is attached server-side by Next.js Route Handlers (never exposed to localStorage).
 */

const PUBLIC_API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export async function publicFetch(path, options = {}) {
  const res = await fetch(`${PUBLIC_API_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  });
  return parseJsonResponse(res);
}

export async function authFetch(path, options = {}) {
  const cleanPath = path.startsWith('/') ? path.slice(1) : path;
  const res = await fetch(`/api/proxy/${cleanPath}`, {
    ...options,
    cache: 'no-store',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  });
  return parseJsonResponse(res);
}

export async function authAction(endpoint, body) {
  const res = await fetch(endpoint, {
    method: 'POST',
    cache: 'no-store',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return parseJsonResponse(res);
}

async function parseJsonResponse(res) {
  if (res.status === 204) {
    return null;
  }
  let data = null;
  try {
    data = await res.json();
  } catch {
    data = null;
  }
  if (!res.ok) {
    const err = new Error(data?.message || `Request failed (${res.status})`);
    err.status = res.status;
    err.fieldErrors = data?.fieldErrors || {};
    throw err;
  }
  return data;
}
