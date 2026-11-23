import { NextResponse } from 'next/server';

export const runtime = 'nodejs';

export async function POST() {
  const response = NextResponse.json({ message: 'Logged out' }, { status: 200 });
  response.cookies.delete('mc_token');
  response.cookies.delete('mc_role');
  response.cookies.delete('mc_user');
  return response;
}
