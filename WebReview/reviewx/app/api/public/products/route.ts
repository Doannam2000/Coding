import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { fail, ok } from "@/lib/api-response";

type UpsertBody = {
  id?: string;
  name?: string;
  slug?: string;
  brandId?: string;
  categoryId?: string;
  thumbnail?: string | null;
  description?: string | null;
  worthScore?: number | null;
  verdict?: string | null;
  pros?: string[] | string | null;
  cons?: string[] | string | null;
  bestFor?: string[] | string | null;
  avoidIf?: string[] | string | null;
  specs?: string[] | string | null;
  status?: "DRAFT" | "PUBLISHED" | "ARCHIVED";
};

function parseItems(raw: string | null | undefined): string[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return raw.split("\n").filter(Boolean).slice(0, 20);
  }
}

function toStoredList(value: string[] | string | null | undefined) {
  if (Array.isArray(value)) return JSON.stringify(value);
  return value ?? null;
}

export async function GET(request: NextRequest) {
  const slug = request.nextUrl.searchParams.get("slug")?.trim() ?? "";
  if (!slug) {
    return NextResponse.json(fail("Thieu slug."), { status: 400 });
  }

  const product = await prisma.product.findUnique({
    where: { slug },
    select: {
      id: true,
      slug: true,
      name: true,
      thumbnail: true,
      description: true,
      priceMin: true,
      priceMax: true,
      rating: true,
      soldCount: true,
      worthScore: true,
      verdict: true,
      pros: true,
      cons: true,
      bestFor: true,
      avoidIf: true,
      specs: true,
      status: true,
      createdAt: true,
      updatedAt: true,
      brand: { select: { id: true, name: true, slug: true, logo: true } },
      category: { select: { id: true, name: true, slug: true } },
      images: { orderBy: { sortOrder: "asc" } },
    },
  });

  if (!product) {
    return NextResponse.json(fail("Khong tim thay san pham."), { status: 404 });
  }

  const [reviews, relatedDeals] = await Promise.all([
    prisma.review.findMany({
      where: { productId: product.id, status: "PUBLISHED" },
      take: 3,
      select: { id: true, slug: true, title: true, score: true, coverImage: true },
    }),
    prisma.deal.findMany({
      where: { productId: product.id, status: "Active", endTime: { gte: new Date() } },
      take: 3,
      select: { id: true, discount: true, currentPrice: true, oldPrice: true },
    }),
  ]);

  const data = {
    id: product.id,
    slug: product.slug,
    name: product.name,
    brand: product.brand?.name ?? "",
    brandSlug: product.brand?.slug ?? "",
    category: product.category?.name ?? "",
    categorySlug: product.category?.slug ?? "",
    description: product.description ?? "",
    priceMin: product.priceMin,
    priceMax: product.priceMax,
    priceRange: null as string | null,
    score: product.worthScore ?? 0,
    verdict: product.verdict ?? "",
    buyIf: product.verdict ?? "",
    considerIf: null as string | null,
    avoidIf: null as string | null,
    buyPriceHint: null as string | null,
    considerPriceHint: null as string | null,
    pros: parseItems(product.pros),
    cons: parseItems(product.cons),
    bestFor: parseItems(product.bestFor),
    avoidFor: parseItems(product.avoidIf),
    specs: parseItems(product.specs),
    images: product.images.map((image) => image.url),
    coverImage: product.images[0]?.url ?? product.thumbnail ?? "",
    status: product.status,
    soldCount: product.soldCount ?? 0,
    createdAt: product.createdAt.toISOString(),
    updatedAt: product.updatedAt.toISOString(),
    reviews: reviews.map((review) => ({
      id: review.id,
      slug: review.slug,
      title: review.title,
      score: review.score ?? 0,
      coverImage: review.coverImage ?? "",
    })),
    deals: relatedDeals.map((deal) => ({
      id: deal.id,
      title: deal.discount,
      currentPrice: deal.currentPrice,
      oldPrice: deal.oldPrice,
      discount: deal.discount,
      href: `/go/deal/${deal.id}`,
      platform: "Shopee",
    })),
  };

  return NextResponse.json(ok(data));
}

export async function POST(request: NextRequest) {
  const body = (await request.json().catch(() => null)) as UpsertBody | null;
  if (!body?.name || !body.slug) {
    return NextResponse.json(fail("Du lieu khong hop le."), { status: 400 });
  }

  const existing = await prisma.product.findUnique({ where: { slug: body.slug } });
  if (existing && body.id && existing.id !== body.id) {
    return NextResponse.json(fail("Slug da ton tai."), { status: 409 });
  }

  try {
    const data = await prisma.product.upsert({
      where: { slug: body.slug },
      create: {
        name: body.name,
        slug: body.slug,
        brandId: body.brandId ?? "",
        categoryId: body.categoryId ?? "",
        thumbnail: body.thumbnail ?? null,
        description: body.description ?? null,
        worthScore: body.worthScore ?? null,
        verdict: body.verdict ?? null,
        pros: toStoredList(body.pros),
        cons: toStoredList(body.cons),
        bestFor: toStoredList(body.bestFor),
        avoidIf: toStoredList(body.avoidIf),
        specs: toStoredList(body.specs),
        status: body.status ?? "DRAFT",
      },
      update: {
        name: body.name,
        thumbnail: body.thumbnail ?? null,
        description: body.description ?? null,
        worthScore: body.worthScore ?? null,
        verdict: body.verdict ?? null,
        pros: toStoredList(body.pros),
        cons: toStoredList(body.cons),
        bestFor: toStoredList(body.bestFor),
        avoidIf: toStoredList(body.avoidIf),
        specs: toStoredList(body.specs),
        status: body.status,
      },
    });

    return NextResponse.json(ok(data));
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Loi khi luu san pham.";
    return NextResponse.json(fail(message), { status: 500 });
  }
}
