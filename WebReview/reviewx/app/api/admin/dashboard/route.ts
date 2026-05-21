import { NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";

function toHoursAgo(value: Date) {
  const diffMs = Date.now() - value.getTime();
  const minutes = Math.max(1, Math.floor(diffMs / 60000));
  if (minutes < 60) return `${minutes} phút trước`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  return `${days} ngày trước`;
}

function timeUntil(value: Date) {
  const diffMs = value.getTime() - Date.now();
  const minutes = Math.max(1, Math.floor(diffMs / 60000));
  if (minutes < 60) return `${minutes} phút nữa`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} giờ nữa`;
  const days = Math.floor(hours / 24);
  return `${days} ngày nữa`;
}

export async function GET(request: Request) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;

  try {
    const now = new Date();
    const soonThreshold = new Date(now.getTime() + 1000 * 60 * 60 * 24 * 3);

    const [
      totalProducts,
      totalReviews,
      totalActiveDeals,
      totalExpiredDeals,
      totalClicks,
      latestReviews,
      latestDeals,
      topProducts,
      topReviews,
      endingSoonDeals,
      missingAffiliateDeals,
    ] = await Promise.all([
      prisma.product.count(),
      prisma.review.count(),
      prisma.deal.count({ where: { status: "ACTIVE" } }),
      prisma.deal.count({ where: { status: "EXPIRED" } }),
      prisma.clickEvent.count(),
      prisma.review.findMany({
        orderBy: { updatedAt: "desc" },
        take: 4,
        select: { id: true, title: true, updatedAt: true },
      }),
      prisma.deal.findMany({
        orderBy: { updatedAt: "desc" },
        take: 4,
        select: { id: true, product: { select: { name: true } }, status: true, updatedAt: true },
      }),
      prisma.clickEvent.groupBy({ by: ["productId"], _count: { _all: true }, orderBy: { _count: { productId: "desc" } }, take: 5 }),
      prisma.clickEvent.groupBy({ by: ["reviewId"], _count: { _all: true }, where: { reviewId: { not: null } }, orderBy: { _count: { reviewId: "desc" } }, take: 5 }),
      prisma.deal.findMany({
        where: { status: "ACTIVE", endTime: { gte: now, lte: soonThreshold } },
        orderBy: { endTime: "asc" },
        take: 5,
        select: {
          id: true,
          endTime: true,
          product: { select: { id: true, name: true } },
        },
      }),
      prisma.deal.findMany({
        where: {
          OR: [{ affiliateLink: { affiliateUrl: "" } }, { affiliateLink: { status: "BROKEN" } }],
        },
        orderBy: { updatedAt: "desc" },
        take: 5,
        select: {
          id: true,
          product: { select: { id: true, name: true } },
          updatedAt: true,
        },
      }),
    ]);

    const topProductIds = topProducts.map((x: { productId: string }) => x.productId);
    const topReviewIds = topReviews.map((x: { reviewId: string | null }) => x.reviewId).filter(Boolean) as string[];

    const [productRows, reviewRows] = await Promise.all([
      topProductIds.length ? prisma.product.findMany({ where: { id: { in: topProductIds } }, select: { id: true, name: true } }) : [],
      topReviewIds.length ? prisma.review.findMany({ where: { id: { in: topReviewIds } }, select: { id: true, title: true } }) : [],
    ]);

    const productMap = new Map(productRows.map((r: { id: string; name: string }) => [r.id, r.name]));
    const reviewMap = new Map(reviewRows.map((r: { id: string; title: string }) => [r.id, r.title]));

    const metrics = [
      { label: "Total products", value: String(totalProducts), tone: "blue" as const },
      { label: "Total reviews", value: String(totalReviews), tone: "blue" as const },
      { label: "Total active deals", value: String(totalActiveDeals), tone: "green" as const },
      { label: "Total expired deals", value: String(totalExpiredDeals), tone: "amber" as const },
      { label: "Total affiliate clicks", value: totalClicks.toLocaleString("vi-VN"), tone: "blue" as const },
    ];

    const activity = [
      ...latestReviews.map((r: { id: string; title: string; updatedAt: Date }) => ({
        id: `review-${r.id}`,
        title: `Review '${r.title}' vừa được cập nhật`,
        time: toHoursAgo(r.updatedAt),
      })),
      ...latestDeals.map((d: { id: string; product: { name: string }; status: string; updatedAt: Date }) => ({
        id: `deal-${d.id}`,
        title: `Deal '${d.product.name}' đang ở trạng thái ${d.status}`,
        time: toHoursAgo(d.updatedAt),
      })),
    ].slice(0, 8);

    const topProductItems = topProducts.map((row: { productId: string; _count: { _all: number } }) => ({
      id: row.productId,
      name: productMap.get(row.productId) ?? row.productId,
      clicks: row._count._all,
    }));

    const topReviewItems = topReviews.map((row: { reviewId: string | null; _count: { _all: number } }) => ({
      id: row.reviewId ?? "unknown",
      name: (row.reviewId && reviewMap.get(row.reviewId)) ?? "Unknown review",
      views: row._count._all,
    }));

    const endingSoon = endingSoonDeals.map((deal: { id: string; endTime: Date; product: { id: string; name: string } }) => ({
      id: deal.id,
      productId: deal.product.id,
      productName: deal.product.name,
      endTime: deal.endTime.toISOString(),
      timeLeft: timeUntil(deal.endTime),
    }));

    const missingAffiliate = missingAffiliateDeals.map((deal: { id: string; product: { id: string; name: string }; updatedAt: Date }) => ({
      id: deal.id,
      productId: deal.product.id,
      productName: deal.product.name,
      updatedAt: deal.updatedAt.toISOString(),
      updatedLabel: toHoursAgo(deal.updatedAt),
    }));

    return NextResponse.json(ok({
      metrics,
      activity,
      topProducts: topProductItems,
      topReviews: topReviewItems,
      endingSoon,
      missingAffiliate,
    }));
  } catch {
    return NextResponse.json(fail("Không thể tải dashboard data."), { status: 500 });
  }
}
