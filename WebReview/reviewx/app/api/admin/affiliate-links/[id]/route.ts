import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";

type LinkStatus = "ACTIVE" | "INACTIVE" | "BROKEN";

export async function GET(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const link = await prisma.affiliateLink.findUnique({ where: { id } });
  if (!link) return NextResponse.json(fail("Không tìm thấy affiliate link."), { status: 404 });
  return NextResponse.json(ok(link));
}

export async function PATCH(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const body = (await request.json()) as { status?: LinkStatus };
  if (!body.status || !["ACTIVE", "INACTIVE", "BROKEN"].includes(body.status)) {
    return NextResponse.json(fail("Trạng thái không hợp lệ."), { status: 400 });
  }
  const link = await prisma.affiliateLink.findUnique({ where: { id } });
  if (!link) return NextResponse.json(fail("Không tìm thấy affiliate link."), { status: 404 });
  const updated = await prisma.affiliateLink.update({ where: { id }, data: { status: body.status } });
  return NextResponse.json(ok(updated));
}

export async function DELETE(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  await prisma.affiliateLink.delete({ where: { id } }).catch(() => null);
  return NextResponse.json(ok({ deleted: true }));
}
