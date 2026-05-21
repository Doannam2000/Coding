import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";
import { normalizePaginationParams } from "@/lib/validators";

const DEAL_STATUSES = new Set(["Active", "Expired", "Draft"]);

function parseDateInput(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

type DealPayload = {
  productId?: string;
  affiliateLinkId?: string;
  currentPrice?: string;
  oldPrice?: string;
  discount?: string;
  couponCode?: string | null;
  startTime?: string;
  endTime?: string;
  status?: string;
};

export async function GET(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;

  const { page, limit, skip } = normalizePaginationParams(request.nextUrl.searchParams);
  const q = request.nextUrl.searchParams.get("q")?.trim() ?? "";
  const status = request.nextUrl.searchParams.get("status")?.trim() ?? "ALL";
  const where = {
    ...(q
      ? {
          OR: [
            { product: { name: { contains: q } } },
            { discount: { contains: q } },
            { currentPrice: { contains: q } },
          ],
        }
      : {}),
    ...(status !== "ALL" ? { status } : {}),
  };
  const items = await prisma.deal.findMany({
    where,
    include: {
      product: {
        include: {
          images: { take: 1, orderBy: { sortOrder: "asc" } },
        },
      },
      affiliateLink: true,
    },
    orderBy: { updatedAt: "desc" },
    skip,
    take: limit,
  });

  return NextResponse.json(ok({ items, page, limit }));
}

export async function POST(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;

  const body = (await request.json()) as DealPayload;
  const productId = body.productId?.trim() ?? "";
  const affiliateLinkId = body.affiliateLinkId?.trim() ?? "";
  const currentPrice = body.currentPrice?.trim() ?? "";
  const oldPrice = body.oldPrice?.trim() ?? "";
  const discount = body.discount?.trim() ?? "";
  const couponCode = body.couponCode?.trim() ?? "";
  const startTime = body.startTime?.trim() ?? "";
  const endTime = body.endTime?.trim() ?? "";
  const status = body.status?.trim() ?? "Draft";

  if (!productId || !affiliateLinkId || !currentPrice || !oldPrice || !discount || !startTime || !endTime) {
    return NextResponse.json(fail("Thiếu trường bắt buộc."), { status: 400 });
  }

  if (!DEAL_STATUSES.has(status)) {
    return NextResponse.json(fail("Trạng thái deal không hợp lệ."), { status: 400 });
  }

  const parsedStart = parseDateInput(startTime);
  const parsedEnd = parseDateInput(endTime);
  if (!parsedStart || !parsedEnd || parsedStart >= parsedEnd) {
    return NextResponse.json(fail("Khoảng thời gian deal không hợp lệ."), { status: 400 });
  }

  const [product, affiliateLink] = await Promise.all([
    prisma.product.findUnique({ where: { id: productId }, select: { id: true } }),
    prisma.affiliateLink.findUnique({ where: { id: affiliateLinkId }, select: { id: true, productId: true } }),
  ]);
  if (!product) return NextResponse.json(fail("Product không tồn tại."), { status: 400 });
  if (!affiliateLink) return NextResponse.json(fail("Affiliate link không tồn tại."), { status: 400 });
  if (affiliateLink.productId !== productId) {
    return NextResponse.json(fail("Affiliate link không thuộc product đã chọn."), { status: 400 });
  }

  const created = await prisma.deal.create({
    data: {
      productId,
      affiliateLinkId,
      currentPrice,
      oldPrice,
      discount,
      couponCode: couponCode || null,
      startTime: parsedStart,
      endTime: parsedEnd,
      status,
    },
  });

  return NextResponse.json(ok(created));
}
