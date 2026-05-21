import { NextRequest, NextResponse } from "next/server";
import type { Prisma } from "@prisma/client";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";
import { normalizePaginationParams } from "@/lib/validators";

type ProductStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

type ProductPayload = {
  name?: string;
  slug?: string;
  brandId?: string;
  categoryId?: string;
  description?: string | null;
  shortDescription?: string | null;
  fullDescription?: string | null;
  badge?: string | null;
  thumbnail?: string | null;
  thumbnailAlt?: string | null;
  priceMin?: number | null;
  priceMax?: number | null;
  currentPrice?: number | null;
  originalPrice?: number | null;
  priceRangeMin?: number | null;
  priceRangeMax?: number | null;
  currency?: string | null;
  priceUpdatedAt?: string | null;
  rating?: number | null;
  soldCount?: number | null;
  worthScore?: number | null;
  verdict?: string | null;
  verdictLabel?: string | null;
  shouldBuyIf?: string | null;
  considerIf?: string | null;
  avoidIfText?: string | null;
  buyUnderPrice?: number | null;
  considerAbovePrice?: number | null;
  finalVerdict?: string | null;
  pros?: string | null;
  cons?: string | null;
  prosList?: string[];
  consList?: string[];
  bestFor?: string | null;
  avoidIf?: string | null;
  suitableForList?: string[];
  notSuitableForList?: string[];
  specs?: string | null;
  specsItems?: Array<{ key?: string; value?: string; group?: string | null; sortOrder?: number | null }>;
  praisedPoints?: string[];
  complainedPoints?: string[];
  sentimentScore?: number | null;
  insightNote?: string | null;
  dataSourceNote?: string | null;
  seoTitle?: string | null;
  seoDescription?: string | null;
  seoOgImage?: string | null;
  canonicalUrl?: string | null;
  noindex?: boolean;
  shopeeOriginalUrl?: string | null;
  shopeeAffiliateUrl?: string | null;
  shopeeTrackingNote?: string | null;
  lazadaOriginalUrl?: string | null;
  lazadaAffiliateUrl?: string | null;
  tikiOriginalUrl?: string | null;
  tikiAffiliateUrl?: string | null;
  officialStoreUrl?: string | null;
  primaryPlatform?: string | null;
  ctaLabel?: string | null;
  linkStatus?: string | null;
  status?: ProductStatus;
  tagIds?: string[];
  imageUrls?: string[];
  galleryImages?: Array<{ url?: string; alt?: string | null; sortOrder?: number | null }>;
  affiliateLinkIds?: string[];
};

const PRODUCT_LIST_INCLUDE = {
  brand: true,
  category: true,
  productTags: { include: { tag: true } },
  images: { orderBy: { sortOrder: "asc" } },
  affiliateLinks: true,
} satisfies Prisma.ProductInclude;

type ProductListItem = Prisma.ProductGetPayload<{ include: typeof PRODUCT_LIST_INCLUDE }>;

function normalizeUniqueIds(ids: string[] | undefined): string[] {
  if (!ids?.length) return [];
  const unique = new Set<string>();
  for (const id of ids) {
    const trimmed = id.trim();
    if (trimmed) unique.add(trimmed);
  }
  return Array.from(unique);
}

function toCleanText(value: string | null | undefined): string | null {
  if (value === null || value === undefined) return null;
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function normalizeList(values: string[] | null | undefined): string[] {
  if (!values?.length) return [];
  const unique = new Set<string>();
  for (const value of values) {
    const trimmed = value.trim();
    if (trimmed) unique.add(trimmed);
  }
  return Array.from(unique);
}

function normalizeListFromText(value: string | null | undefined): string[] {
  if (!value) return [];
  return value
    .split("\n")
    .map((item) => item.trim())
    .filter(Boolean);
}

function listToJson(values: string[]): string | null {
  return values.length > 0 ? JSON.stringify(values) : null;
}

function normalizeSpecsItems(payload: ProductPayload): Array<{ key: string; value: string; group: string | null; sortOrder: number }> {
  const specsFromItems = (payload.specsItems ?? [])
    .map((item, index) => ({
      key: item.key?.trim() ?? "",
      value: item.value?.trim() ?? "",
      group: toCleanText(item.group),
      sortOrder: Number.isFinite(item.sortOrder) ? Number(item.sortOrder) : index,
    }))
    .filter((item) => item.key && item.value);

  if (specsFromItems.length > 0) return specsFromItems;

  return normalizeListFromText(payload.specs)
    .map((line, index) => {
      const [rawKey, ...rawValueParts] = line.split(":");
      const key = (rawKey ?? "").trim();
      const value = rawValueParts.join(":").trim();
      return { key, value, group: null, sortOrder: index };
    })
    .filter((item) => item.key && item.value);
}

function normalizeGalleryImages(payload: ProductPayload): Array<{ url: string; alt: string | null; sortOrder: number }> {
  if (payload.galleryImages?.length) {
    const normalized = payload.galleryImages
      .map((item, index) => ({
        url: item.url?.trim() ?? "",
        alt: toCleanText(item.alt),
        sortOrder: Number.isFinite(item.sortOrder) ? Number(item.sortOrder) : index,
      }))
      .filter((item) => item.url);

    return normalized.sort((a, b) => a.sortOrder - b.sortOrder);
  }

  return (payload.imageUrls ?? [])
    .map((url, index) => ({ url: url.trim(), alt: null, sortOrder: index }))
    .filter((item) => item.url);
}

function calculateDiscountPercent(currentPrice: number | null, originalPrice: number | null): number | null {
  if (currentPrice === null || originalPrice === null) return null;
  if (originalPrice <= 0 || currentPrice < 0 || currentPrice >= originalPrice) return 0;
  return Math.round(((originalPrice - currentPrice) / originalPrice) * 100);
}

function validatePublishedProduct(payload: {
  name: string;
  slug: string;
  brandId: string;
  categoryId: string;
  shortDescription: string | null;
  fullDescription: string | null;
  thumbnail: string | null;
  thumbnailAlt: string | null;
  worthScore: number | null;
  currentPrice: number | null;
  originalPrice: number | null;
  verdictLabel: string | null;
  prosList: string[];
  consList: string[];
  specsItems: Array<{ key: string; value: string }>;
}) {
  const missing: string[] = [];

  if (!payload.name) missing.push("name");
  if (!payload.slug) missing.push("slug");
  if (!payload.brandId) missing.push("brandId");
  if (!payload.categoryId) missing.push("categoryId");
  if (!payload.shortDescription) missing.push("shortDescription");
  if (!payload.fullDescription) missing.push("fullDescription");
  if (!payload.thumbnail) missing.push("thumbnail");
  if (!payload.thumbnailAlt) missing.push("thumbnailAlt");
  if (payload.worthScore === null) missing.push("worthScore");
  if (!payload.verdictLabel) missing.push("verdictLabel");
  if (payload.currentPrice === null) missing.push("currentPrice");
  if (payload.originalPrice === null) missing.push("originalPrice");
  if (payload.prosList.length === 0) missing.push("prosList");
  if (payload.consList.length === 0) missing.push("consList");
  if (payload.specsItems.length === 0) missing.push("specsItems");

  if (missing.length > 0) {
    return `Published product thiếu field bắt buộc: ${missing.join(", ")}.`;
  }

  if (payload.currentPrice !== null && payload.currentPrice < 0) {
    return "Current price phải lớn hơn hoặc bằng 0.";
  }

  if (payload.originalPrice !== null && payload.originalPrice < 0) {
    return "Original price phải lớn hơn hoặc bằng 0.";
  }

  if (
    payload.currentPrice !== null &&
    payload.originalPrice !== null &&
    payload.currentPrice > payload.originalPrice
  ) {
    return "Current price không được lớn hơn original price.";
  }

  if (payload.worthScore !== null && (payload.worthScore < 0 || payload.worthScore > 10)) {
    return "Score phải nằm trong khoảng 0-10.";
  }

  return null;
}

function getOrderBy(sort: string, dir: string): Prisma.ProductOrderByWithRelationInput {
  const direction: Prisma.SortOrder = dir === "asc" ? "asc" : "desc";

  if (sort === "createdAt") return { createdAt: direction };
  if (sort === "price") return { priceMin: direction };
  if (sort === "score") return { worthScore: direction };

  return { updatedAt: direction };
}

function getDiscountPercent(item: Pick<ProductListItem, "priceMin" | "priceMax">): number {
  const currentPrice = item.priceMin;
  const originalPrice = item.priceMax;

  if (currentPrice === null || originalPrice === null || originalPrice <= 0) return 0;
  if (currentPrice <= 0 || currentPrice >= originalPrice) return 0;

  return ((originalPrice - currentPrice) / originalPrice) * 100;
}

function getAffiliateStatusRank(item: Pick<ProductListItem, "affiliateLinks">): number {
  const links = item.affiliateLinks;
  if (links.length === 0) return 0;

  const activeCount = links.filter((link) => link.status === "ACTIVE").length;
  if (activeCount > 0) return 2000 + activeCount;

  const inactiveCount = links.filter((link) => link.status === "INACTIVE").length;
  if (inactiveCount > 0) return 1000 + inactiveCount;

  return 1;
}

function sortByDirection(a: number, b: number, dir: string): number {
  return dir === "asc" ? a - b : b - a;
}

function sortProductsByComputedField(items: ProductListItem[], sort: string, dir: string): ProductListItem[] {
  return [...items].sort((a, b) => {
    const valueA = sort === "discount" ? getDiscountPercent(a) : getAffiliateStatusRank(a);
    const valueB = sort === "discount" ? getDiscountPercent(b) : getAffiliateStatusRank(b);

    const primaryCompare = sortByDirection(valueA, valueB, dir);
    if (primaryCompare !== 0) return primaryCompare;

    return b.updatedAt.getTime() - a.updatedAt.getTime();
  });
}

function pushAnd(where: Prisma.ProductWhereInput, condition: Prisma.ProductWhereInput) {
  const existingAnd = Array.isArray(where.AND) ? where.AND : [];
  where.AND = [...existingAnd, condition];
}

export async function GET(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;

  const { page, limit, skip } = normalizePaginationParams(request.nextUrl.searchParams);
  const q = request.nextUrl.searchParams.get("q")?.trim() ?? "";
  const status = request.nextUrl.searchParams.get("status") ?? "ALL";
  const filter = request.nextUrl.searchParams.get("filter") ?? "";
  const sort = request.nextUrl.searchParams.get("sort") ?? "updatedAt";
  const sortDir = request.nextUrl.searchParams.get("dir") ?? "desc";

  const where: Prisma.ProductWhereInput = {};
  if (q) {
    where.OR = [{ name: { contains: q } }, { slug: { contains: q } }];
  }
  if (status !== "ALL") {
    where.status = status as ProductStatus;
  }

  if (filter === "missing-image") {
    pushAnd(where, {
      OR: [{ thumbnail: null }, { images: { none: {} } }],
    });
  } else if (filter === "missing-specs") {
    pushAnd(where, {
      OR: [{ specs: null }, { specs: "" }],
    });
  } else if (filter === "missing-pros-cons") {
    pushAnd(where, {
      OR: [{ pros: null }, { pros: "" }, { cons: null }, { cons: "" }],
    });
  }

  if (sort === "discount" || sort === "affiliate") {
    const allItems = await prisma.product.findMany({
      where,
      include: PRODUCT_LIST_INCLUDE,
    });
    const sortedItems = sortProductsByComputedField(allItems, sort, sortDir);
    const pagedItems = sortedItems.slice(skip, skip + limit);
    return NextResponse.json(ok({ items: pagedItems, total: sortedItems.length, page, limit }));
  }

  const [items, total] = await Promise.all([
    prisma.product.findMany({
      where,
      include: PRODUCT_LIST_INCLUDE,
      orderBy: getOrderBy(sort, sortDir),
      skip,
      take: limit,
    }),
    prisma.product.count({ where }),
  ]);

  return NextResponse.json(ok({ items, total, page, limit }));
}

export async function POST(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;

  const body = (await request.json()) as ProductPayload;
  const name = body.name?.trim() ?? "";
  const slug = body.slug?.trim() ?? "";
  const brandId = body.brandId?.trim() ?? "";
  const categoryId = body.categoryId?.trim() ?? "";

  if (!name || !slug || !brandId || !categoryId) {
    return NextResponse.json(fail("Thiếu trường bắt buộc."), { status: 400 });
  }

  const existed = await prisma.product.findUnique({ where: { slug } });
  if (existed) {
    return NextResponse.json(fail("Slug đã tồn tại."), { status: 400 });
  }

  const tagIds = normalizeUniqueIds(body.tagIds);
  const galleryImages = normalizeGalleryImages(body);
  const specsItems = normalizeSpecsItems(body);
  const affiliateLinkIds = normalizeUniqueIds(body.affiliateLinkIds);
  const shortDescription = toCleanText(body.shortDescription);
  const fullDescription = toCleanText(body.fullDescription ?? body.description);
  const thumbnail = toCleanText(body.thumbnail) ?? galleryImages[0]?.url ?? null;
  const thumbnailAlt = toCleanText(body.thumbnailAlt);
  const currentPrice = body.currentPrice ?? body.priceMin ?? null;
  const originalPrice = body.originalPrice ?? body.priceMax ?? null;
  const priceRangeMin = body.priceRangeMin ?? body.priceMin ?? null;
  const priceRangeMax = body.priceRangeMax ?? body.priceMax ?? null;
  const worthScore = body.worthScore ?? null;
  const prosList = normalizeList(body.prosList ?? normalizeListFromText(body.pros));
  const consList = normalizeList(body.consList ?? normalizeListFromText(body.cons));
  const suitableForList = normalizeList(body.suitableForList ?? normalizeListFromText(body.bestFor));
  const notSuitableForList = normalizeList(body.notSuitableForList ?? normalizeListFromText(body.avoidIf));
  const praisedPoints = normalizeList(body.praisedPoints);
  const complainedPoints = normalizeList(body.complainedPoints);
  const verdictLabel = toCleanText(body.verdictLabel ?? body.verdict);

  if (body.status === "PUBLISHED") {
    const validationError = validatePublishedProduct({
      name,
      slug,
      brandId,
      categoryId,
      shortDescription,
      fullDescription,
      thumbnail,
      thumbnailAlt,
      worthScore,
      currentPrice,
      originalPrice,
      verdictLabel,
      prosList,
      consList,
      specsItems,
    });
    if (validationError) {
      return NextResponse.json(fail(validationError), { status: 400 });
    }
  }

  if (tagIds.length) {
    const validTagCount = await prisma.tag.count({ where: { id: { in: tagIds } } });
    if (validTagCount !== tagIds.length) {
      return NextResponse.json(fail("Danh sách tags không hợp lệ."), { status: 400 });
    }
  }

  if (affiliateLinkIds.length) {
    const validAffiliateLinkCount = await prisma.affiliateLink.count({
      where: { id: { in: affiliateLinkIds } },
    });
    if (validAffiliateLinkCount !== affiliateLinkIds.length) {
      return NextResponse.json(fail("Danh sách affiliate links không hợp lệ."), { status: 400 });
    }
  }

  const created = await prisma.product.create({
    data: {
      name,
      slug,
      brandId,
      categoryId,
      description: toCleanText(body.description) ?? fullDescription,
      shortDescription,
      fullDescription,
      badge: toCleanText(body.badge),
      thumbnail,
      thumbnailAlt,
      priceMin: priceRangeMin,
      priceMax: priceRangeMax,
      currentPrice,
      originalPrice,
      discountPercent: calculateDiscountPercent(currentPrice, originalPrice),
      priceRangeMin,
      priceRangeMax,
      currency: (toCleanText(body.currency) ?? "VND").toUpperCase(),
      priceUpdatedAt: body.priceUpdatedAt ? new Date(body.priceUpdatedAt) : null,
      rating: body.rating ?? null,
      soldCount: body.soldCount ?? null,
      worthScore,
      verdict: toCleanText(body.verdict),
      verdictLabel,
      shouldBuyIf: toCleanText(body.shouldBuyIf),
      considerIf: toCleanText(body.considerIf),
      avoidIfText: toCleanText(body.avoidIfText),
      buyUnderPrice: body.buyUnderPrice ?? null,
      considerAbovePrice: body.considerAbovePrice ?? null,
      finalVerdict: toCleanText(body.finalVerdict),
      pros: prosList.join("\n") || null,
      cons: consList.join("\n") || null,
      prosJson: listToJson(prosList),
      consJson: listToJson(consList),
      bestFor: suitableForList.join("\n") || null,
      avoidIf: notSuitableForList.join("\n") || null,
      suitableForJson: listToJson(suitableForList),
      notSuitableForJson: listToJson(notSuitableForList),
      specs:
        specsItems.length > 0
          ? specsItems.map((item) => `${item.key}: ${item.value}`).join("\n")
          : toCleanText(body.specs),
      praisedPointsJson: listToJson(praisedPoints),
      complainedPointsJson: listToJson(complainedPoints),
      sentimentScore: body.sentimentScore ?? null,
      insightNote: toCleanText(body.insightNote),
      dataSourceNote: toCleanText(body.dataSourceNote),
      seoTitle: toCleanText(body.seoTitle),
      seoDescription: toCleanText(body.seoDescription),
      seoOgImage: toCleanText(body.seoOgImage),
      canonicalUrl: toCleanText(body.canonicalUrl),
      noindex: Boolean(body.noindex),
      shopeeOriginalUrl: toCleanText(body.shopeeOriginalUrl),
      shopeeAffiliateUrl: toCleanText(body.shopeeAffiliateUrl),
      shopeeTrackingNote: toCleanText(body.shopeeTrackingNote),
      lazadaOriginalUrl: toCleanText(body.lazadaOriginalUrl),
      lazadaAffiliateUrl: toCleanText(body.lazadaAffiliateUrl),
      tikiOriginalUrl: toCleanText(body.tikiOriginalUrl),
      tikiAffiliateUrl: toCleanText(body.tikiAffiliateUrl),
      officialStoreUrl: toCleanText(body.officialStoreUrl),
      primaryPlatform: toCleanText(body.primaryPlatform),
      ctaLabel: toCleanText(body.ctaLabel),
      linkStatus: toCleanText(body.linkStatus),
      status: body.status ?? "DRAFT",
      productTags: tagIds.length ? { create: tagIds.map((tagId) => ({ tagId })) } : undefined,
      images: galleryImages.length
        ? {
            create: galleryImages.map((item, index) => ({
              url: item.url,
              alt: item.alt,
              sortOrder: Number.isFinite(item.sortOrder) ? item.sortOrder : index,
            })),
          }
        : undefined,
      productSpecs: specsItems.length
        ? {
            create: specsItems.map((item, index) => ({
              key: item.key,
              value: item.value,
              group: item.group,
              sortOrder: Number.isFinite(item.sortOrder) ? item.sortOrder : index,
            })),
          }
        : undefined,
      affiliateLinks: affiliateLinkIds.length
        ? {
            connect: affiliateLinkIds.map((id) => ({ id })),
          }
        : undefined,
    },
  });

  return NextResponse.json(ok(created));
}
