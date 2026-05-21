import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";

export async function GET(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const item = await prisma.review.findUnique({ where: { id }, include: { product: true } });
  if (!item) return NextResponse.json(fail("Không tìm thấy AI draft."), { status: 404 });
  return NextResponse.json(ok(item));
}

export async function PATCH(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const body = (await request.json()) as { action?: string; quickVerdict?: string; seoTitle?: string; seoDescription?: string; articleDraft?: string };
  const exists = await prisma.review.findUnique({ where: { id } });
  if (!exists) return NextResponse.json(fail("Không tìm thấy AI draft."), { status: 404 });
  let status = exists.status;
  if (body.action === "approve") status = "PUBLISHED";
  else if (body.action === "reject") status = "ARCHIVED";
  const updated = await prisma.review.update({
    where: { id },
    data: {
      ...(body.quickVerdict !== undefined ? { summary: body.quickVerdict.trim() } : {}),
      ...(body.seoTitle !== undefined ? { seoTitle: body.seoTitle.trim() } : {}),
      ...(body.seoDescription !== undefined ? { seoDescription: body.seoDescription.trim() } : {}),
      ...(body.articleDraft !== undefined ? { content: body.articleDraft.trim() } : {}),
      status,
    },
  });
  return NextResponse.json(ok(updated));
}
