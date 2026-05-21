import { createHmac, timingSafeEqual } from "node:crypto";

type AdminSessionPayload = {
  role: "admin";
  exp: number;
};

const DEFAULT_SECRET = "reviewx-dev-admin-secret-change-me";

function getSecret() {
  return process.env.ADMIN_SESSION_SECRET || DEFAULT_SECRET;
}

function toBase64Url(value: string) {
  return Buffer.from(value, "utf8").toString("base64url");
}

function fromBase64Url(value: string) {
  return Buffer.from(value, "base64url").toString("utf8");
}

function sign(data: string) {
  return createHmac("sha256", getSecret()).update(data).digest("base64url");
}

export function createAdminSessionToken(maxAgeSeconds: number) {
  const payload: AdminSessionPayload = {
    role: "admin",
    exp: Math.floor(Date.now() / 1000) + maxAgeSeconds,
  };
  const encodedPayload = toBase64Url(JSON.stringify(payload));
  const signature = sign(encodedPayload);
  return `${encodedPayload}.${signature}`;
}

export function verifyAdminSessionToken(token: string | undefined) {
  if (!token) return false;
  const [encodedPayload, signature] = token.split(".");
  if (!encodedPayload || !signature) return false;

  const expectedSignature = sign(encodedPayload);
  const actual = Buffer.from(signature);
  const expected = Buffer.from(expectedSignature);
  if (actual.length !== expected.length) return false;
  if (!timingSafeEqual(actual, expected)) return false;

  try {
    const payload = JSON.parse(fromBase64Url(encodedPayload)) as AdminSessionPayload;
    if (payload.role !== "admin") return false;
    if (payload.exp <= Math.floor(Date.now() / 1000)) return false;
    return true;
  } catch {
    return false;
  }
}

export async function isAdminAuthenticated(): Promise<boolean> {
  const { cookies } = await import("next/headers");
  const cookieStore = await cookies();
  const token = cookieStore.get("reviewx_admin_session")?.value;
  const role = cookieStore.get("reviewx_admin_role")?.value;
  return verifyAdminSessionToken(token) && role === "admin";
}
