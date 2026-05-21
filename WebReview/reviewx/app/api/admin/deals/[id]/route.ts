import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";

const DEAL_STATUSES = new Set(["Active", "Expired", "Draft"]);

function parseDateInput(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

type DealPatchPayload = {
  currentPrice?: string;
  oldPrice?: string;
  discount?: string;
  couponCode?: string | null;
  startTime?: string;
  endTime?: string;
  status?: string;
  affiliateLinkId?: string;
};

export async function DELETE(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;

  const row = await prisma.deal.findUnique({ where: { id } });
  if (!row) return NextResponse.json(fail("Không tìm thấy deal."), { status: 404 });

  await prisma.deal.delete({ where: { id } });
  return NextResponse.json(ok({ id }));
}

export async function PATCH(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const body = (await request.json()) as DealPatchPayload;

  const row = await prisma.deal.findUnique({ where: { id } });
  if (!row) return NextResponse.json(fail("Không tìm thấy deal."), { status: 404 });

  if (body.status !== undefined && !DEAL_STATUSES.has(body.status.trim())) {
    return NextResponse.json(fail("Trạng thái deal không hợp lệ."), { status: 400 });
  }

  const nextAffiliateLinkId = body.affiliateLinkId !== undefined ? body.affiliateLinkId.trim() : row.affiliateLinkId;
  if (!nextAffiliateLinkId) return NextResponse.json(fail("Affiliate link không hợp lệ."), { status: 400 });
  const affiliateLink = await prisma.affiliateLink.findUnique({ where: { id: nextAffiliateLinkId }, select: { id: true, productId: true } });
  if (!affiliateLink) return NextResponse.json(fail("Affiliate link không tồn tại."), { status: 400 });
  if (affiliateLink.productId !== row.productId) {
    return NextResponse.json(fail("Affiliate link không thuộc product của deal."), { status: 400 });
  }

  const parsedStart = body.startTime !== undefined ? parseDateInput(body.startTime) : row.startTime;
  const parsedEnd = body.endTime !== undefined ? parseDateInput(body.endTime) : row.endTime;
  if (!parsedStart || !parsedEnd || parsedStart >= parsedEnd) {
    return NextResponse.json(fail("Khoảng thời gian deal không hợp lệ."), { status: 400 });
  }

  const updated = await prisma.deal.update({
    where: { id },
    data: {
      ...(body.currentPrice !== undefined ? { currentPrice: body.currentPrice.trim() } : {}),
      ...(body.oldPrice !== undefined ? { oldPrice: body.oldPrice.trim() } : {}),
      ...(body.discount !== undefined ? { discount: body.discount.trim() } : {}),
      ...(body.couponCode !== undefined ? { couponCode: body.couponCode?.trim() || null } : {}),
      ...(body.startTime !== undefined ? { startTime: parsedStart } : {}),
      ...(body.endTime !== undefined ? { endTime: parsedEnd } : {}),
      ...(body.status !== undefined ? { status: body.status.trim() } : {}),
      ...(body.affiliateLinkId !== undefined ? { affiliateLinkId: body.affiliateLinkId.trim() } : {}),
    },
  });

  return NextResponse.json(ok(updated));
}
