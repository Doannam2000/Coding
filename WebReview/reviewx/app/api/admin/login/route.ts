import { createHash, timingSafeEqual } from "node:crypto";
import { NextResponse } from "next/server";
import { createAdminSessionToken } from "@/lib/admin-auth";

type LoginBody = { username?: string; password?: string };

type RateLimitState = {
  count: number;
  firstAttemptAt: number;
};

const MAX_ATTEMPTS = 5;
const WINDOW_MS = 5 * 60 * 1000;
const loginAttempts = new Map<string, RateLimitState>();

function getClientKey(request: Request) {
  const forwarded = request.headers.get("x-forwarded-for")?.split(",")[0]?.trim();
  return forwarded || request.headers.get("x-real-ip") || "unknown";
}

function isRateLimited(key: string) {
  const now = Date.now();
  const state = loginAttempts.get(key);
  if (!state) return false;
  if (now - state.firstAttemptAt > WINDOW_MS) {
    loginAttempts.delete(key);
    return false;
  }
  return state.count >= MAX_ATTEMPTS;
}

function registerFailedAttempt(key: string) {
  const now = Date.now();
  const state = loginAttempts.get(key);
  if (!state || now - state.firstAttemptAt > WINDOW_MS) {
    loginAttempts.set(key, { count: 1, firstAttemptAt: now });
    return;
  }
  loginAttempts.set(key, { ...state, count: state.count + 1 });
}

function resetAttempts(key: string) {
  loginAttempts.delete(key);
}

function sha256Hex(value: string) {
  return createHash("sha256").update(value, "utf8").digest("hex");
}

function safeEqual(a: string, b: string) {
  const left = Buffer.from(a);
  const right = Buffer.from(b);
  if (left.length !== right.length) return false;
  return timingSafeEqual(left, right);
}

export async function POST(request: Request) {
  const key = getClientKey(request);
  if (isRateLimited(key)) {
    return NextResponse.json({ error: "Bạn đã thử quá nhiều lần. Vui lòng thử lại sau vài phút." }, { status: 429 });
  }

  const body = (await request.json().catch(() => null)) as LoginBody | null;
  const username = body?.username?.trim() ?? "";
  const password = body?.password?.trim() ?? "";
  const expectedUsername = process.env.ADMIN_USERNAME?.trim() ?? "";
  const expectedPasswordHash = process.env.ADMIN_PASSWORD_HASH?.trim().toLowerCase() ?? "";
  const expectedPassword = process.env.ADMIN_PASSWORD?.trim() ?? "";

  if (!expectedUsername || (!expectedPasswordHash && !expectedPassword)) {
    return NextResponse.json({ error: "Admin chưa được cấu hình thông tin đăng nhập an toàn." }, { status: 500 });
  }

  const usernameMatches = safeEqual(username, expectedUsername);
  const hashMatches = expectedPasswordHash ? safeEqual(sha256Hex(password), expectedPasswordHash) : false;
  const plainPasswordMatches = !expectedPasswordHash && expectedPassword ? safeEqual(password, expectedPassword) : false;
  const passwordMatches = hashMatches || plainPasswordMatches;

  if (!usernameMatches || !passwordMatches) {
    registerFailedAttempt(key);
    return NextResponse.json({ error: "Sai tài khoản hoặc mật khẩu" }, { status: 401 });
  }

  resetAttempts(key);

  const response = NextResponse.json({ ok: true });
  const maxAge = 60 * 60 * 8;
  const cookieOptions = {
    httpOnly: true,
    sameSite: "lax" as const,
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge,
  };

  response.cookies.set("reviewx_admin_session", createAdminSessionToken(maxAge), cookieOptions);
  response.cookies.set("reviewx_admin_role", "admin", cookieOptions);

  return response;
}
