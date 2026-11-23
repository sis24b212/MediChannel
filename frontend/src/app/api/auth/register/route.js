import { NextResponse } from 'next/server';

export const runtime = 'nodejs';

const API_BASE = process.env.API_BASE_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export async function POST(request) {
  try {
    const body = await request.json();
    const backendRes = await fetch(`${API_BASE}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });

    const data = await backendRes.json();
    if (!backendRes.ok) {
      return NextResponse.json(data, { status: backendRes.status });
    }

    const isProd = process.env.NODE_ENV === 'production';
    const response = NextResponse.json(
      {
        userId: data.userId,
        fullName: data.fullName,
        email: data.email,
        role: data.role,
        mustChangePassword: false,
      },
      { status: 201, headers: { 'Cache-Control': 'no-store' } }
    );

    response.cookies.set('mc_token', data.token, {
      httpOnly: true,
      secure: isProd,
      sameSite: 'lax',
      path: '/',
      maxAge: 60 * 60 * 24,
    });

    response.cookies.set('mc_role', data.role, {
      httpOnly: false,
      secure: isProd,
      sameSite: 'lax',
      path: '/',
      maxAge: 60 * 60 * 24,
    });

    response.cookies.set('mc_user', JSON.stringify({
      userId: data.userId,
      fullName: data.fullName,
      role: data.role,
      mustChangePassword: false,
    }), {
      httpOnly: false,
      secure: isProd,
      sameSite: 'lax',
      path: '/',
      maxAge: 60 * 60 * 24,
    });

    return response;
  } catch {
    return NextResponse.json({ message: 'Unable to reach registration server.' }, { status: 502 });
  }
}
