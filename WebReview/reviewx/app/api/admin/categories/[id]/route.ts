import { NextRequest, NextResponse } from "next/server";
import { Prisma } from "@prisma/client";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";

export async function GET(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const item = await prisma.category.findUnique({ where: { id } });
  if (!item) return NextResponse.json(fail("Không tìm thấy category."), { status: 404 });
  return NextResponse.json(ok(item));
}

export async function PATCH(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const body = (await request.json()) as { name?: string; slug?: string; icon?: string; description?: string; seoTitle?: string; seoDescription?: string; sortOrder?: number; status?: "DRAFT" | "PUBLISHED" | "ARCHIVED" };
  const exists = await prisma.category.findUnique({ where: { id } });
  if (!exists) return NextResponse.json(fail("Không tìm thấy category."), { status: 404 });
  const data: Prisma.CategoryUncheckedUpdateInput = {};
  if (body.name !== undefined) data.name = body.name.trim();
  if (body.slug !== undefined) data.slug = body.slug.trim();
  if (body.icon !== undefined) data.icon = body.icon.trim() || null;
  if (body.description !== undefined) data.description = body.description.trim() || null;
  if (body.seoTitle !== undefined) data.seoTitle = body.seoTitle.trim() || null;
  if (body.seoDescription !== undefined) data.seoDescription = body.seoDescription.trim() || null;
  if (body.sortOrder !== undefined) data.sortOrder = Number(body.sortOrder);
  if (body.status !== undefined) data.status = body.status;
  const updated = await prisma.category.update({ where: { id }, data });
  return NextResponse.json(ok(updated));
}

export async function DELETE(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  await prisma.category.delete({ where: { id } }).catch(() => null);
  return NextResponse.json(ok({ deleted: true }));
}
