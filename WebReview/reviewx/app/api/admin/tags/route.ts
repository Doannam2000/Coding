import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { ok } from "@/lib/api-response";
import { normalizePaginationParams } from "@/lib/validators";

export async function GET(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;

  const { page, limit, skip } = normalizePaginationParams(request.nextUrl.searchParams);
  const q = request.nextUrl.searchParams.get("q")?.trim() ?? "";

  const where = q ? { name: { contains: q } } : {};
  const [items, total] = await Promise.all([
    prisma.tag.findMany({
      where,
      orderBy: { updatedAt: "desc" },
      skip,
      take: limit,
    }),
    prisma.tag.count({ where }),
  ]);

  return NextResponse.json(ok({ items, total, page, limit }));
}
