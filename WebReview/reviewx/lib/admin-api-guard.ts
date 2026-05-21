import { NextResponse } from "next/server";
import { verifyAdminSessionToken } from "@/lib/admin-auth";

const requestHits = new Map<string, { count: number; resetAt: number }>();

function getClientIp(request: Request) {
  const forwarded = request.headers.get("x-forwarded-for");
  if (forwarded) return forwarded.split(",")[0]?.trim() ?? "unknown";
  return request.headers.get("x-real-ip") ?? "unknown";
}

export function requireAdminApiAccess(request: Request) {
  const cookieHeader = request.headers.get("cookie") ?? "";

  const cookieTokenMatch = cookieHeader.match(/reviewx_admin_session=([^;]+)/);
  const cookieRoleMatch = cookieHeader.match(/reviewx_admin_role=([^;]+)/);
  const cookieToken = cookieTokenMatch?.[1] ?? "";
  const cookieRole = cookieRoleMatch?.[1] ?? "";

  const isAuthorized = verifyAdminSessionToken(cookieToken);
  const hasAdminRole = cookieRole === "admin";

  if (!isAuthorized || !hasAdminRole) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const ip = getClientIp(request);
  const key = `${ip}:admin-api`;
  const now = Date.now();
  const windowMs = 60_000;
  const maxHits = 120;
  const current = requestHits.get(key);

  if (!current || current.resetAt <= now) {
    requestHits.set(key, { count: 1, resetAt: now + windowMs });
    return null;
  }

  if (current.count >= maxHits) {
    return NextResponse.json(
      { error: "Too Many Requests", hint: "Rate limit placeholder active" },
      { status: 429 },
    );
  }

  current.count += 1;
  requestHits.set(key, current);
  return null;
}
