import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

export const runtime = 'nodejs';

/**
 * Returns authenticated user summary and ephemeral token for STOMP WebSocket CONNECT header.
 */
export async function GET() {
  const cookieStore = await cookies();
  const token = cookieStore.get('mc_token')?.value;
  const userCookie = cookieStore.get('mc_user')?.value;

  if (!token || !userCookie) {
    return NextResponse.json(
      { authenticated: false, user: null },
      { status: 200, headers: { 'Cache-Control': 'no-store' } }
    );
  }

  try {
    const user = JSON.parse(userCookie);
    return NextResponse.json(
      { authenticated: true, user, wsToken: token },
      { status: 200, headers: { 'Cache-Control': 'no-store' } }
    );
  } catch {
    return NextResponse.json({ authenticated: false, user: null }, { status: 200 });
  }
}
