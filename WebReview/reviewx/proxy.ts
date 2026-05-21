import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { verifyAdminSessionToken } from "@/lib/admin-auth";

export function proxy(request: NextRequest) {
  if (request.nextUrl.pathname === "/admin/login") {
    return NextResponse.next();
  }

  const adminToken = request.cookies.get("reviewx_admin_session")?.value;
  const adminRole = request.cookies.get("reviewx_admin_role")?.value;
  const isAuthorized = verifyAdminSessionToken(adminToken);
  const hasAdminRole = adminRole === "admin";

  if (!isAuthorized || !hasAdminRole) {
    const url = request.nextUrl.clone();
    url.pathname = "/admin/login";
    url.searchParams.set("from", request.nextUrl.pathname);
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*"],
};
