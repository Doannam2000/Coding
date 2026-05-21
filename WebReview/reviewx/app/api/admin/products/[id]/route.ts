import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";

type ProductStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

type ProductPatchPayload = {
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

const PRODUCT_DETAIL_INCLUDE = {
  brand: true,
  category: true,
  productTags: { include: { tag: true } },
  images: { orderBy: { sortOrder: "asc" } },
  productSpecs: { orderBy: { sortOrder: "asc" } },
  affiliateLinks: true,
} as const;

function normalizeUniqueIds(ids: string[] | undefined): string[] | undefined {
  if (!ids) return undefined;
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

function listFromStored(jsonValue: string | null | undefined, fallbackText: string | null | undefined): string[] {
  if (jsonValue) {
    try {
      const parsed = JSON.parse(jsonValue);
      if (Array.isArray(parsed)) {
        return parsed.map((item) => String(item).trim()).filter(Boolean);
      }
    } catch {
      // ignore parse error and fallback to plain text
    }
  }
  return normalizeListFromText(fallbackText);
}

function normalizeSpecsItems(payload: ProductPatchPayload): Array<{ key: string; value: string; group: string | null; sortOrder: number }> {
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

function normalizeGalleryImages(payload: ProductPatchPayload): Array<{ url: string; alt: string | null; sortOrder: number }> {
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

export async function GET(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const item = await prisma.product.findUnique({
    where: { id },
    include: PRODUCT_DETAIL_INCLUDE,
  });
  if (!item) return NextResponse.json(fail("Không tìm thấy sản phẩm."), { status: 404 });
  return NextResponse.json(ok(item));
}

export async function PATCH(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const body = (await request.json()) as ProductPatchPayload;

  const exists = await prisma.product.findUnique({
    where: { id },
    include: {
      images: { orderBy: { sortOrder: "asc" } },
      productSpecs: { orderBy: { sortOrder: "asc" } },
    },
  });
  if (!exists) return NextResponse.json(fail("Không tìm thấy sản phẩm."), { status: 404 });

  const nextSlug = body.slug !== undefined ? body.slug.trim() : exists.slug;
  if (!nextSlug) {
    return NextResponse.json(fail("Slug là bắt buộc."), { status: 400 });
  }

  if (nextSlug !== exists.slug) {
    const sameSlug = await prisma.product.findUnique({ where: { slug: nextSlug } });
    if (sameSlug && sameSlug.id !== id) {
      return NextResponse.json(fail("Slug đã tồn tại."), { status: 400 });
    }
  }

  const tagIds = normalizeUniqueIds(body.tagIds);
  const affiliateLinkIds = normalizeUniqueIds(body.affiliateLinkIds);
  const nextGalleryImages =
    body.galleryImages !== undefined || body.imageUrls !== undefined ? normalizeGalleryImages(body) : null;
  const nextSpecsItems =
    body.specsItems !== undefined || body.specs !== undefined ? normalizeSpecsItems(body) : null;

  if (tagIds && tagIds.length) {
    const validTagCount = await prisma.tag.count({ where: { id: { in: tagIds } } });
    if (validTagCount !== tagIds.length) {
      return NextResponse.json(fail("Danh sách tags không hợp lệ."), { status: 400 });
    }
  }

  if (affiliateLinkIds && affiliateLinkIds.length) {
    const validAffiliateLinkCount = await prisma.affiliateLink.count({
      where: { id: { in: affiliateLinkIds } },
    });
    if (validAffiliateLinkCount !== affiliateLinkIds.length) {
      return NextResponse.json(fail("Danh sách affiliate links không hợp lệ."), { status: 400 });
    }
  }

  const nextProsList =
    body.prosList !== undefined || body.pros !== undefined
      ? normalizeList(body.prosList ?? normalizeListFromText(body.pros))
      : listFromStored(exists.prosJson, exists.pros);
  const nextConsList =
    body.consList !== undefined || body.cons !== undefined
      ? normalizeList(body.consList ?? normalizeListFromText(body.cons))
      : listFromStored(exists.consJson, exists.cons);
  const nextSuitableForList =
    body.suitableForList !== undefined || body.bestFor !== undefined
      ? normalizeList(body.suitableForList ?? normalizeListFromText(body.bestFor))
      : listFromStored(exists.suitableForJson, exists.bestFor);
  const nextNotSuitableForList =
    body.notSuitableForList !== undefined || body.avoidIf !== undefined
      ? normalizeList(body.notSuitableForList ?? normalizeListFromText(body.avoidIf))
      : listFromStored(exists.notSuitableForJson, exists.avoidIf);
  const nextPraisedPoints =
    body.praisedPoints !== undefined
      ? normalizeList(body.praisedPoints)
      : listFromStored(exists.praisedPointsJson, null);
  const nextComplainedPoints =
    body.complainedPoints !== undefined
      ? normalizeList(body.complainedPoints)
      : listFromStored(exists.complainedPointsJson, null);

  const nextName = body.name !== undefined ? body.name.trim() : exists.name;
  const nextBrandId = body.brandId !== undefined ? body.brandId.trim() : exists.brandId;
  const nextCategoryId = body.categoryId !== undefined ? body.categoryId.trim() : exists.categoryId;
  const nextShortDescription =
    body.shortDescription !== undefined ? toCleanText(body.shortDescription) : exists.shortDescription;
  const nextFullDescription =
    body.fullDescription !== undefined
      ? toCleanText(body.fullDescription)
      : body.description !== undefined
        ? toCleanText(body.description)
        : exists.fullDescription ?? exists.description;
  const nextThumbnail =
    body.thumbnail !== undefined
      ? toCleanText(body.thumbnail)
      : nextGalleryImages !== null
        ? nextGalleryImages[0]?.url ?? null
        : exists.thumbnail;
  const nextThumbnailAlt =
    body.thumbnailAlt !== undefined
      ? toCleanText(body.thumbnailAlt)
      : nextGalleryImages !== null
        ? nextGalleryImages[0]?.alt ?? exists.thumbnailAlt
        : exists.thumbnailAlt;
  const nextWorthScore = body.worthScore !== undefined ? body.worthScore : exists.worthScore;
  const nextCurrentPrice =
    body.currentPrice !== undefined
      ? body.currentPrice
      : body.priceMin !== undefined
        ? body.priceMin
        : exists.currentPrice ?? exists.priceMin;
  const nextOriginalPrice =
    body.originalPrice !== undefined
      ? body.originalPrice
      : body.priceMax !== undefined
        ? body.priceMax
        : exists.originalPrice ?? exists.priceMax;
  const nextPriceRangeMin =
    body.priceRangeMin !== undefined
      ? body.priceRangeMin
      : body.priceMin !== undefined
        ? body.priceMin
        : exists.priceRangeMin ?? exists.priceMin;
  const nextPriceRangeMax =
    body.priceRangeMax !== undefined
      ? body.priceRangeMax
      : body.priceMax !== undefined
        ? body.priceMax
        : exists.priceRangeMax ?? exists.priceMax;
  const nextVerdictLabel =
    body.verdictLabel !== undefined ? toCleanText(body.verdictLabel) : exists.verdictLabel ?? exists.verdict;
  const resolvedSpecs =
    nextSpecsItems !== null
      ? nextSpecsItems
      : exists.productSpecs.map((spec) => ({
          key: spec.key,
          value: spec.value,
          group: spec.group,
          sortOrder: spec.sortOrder,
        }));
  const nextStatus = body.status ?? exists.status;

  if (nextStatus === "PUBLISHED") {
    const validationError = validatePublishedProduct({
      name: nextName,
      slug: nextSlug,
      brandId: nextBrandId,
      categoryId: nextCategoryId,
      shortDescription: nextShortDescription,
      fullDescription: nextFullDescription,
      thumbnail: nextThumbnail,
      thumbnailAlt: nextThumbnailAlt,
      worthScore: nextWorthScore,
      currentPrice: nextCurrentPrice,
      originalPrice: nextOriginalPrice,
      verdictLabel: nextVerdictLabel,
      prosList: nextProsList,
      consList: nextConsList,
      specsItems: resolvedSpecs,
    });
    if (validationError) {
      return NextResponse.json(fail(validationError), { status: 400 });
    }
  }

  const updated = await prisma.$transaction(async (tx) => {
    if (tagIds) {
      await tx.productTag.deleteMany({ where: { productId: id } });
      if (tagIds.length) {
        await tx.productTag.createMany({
          data: tagIds.map((tagId) => ({ productId: id, tagId })),
        });
      }
    }

    if (nextGalleryImages !== null) {
      await tx.productImage.deleteMany({ where: { productId: id } });
      if (nextGalleryImages.length) {
        await tx.productImage.createMany({
          data: nextGalleryImages.map((item, index) => ({
            productId: id,
            url: item.url,
            alt: item.alt,
            sortOrder: Number.isFinite(item.sortOrder) ? item.sortOrder : index,
          })),
        });
      }
    }

    if (nextSpecsItems !== null) {
      await tx.productSpec.deleteMany({ where: { productId: id } });
      if (nextSpecsItems.length) {
        await tx.productSpec.createMany({
          data: nextSpecsItems.map((item, index) => ({
            productId: id,
            key: item.key,
            value: item.value,
            group: item.group,
            sortOrder: Number.isFinite(item.sortOrder) ? item.sortOrder : index,
          })),
        });
      }
    }

    return tx.product.update({
      where: { id },
      data: {
        name: nextName,
        slug: nextSlug,
        brandId: nextBrandId,
        categoryId: nextCategoryId,
        description:
          body.description !== undefined
            ? toCleanText(body.description)
            : exists.description ?? nextFullDescription,
        shortDescription: nextShortDescription,
        fullDescription: nextFullDescription,
        badge: body.badge !== undefined ? toCleanText(body.badge) : exists.badge,
        thumbnail: nextThumbnail,
        thumbnailAlt: nextThumbnailAlt,
        priceMin: nextPriceRangeMin,
        priceMax: nextPriceRangeMax,
        currentPrice: nextCurrentPrice,
        originalPrice: nextOriginalPrice,
        discountPercent: calculateDiscountPercent(nextCurrentPrice, nextOriginalPrice),
        priceRangeMin: nextPriceRangeMin,
        priceRangeMax: nextPriceRangeMax,
        currency:
          body.currency !== undefined
            ? (toCleanText(body.currency) ?? "VND").toUpperCase()
            : exists.currency,
        priceUpdatedAt:
          body.priceUpdatedAt !== undefined
            ? body.priceUpdatedAt
              ? new Date(body.priceUpdatedAt)
              : null
            : exists.priceUpdatedAt,
        rating: body.rating !== undefined ? body.rating : exists.rating,
        soldCount: body.soldCount !== undefined ? body.soldCount : exists.soldCount,
        worthScore: nextWorthScore,
        verdict: body.verdict !== undefined ? toCleanText(body.verdict) : exists.verdict,
        verdictLabel: nextVerdictLabel,
        shouldBuyIf:
          body.shouldBuyIf !== undefined ? toCleanText(body.shouldBuyIf) : exists.shouldBuyIf,
        considerIf: body.considerIf !== undefined ? toCleanText(body.considerIf) : exists.considerIf,
        avoidIfText:
          body.avoidIfText !== undefined ? toCleanText(body.avoidIfText) : exists.avoidIfText,
        buyUnderPrice:
          body.buyUnderPrice !== undefined ? body.buyUnderPrice : exists.buyUnderPrice,
        considerAbovePrice:
          body.considerAbovePrice !== undefined
            ? body.considerAbovePrice
            : exists.considerAbovePrice,
        finalVerdict:
          body.finalVerdict !== undefined ? toCleanText(body.finalVerdict) : exists.finalVerdict,
        pros: nextProsList.join("\n") || null,
        cons: nextConsList.join("\n") || null,
        prosJson: listToJson(nextProsList),
        consJson: listToJson(nextConsList),
        bestFor: nextSuitableForList.join("\n") || null,
        avoidIf: nextNotSuitableForList.join("\n") || null,
        suitableForJson: listToJson(nextSuitableForList),
        notSuitableForJson: listToJson(nextNotSuitableForList),
        specs:
          resolvedSpecs.length > 0
            ? resolvedSpecs.map((item) => `${item.key}: ${item.value}`).join("\n")
            : null,
        praisedPointsJson: listToJson(nextPraisedPoints),
        complainedPointsJson: listToJson(nextComplainedPoints),
        sentimentScore:
          body.sentimentScore !== undefined ? body.sentimentScore : exists.sentimentScore,
        insightNote:
          body.insightNote !== undefined ? toCleanText(body.insightNote) : exists.insightNote,
        dataSourceNote:
          body.dataSourceNote !== undefined
            ? toCleanText(body.dataSourceNote)
            : exists.dataSourceNote,
        seoTitle: body.seoTitle !== undefined ? toCleanText(body.seoTitle) : exists.seoTitle,
        seoDescription:
          body.seoDescription !== undefined
            ? toCleanText(body.seoDescription)
            : exists.seoDescription,
        seoOgImage:
          body.seoOgImage !== undefined ? toCleanText(body.seoOgImage) : exists.seoOgImage,
        canonicalUrl:
          body.canonicalUrl !== undefined
            ? toCleanText(body.canonicalUrl)
            : exists.canonicalUrl,
        noindex: body.noindex !== undefined ? Boolean(body.noindex) : exists.noindex,
        shopeeOriginalUrl:
          body.shopeeOriginalUrl !== undefined
            ? toCleanText(body.shopeeOriginalUrl)
            : exists.shopeeOriginalUrl,
        shopeeAffiliateUrl:
          body.shopeeAffiliateUrl !== undefined
            ? toCleanText(body.shopeeAffiliateUrl)
            : exists.shopeeAffiliateUrl,
        shopeeTrackingNote:
          body.shopeeTrackingNote !== undefined
            ? toCleanText(body.shopeeTrackingNote)
            : exists.shopeeTrackingNote,
        lazadaOriginalUrl:
          body.lazadaOriginalUrl !== undefined
            ? toCleanText(body.lazadaOriginalUrl)
            : exists.lazadaOriginalUrl,
        lazadaAffiliateUrl:
          body.lazadaAffiliateUrl !== undefined
            ? toCleanText(body.lazadaAffiliateUrl)
            : exists.lazadaAffiliateUrl,
        tikiOriginalUrl:
          body.tikiOriginalUrl !== undefined
            ? toCleanText(body.tikiOriginalUrl)
            : exists.tikiOriginalUrl,
        tikiAffiliateUrl:
          body.tikiAffiliateUrl !== undefined
            ? toCleanText(body.tikiAffiliateUrl)
            : exists.tikiAffiliateUrl,
        officialStoreUrl:
          body.officialStoreUrl !== undefined
            ? toCleanText(body.officialStoreUrl)
            : exists.officialStoreUrl,
        primaryPlatform:
          body.primaryPlatform !== undefined
            ? toCleanText(body.primaryPlatform)
            : exists.primaryPlatform,
        ctaLabel: body.ctaLabel !== undefined ? toCleanText(body.ctaLabel) : exists.ctaLabel,
        linkStatus:
          body.linkStatus !== undefined ? toCleanText(body.linkStatus) : exists.linkStatus,
        status: nextStatus,
        ...(affiliateLinkIds
          ? { affiliateLinks: { set: affiliateLinkIds.map((linkId) => ({ id: linkId })) } }
          : {}),
      },
      include: PRODUCT_DETAIL_INCLUDE,
    });
  });

  return NextResponse.json(ok(updated));
}

export async function DELETE(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;

  const exists = await prisma.product.findUnique({
    where: { id },
    include: {
      _count: {
        select: {
          reviews: true,
          deals: true,
          affiliateLinks: true,
          clickEvents: true,
        },
      },
    },
  });

  if (!exists) {
    return NextResponse.json(fail("Không tìm thấy sản phẩm."), { status: 404 });
  }

  if (
    exists._count.reviews > 0 ||
    exists._count.deals > 0 ||
    exists._count.affiliateLinks > 0 ||
    exists._count.clickEvents > 0
  ) {
    return NextResponse.json(
      fail("Không thể xóa sản phẩm đang có review/deal/affiliate/click liên quan. Hãy archive hoặc migrate trước."),
      { status: 400 },
    );
  }

  await prisma.$transaction(async (tx) => {
    await tx.productTag.deleteMany({ where: { productId: id } });
    await tx.productImage.deleteMany({ where: { productId: id } });
    await tx.productSpec.deleteMany({ where: { productId: id } });
    await tx.productSource.deleteMany({ where: { productId: id } });
    await tx.comparisonItem.deleteMany({ where: { productId: id } });
    await tx.product.delete({ where: { id } });
  });

  return NextResponse.json(ok({ deleted: true }));
}
