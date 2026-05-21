import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";
import { normalizePaginationParams } from "@/lib/validators";

type CategoryPayload = {
  name?: string;
  slug?: string;
  parentId?: string;
  icon?: string;
  description?: string;
  seoTitle?: string;
  seoDescription?: string;
  sortOrder?: number;
};

function toSlug(value: string) {
  return value.toLowerCase().trim().replace(/[^a-z0-9\s-]/g, "").replace(/\s+/g, "-");
}

export async function GET(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { page, limit, skip } = normalizePaginationParams(request.nextUrl.searchParams);
  const items = await prisma.category.findMany({
    orderBy: [{ sortOrder: "asc" }, { updatedAt: "desc" }],
    skip,
    take: limit,
    include: {
      _count: {
        select: {
          products: true,
          reviews: true,
        },
      },
      products: {
        select: {
          _count: {
            select: {
              deals: true,
            },
          },
        },
      },
    },
  });
  return NextResponse.json(ok({ items, page, limit }));
}

export async function POST(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const body = (await request.json()) as CategoryPayload;
  const name = body.name?.trim() ?? "";
  const slug = body.slug?.trim() || toSlug(name);
  if (!name || !slug) return NextResponse.json(fail("Thiếu name hoặc slug."), { status: 400 });
  const exists = await prisma.category.findUnique({ where: { slug } });
  if (exists) return NextResponse.json(fail("Slug đã tồn tại."), { status: 400 });
  if (body.parentId?.trim()) {
    const parent = await prisma.category.findUnique({ where: { id: body.parentId.trim() } });
    if (!parent) return NextResponse.json(fail("Parent category không tồn tại."), { status: 400 });
  }
  const created = await prisma.category.create({
    data: {
      name,
      slug,
      parentId: body.parentId?.trim() || null,
      icon: body.icon?.trim() || null,
      description: body.description?.trim() || null,
      seoTitle: body.seoTitle?.trim() || null,
      seoDescription: body.seoDescription?.trim() || null,
      sortOrder: Number.isFinite(body.sortOrder) ? Number(body.sortOrder) : 0,
      status: "DRAFT",
    },
  });
  return NextResponse.json(ok(created));
}
