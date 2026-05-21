import { NextRequest, NextResponse } from "next/server";

const LINK_STATUSES = ["ACTIVE", "INACTIVE", "BROKEN"] as const;
type LinkStatus = (typeof LINK_STATUSES)[number];

function isLinkStatus(value: string): value is LinkStatus {
  return LINK_STATUSES.includes(value as LinkStatus);
}
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";
import { normalizePaginationParams, parseHttpUrl } from "@/lib/validators";

type CreatePayload = {
  label?: string;
  productId?: string;
  reviewId?: string;
  platform?: string;
  originalUrl?: string;
  affiliateUrl?: string;
  status?: LinkStatus;
};

function toInternalUrl(id: string, platform: string) {
  const p = platform.toLowerCase();
  const suffix = p === "shopee" ? "shp" : p === "lazada" ? "lzd" : "tki";
  return `/recommends/${id}-${suffix}`;
}

export async function GET(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;

  const { page, limit, skip } = normalizePaginationParams(request.nextUrl.searchParams);
  const q = request.nextUrl.searchParams.get("q")?.trim() ?? "";
  const platform = request.nextUrl.searchParams.get("platform") ?? "ALL";
  const status = request.nextUrl.searchParams.get("status") ?? "ALL";

  const where = {
    ...(q
      ? {
          OR: [
            { label: { contains: q } },
            { productId: { contains: q } },
            { reviewId: { contains: q } },
            { internalUrl: { contains: q } },
          ],
        }
      : {}),
    ...(platform !== "ALL" ? { platform } : {}),
    ...(status !== "ALL" && isLinkStatus(status) ? { status } : {}),
  };

  const [items, total] = await Promise.all([
    prisma.affiliateLink.findMany({
      where,
      orderBy: { createdAt: "desc" },
      skip,
      take: limit,
    }),
    prisma.affiliateLink.count({ where }),
  ]);

  return NextResponse.json(ok({ items, total, page, limit }));
}

export async function POST(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;

  const body = (await request.json()) as CreatePayload;
  const label = body.label?.trim() ?? "";
  const productId = body.productId?.trim() ?? "";
  const reviewId = body.reviewId?.trim() ?? "";
  const platform = body.platform?.trim() ?? "";
  const originalUrl = body.originalUrl?.trim() ?? "";
  const affiliateUrl = body.affiliateUrl?.trim() ?? "";
  const status = body.status ?? "ACTIVE";
  if (!isLinkStatus(status)) {
    return NextResponse.json(fail("Trạng thái affiliate link không hợp lệ."), { status: 400 });
  }

  if (!label || !productId || !platform || !originalUrl || !affiliateUrl) {
    return NextResponse.json(fail("Vui lòng nhập đủ các trường bắt buộc."), { status: 400 });
  }

  if (!parseHttpUrl(originalUrl) || !parseHttpUrl(affiliateUrl)) {
    return NextResponse.json(fail("Original URL hoặc Affiliate URL không hợp lệ."), { status: 400 });
  }

  const product = await prisma.product.findUnique({ where: { id: productId }, select: { id: true } });
  if (!product) return NextResponse.json(fail("Product không tồn tại."), { status: 400 });

  if (reviewId) {
    const review = await prisma.review.findUnique({ where: { id: reviewId }, select: { id: true, productId: true } });
    if (!review) return NextResponse.json(fail("Review không tồn tại."), { status: 400 });
    if (review.productId !== productId) {
      return NextResponse.json(fail("Review không thuộc product đã chọn."), { status: 400 });
    }
  }

  const created = await prisma.affiliateLink.create({
    data: {
      label,
      productId,
      reviewId: reviewId || null,
      platform,
      originalUrl,
      affiliateUrl,
      status,
      internalUrl: `/recommends/pending-${Date.now()}`,
    },
  });

  const withInternalUrl = await prisma.affiliateLink.update({
    where: { id: created.id },
    data: { internalUrl: toInternalUrl(created.id, platform) },
  });

  return NextResponse.json(ok(withInternalUrl));
}
