import { NextResponse } from 'next/server';

const ROLE_PREFIX_MAP = {
  '/patient': 'CUSTOMER',
  '/doctor': 'DOCTOR',
  '/manager': 'MANAGER',
  '/admin': 'ADMIN',
};

/**
 * Route protection middleware (Point 7 Frontend):
 * Ensures protected role dashboards require a valid session cookie and matching role.
 */
export function middleware(request) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get('mc_token')?.value;
  const role = request.cookies.get('mc_role')?.value;

  for (const [prefix, requiredRole] of Object.entries(ROLE_PREFIX_MAP)) {
    if (pathname.startsWith(prefix)) {
      if (!token) {
        const loginUrl = new URL('/login', request.url);
        loginUrl.searchParams.set('redirect', pathname);
        return NextResponse.redirect(loginUrl);
      }
      if (role && role !== requiredRole) {
        return NextResponse.redirect(new URL('/', request.url));
      }
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/patient/:path*', '/doctor/:path*', '/manager/:path*', '/admin/:path*'],
};
