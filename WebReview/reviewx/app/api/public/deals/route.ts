import { NextRequest, NextResponse } from "next/server";
import type { Prisma } from "@prisma/client";
import { prisma } from "@/lib/prisma";
import { ok } from "@/lib/api-response";
import { normalizePaginationParams } from "@/lib/validators";

type PublicDealItem = {
  id: string;
  productId: string;
  productSlug: string;
  productName: string;
  image: string;
  currentPrice: string;
  oldPrice: string;
  discount: string;
  couponCode: string | null;
  startTime: string;
  endTime: string;
  status: "active" | "expired" | "scheduled" | "disabled";
  platform: string;
  isFeatured: boolean;
  affiliateUrl: string | null;
  internalUrl: string | null;
  hasAffiliate: boolean;
};

function toNumberFromText(value: string) {
  const digits = value.replace(/\D/g, "");
  return digits ? Number(digits) : 0;
}

function toDiscountPercent(value: string) {
  const match = value.match(/\d+/);
  return match ? Number(match[0]) : 0;
}

function mapStatus(status: string, startTime: Date, endTime: Date, now: Date): PublicDealItem["status"] {
  if (status === "Disabled") return "disabled";
  if (status === "Expired" || endTime <= now) return "expired";
  if (status === "Draft" || startTime > now) return "scheduled";
  return "active";
}

function applyStatusFilter(status: string, startTime: Date, endTime: Date, now: Date, wanted: string) {
  const mapped = mapStatus(status, startTime, endTime, now);
  if (wanted === "active") return mapped === "active";
  if (wanted === "expired") return mapped === "expired" || mapped === "disabled";
  if (wanted === "scheduled") return mapped === "scheduled";
  return true;
}

function sortDeals(items: PublicDealItem[], sort: string) {
  if (sort === "biggest-discount") {
    return [...items].sort((a, b) => toDiscountPercent(b.discount) - toDiscountPercent(a.discount));
  }
  if (sort === "lowest-price") {
    return [...items].sort((a, b) => toNumberFromText(a.currentPrice) - toNumberFromText(b.currentPrice));
  }
  if (sort === "ending-soon") {
    return [...items].sort((a, b) => new Date(a.endTime).getTime() - new Date(b.endTime).getTime());
  }
  return [...items].sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime());
}

export async function GET(request: NextRequest) {
  const { page, limit } = normalizePaginationParams(request.nextUrl.searchParams);
  const q = request.nextUrl.searchParams.get("q")?.trim() ?? "";
  const categorySlug = request.nextUrl.searchParams.get("category")?.trim() ?? "";
  const platform = request.nextUrl.searchParams.get("platform")?.trim() ?? "";
  const status = request.nextUrl.searchParams.get("status")?.trim() ?? "all";
  const sort = request.nextUrl.searchParams.get("sort")?.trim() ?? "newest";
  const couponOnly = request.nextUrl.searchParams.get("couponOnly") === "true";
  const minDiscount = Number(request.nextUrl.searchParams.get("minDiscount") ?? "0") || 0;
  const maxPrice = Number(request.nextUrl.searchParams.get("maxPrice") ?? "0") || 0;
  const now = new Date();

  let categoryId: string | null = null;
  if (categorySlug) {
    const category = await prisma.category.findUnique({
      where: { slug: categorySlug },
      select: { id: true },
    });
    if (!category) {
      return NextResponse.json(ok({ items: [], page, limit, total: 0 }));
    }
    categoryId = category.id;
  }

  const where: Prisma.DealWhereInput = {};
  const productWhere: Prisma.ProductWhereInput = {};
  if (couponOnly) {
    where.couponCode = { not: null };
  }
  if (platform && platform !== "all") {
    where.affiliateLink = { platform };
  }
  if (categoryId) {
    productWhere.categoryId = categoryId;
  }
  if (q) {
    productWhere.name = { contains: q };
  }
  if (Object.keys(productWhere).length > 0) {
    where.product = productWhere;
  }

  const deals = await prisma.deal.findMany({
    where,
    include: {
      product: {
        select: {
          id: true,
          slug: true,
          name: true,
          images: { take: 1, orderBy: { sortOrder: "asc" } },
        },
      },
      affiliateLink: {
        select: {
          affiliateUrl: true,
          internalUrl: true,
          platform: true,
        },
      },
    },
  });

  let mapped: PublicDealItem[] = deals
    .filter((deal) => applyStatusFilter(deal.status, deal.startTime, deal.endTime, now, status))
    .map((deal) => ({
      id: deal.id,
      productId: deal.productId,
      productSlug: deal.product?.slug ?? "",
      productName: deal.product?.name ?? "",
      image: deal.product?.images?.[0]?.url ?? "",
      currentPrice: deal.currentPrice,
      oldPrice: deal.oldPrice,
      discount: deal.discount,
      couponCode: deal.couponCode,
      startTime: deal.startTime.toISOString(),
      endTime: deal.endTime.toISOString(),
      status: mapStatus(deal.status, deal.startTime, deal.endTime, now),
      platform: deal.affiliateLink?.platform ?? "N/A",
      isFeatured: false,
      affiliateUrl: deal.affiliateLink?.affiliateUrl ?? null,
      internalUrl: deal.affiliateLink?.internalUrl ?? null,
      hasAffiliate: Boolean(deal.affiliateLink?.affiliateUrl),
    }));

  if (minDiscount > 0) {
    mapped = mapped.filter((deal) => toDiscountPercent(deal.discount) >= minDiscount);
  }

  if (maxPrice > 0) {
    mapped = mapped.filter((deal) => toNumberFromText(deal.currentPrice) <= maxPrice);
  }

  const sorted = sortDeals(mapped, sort);
  const total = sorted.length;
  const start = Math.max(0, (page - 1) * limit);
  const items = sorted.slice(start, start + limit);

  return NextResponse.json(ok({ items, page, limit, total }));
}
