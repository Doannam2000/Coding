import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";

export async function GET(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const item = await prisma.brand.findUnique({ where: { id } });
  if (!item) return NextResponse.json(fail("Không tìm thấy brand."), { status: 404 });
  return NextResponse.json(ok(item));
}

export async function PATCH(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const body = (await request.json()) as { name?: string; slug?: string; logo?: string; website?: string; description?: string; trustScore?: number };
  const exists = await prisma.brand.findUnique({ where: { id } });
  if (!exists) return NextResponse.json(fail("Không tìm thấy brand."), { status: 404 });
  const updated = await prisma.brand.update({
    where: { id },
    data: {
      ...(body.name !== undefined ? { name: body.name.trim() } : {}),
      ...(body.slug !== undefined ? { slug: body.slug.trim() } : {}),
      ...(body.logo !== undefined ? { logo: body.logo.trim() || null } : {}),
      ...(body.website !== undefined ? { website: body.website.trim() || null } : {}),
      ...(body.description !== undefined ? { description: body.description.trim() || null } : {}),
      ...(body.trustScore !== undefined ? { trustScore: Number(body.trustScore) } : {}),
    },
  });
  return NextResponse.json(ok(updated));
}

export async function DELETE(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  await prisma.brand.delete({ where: { id } }).catch(() => null);
  return NextResponse.json(ok({ deleted: true }));
}
