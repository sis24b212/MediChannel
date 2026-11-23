import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

export const runtime = 'nodejs';

const API_BASE = process.env.API_BASE_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

/**
 * Authenticated API proxy route handler.
 * Reads the HTTP-only JWT cookie on the server and forwards it as an
 * Authorization: Bearer header to Spring Boot.
 */
async function proxyRequest(request, context) {
  const cookieStore = await cookies();
  const token = cookieStore.get('mc_token')?.value;
  const { path = [] } = await context.params;
  const url = new URL(request.url);
  const targetUrl = `${API_BASE}/${path.join('/')}${url.search}`;

  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const init = { method: request.method, headers };
  if (!['GET', 'HEAD'].includes(request.method)) {
    const text = await request.text();
    if (text) init.body = text;
  }

  try {
    const backendRes = await fetch(targetUrl, init);
    if (backendRes.status === 204) {
      return new NextResponse(null, { status: 204 });
    }
    const text = await backendRes.text();
    return new NextResponse(text, {
      status: backendRes.status,
      headers: {
        'Content-Type': backendRes.headers.get('content-type') || 'application/json',
        'Cache-Control': 'no-store',
      },
    });
  } catch {
    return NextResponse.json({ message: 'Unable to reach MediChannel API server.' }, { status: 502 });
  }
}

export const GET = proxyRequest;
export const POST = proxyRequest;
export const PUT = proxyRequest;
export const PATCH = proxyRequest;
export const DELETE = proxyRequest;
