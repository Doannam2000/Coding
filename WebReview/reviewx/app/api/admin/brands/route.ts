import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";
import { normalizePaginationParams } from "@/lib/validators";

type BrandPayload = {
  name?: string;
  slug?: string;
  logo?: string;
  description?: string;
};

function toSlug(value: string) {
  return value.toLowerCase().trim().replace(/[^a-z0-9\s-]/g, "").replace(/\s+/g, "-");
}

export async function GET(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { page, limit, skip } = normalizePaginationParams(request.nextUrl.searchParams);
  const items = await prisma.brand.findMany({ orderBy: { updatedAt: "desc" }, skip, take: limit });
  return NextResponse.json(ok({ items, page, limit }));
}

export async function POST(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const body = (await request.json()) as BrandPayload;
  const name = body.name?.trim() ?? "";
  const slug = (body.slug?.trim() || toSlug(name));
  if (!name || !slug) return NextResponse.json(fail("Thiếu name hoặc slug."), { status: 400 });
  const exists = await prisma.brand.findUnique({ where: { slug } });
  if (exists) return NextResponse.json(fail("Slug đã tồn tại."), { status: 400 });
  const created = await prisma.brand.create({ data: { name, slug, logo: body.logo?.trim() || null, description: body.description?.trim() || null } });
  return NextResponse.json(ok(created));
}
