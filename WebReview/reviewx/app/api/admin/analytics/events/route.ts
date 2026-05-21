import { NextResponse } from "next/server";
import { getClickEventsFromDb } from "@/lib/analytics-db";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";

export async function GET(request: Request) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;

  const { searchParams } = new URL(request.url);
  const rangeParam = searchParams.get("range");
  const range = rangeParam === "30D" || rangeParam === "90D" ? rangeParam : "7D";
  const events = await getClickEventsFromDb(range);
  return NextResponse.json({ events });
}
