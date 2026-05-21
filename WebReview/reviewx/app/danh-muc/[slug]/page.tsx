import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { prisma } from "@/lib/prisma";
import CategoryPageClient from "./page-client";

export const dynamic = "force-dynamic";

type CategoryRouteParams = {
  params: Promise<{ slug: string }>;
};

type CategoryProductFromDb = {
  id: string;
  name: string;
  price: number;
  priceLabel: string;
  rating: number;
  soldCount: number;
  worthScore: number;
  brand: string;
  shopeeMall: boolean;
  useCase: string;
  status: "dang-mua" | "can-nhac" | "khong-khuyen-nghi";
  image: string;
};

type CategoryReviewFromDb = {
  slug: string;
  title: string;
  excerpt: string;
  category: string;
  score: number;
  author: string;
  publishedDate: string;
  updatedDate: string;
  readingTime: string;
  coverImage: string;
};

type CategoryDealFromDb = {
  id: string;
  productName: string;
  image: string;
  category: string;
  platform: string;
  discountPercent: number;
  currentPriceLabel: string;
  oldPriceLabel: string;
  expiryLabel: string;
  endTime: string;
  isExpired: boolean;
  hasAffiliate: boolean;
  hasCoupon?: boolean;
  couponCode?: string;
};

const FALLBACK_PRODUCT_IMAGE =
  "https://images.unsplash.com/photo-1519389950473-47ba0277781c?q=80&w=1200&auto=format&fit=crop";
const FALLBACK_REVIEW_IMAGE =
  "https://images.unsplash.com/photo-1545127398-14699f92334b?q=80&w=1200&auto=format&fit=crop";
const FALLBACK_DEAL_IMAGE =
  "https://images.unsplash.com/photo-1484704849700-f032a568e944?q=80&w=1200&auto=format&fit=crop";

function scoreToStatus(score: number): "dang-mua" | "can-nhac" | "khong-khuyen-nghi" {
  if (score >= 8) return "dang-mua";
  if (score >= 6) return "can-nhac";
  return "khong-khuyen-nghi";
}

function parseFirstListItem(raw: string | null): string {
  if (!raw) return "";
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return String(parsed[0] ?? "");
  } catch {}
  return raw.split("\n").map((item) => item.trim()).filter(Boolean)[0] ?? "";
}

function formatPriceLabel(price: number): string {
  if (price <= 0) return "â€”";
  return `${price.toLocaleString("vi-VN")}Ä‘`;
}

function formatDate(date: Date): string {
  return date.toISOString().split("T")[0] ?? "";
}

function parsePriceToNumber(raw: string | null): number {
  if (!raw) return 0;
  const digits = raw.replace(/[^\d]/g, "");
  return digits ? Number.parseInt(digits, 10) : 0;
}

async function getCategoryBySlug(slug: string) {
  return prisma.category.findUnique({
    where: { slug },
    select: {
      id: true,
      slug: true,
      name: true,
      icon: true,
      description: true,
      seoTitle: true,
      seoDescription: true,
    },
  });
}

async function hasCategorySlug(slug: string): Promise<boolean> {
  const category = await prisma.category.findUnique({
    where: { slug },
    select: { slug: true },
  });
  return Boolean(category?.slug);
}

export async function generateMetadata({ params }: CategoryRouteParams): Promise<Metadata> {
  const { slug } = await params;
  const category = await getCategoryBySlug(slug);

  if (!category) {
    notFound();
  }

  const title = category.seoTitle?.trim() || `${category.name} | ReviewX`;
  const description = category.seoDescription?.trim() || category.description?.trim() || `Danh má»¥c ${category.name} trÃªn ReviewX.`;

  return {
    title,
    description,
    alternates: { canonical: `/danh-muc/${category.slug}` },
    openGraph: {
      title,
      description,
      url: `/danh-muc/${category.slug}`,
    },
  };
}

export default async function CategoryPage({ params }: CategoryRouteParams) {
  const { slug } = await params;
  const exists = await hasCategorySlug(slug);
  if (!exists) {
    notFound();
  }
  const category = await getCategoryBySlug(slug);

  if (!category) {
    notFound();
  }

  const dbProducts = await prisma.product.findMany({
    where: {
      categoryId: category.id,
      status: "PUBLISHED",
      slug: { not: "" },
    },
    include: {
      brand: { select: { name: true } },
      images: { select: { url: true, sortOrder: true }, orderBy: { sortOrder: "asc" } },
    },
    orderBy: { updatedAt: "desc" },
  });

  const productsFromDb: CategoryProductFromDb[] = dbProducts
    .filter((product) => product.slug.trim().length > 0)
    .map((product) => {
      const price = product.currentPrice ?? product.priceMin ?? product.priceMax ?? 0;
      const worthScore = product.worthScore ?? 0;
      const useCase = parseFirstListItem(product.bestFor) || "Tá»•ng há»£p";

      return {
        id: product.slug,
        name: product.name,
        price,
        priceLabel: formatPriceLabel(price),
        rating: product.rating ?? 0,
        soldCount: product.soldCount ?? 0,
        worthScore,
        brand: product.brand?.name ?? "",
        shopeeMall: (product.primaryPlatform ?? "").toLowerCase().includes("shopee"),
        useCase,
        status: scoreToStatus(worthScore),
        image: product.images[0]?.url ?? product.thumbnail ?? FALLBACK_PRODUCT_IMAGE,
      };
    });

  const dbReviews = await prisma.review.findMany({
    where: {
      status: "PUBLISHED",
      slug: { not: "" },
      OR: [{ categoryId: category.id }, { product: { categoryId: category.id } }],
    },
    include: {
      category: { select: { name: true } },
    },
    orderBy: { updatedAt: "desc" },
    take: 24,
  });

  const reviewsFromDb: CategoryReviewFromDb[] = dbReviews
    .filter((review) => review.slug.trim().length > 0)
    .map((review) => {
      const minutes = Math.max(1, Math.ceil((review.content?.length ?? review.summary?.length ?? 0) / 500));
      return {
        slug: review.slug,
        title: review.title,
        excerpt: review.summary ?? "",
        category: review.category?.name ?? category.name,
        score: review.score ?? 0,
        author: review.author ?? "BiÃªn táº­p ReviewX",
        publishedDate: review.publishedAt ? formatDate(review.publishedAt) : formatDate(review.createdAt),
        updatedDate: formatDate(review.updatedAt),
        readingTime: `${minutes} phÃºt Ä‘á»c`,
        coverImage: review.coverImage ?? FALLBACK_REVIEW_IMAGE,
      };
    });

  const dbDeals = await prisma.deal.findMany({
    where: { product: { categoryId: category.id } },
    include: {
      product: { select: { name: true, thumbnail: true, category: { select: { name: true } } } },
      affiliateLink: { select: { id: true, platform: true, status: true, affiliateUrl: true } },
    },
    orderBy: { updatedAt: "desc" },
    take: 24,
  });

  const dealsFromDb: CategoryDealFromDb[] = dbDeals.map((deal) => {
    const currentPriceNum = parsePriceToNumber(deal.currentPrice);
    const oldPriceNum = parsePriceToNumber(deal.oldPrice);
    const discountPercent =
      oldPriceNum > 0 && currentPriceNum > 0 && currentPriceNum < oldPriceNum
        ? Math.round(((oldPriceNum - currentPriceNum) / oldPriceNum) * 100)
        : parsePriceToNumber(deal.discount);
    const endTime = deal.endTime.toISOString();
    const isExpired = (deal.status ?? "").toLowerCase() !== "active";

    return {
      id: deal.id,
      productName: deal.product?.name ?? "Sáº£n pháº©m",
      image: deal.product?.thumbnail ?? FALLBACK_DEAL_IMAGE,
      category: deal.product?.category?.name ?? category.name,
      platform: deal.affiliateLink?.platform ?? "Shopee",
      discountPercent: Math.max(0, discountPercent),
      currentPriceLabel: formatPriceLabel(currentPriceNum),
      oldPriceLabel: formatPriceLabel(oldPriceNum),
      expiryLabel: isExpired ? "Deal Ä‘Ã£ háº¿t háº¡n" : `Háº¿t háº¡n: ${deal.endTime.toLocaleString("vi-VN")}`,
      endTime,
      isExpired,
      hasAffiliate: Boolean(deal.affiliateLink?.id && deal.affiliateLink.status === "ACTIVE" && deal.affiliateLink.affiliateUrl),
      hasCoupon: Boolean(deal.couponCode),
      couponCode: deal.couponCode ?? undefined,
    };
  });

  return (
    <CategoryPageClient
      slug={category.slug}
      categoryFromDb={{
        slug: category.slug,
        name: category.name,
        icon: category.icon ?? "",
        description: category.description ?? "",
      }}
      productsFromDb={productsFromDb}
      reviewsFromDb={reviewsFromDb}
      dealsFromDb={dealsFromDb}
    />
  );
}


