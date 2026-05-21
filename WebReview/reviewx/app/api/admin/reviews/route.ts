import { NextRequest, NextResponse } from "next/server";
import type { Prisma } from "@prisma/client";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";
import { normalizePaginationParams } from "@/lib/validators";

type ReviewStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

type ReviewBlockType =
  | "heading"
  | "paragraph"
  | "image"
  | "spec_table"
  | "pros_cons"
  | "score_breakdown"
  | "cta_shopee"
  | "faq";

type ReviewBlock = {
  type?: ReviewBlockType;
  content?: string;
};

type FaqPayload = {
  question?: string;
  answer?: string;
  sortOrder?: number | null;
};

type ReviewPayload = {
  title?: string;
  slug?: string;
  productId?: string;
  categoryId?: string | null;
  author?: string | null;
  summary?: string | null;
  content?: string | null;
  contentBlocks?: ReviewBlock[];
  score?: number | null;
  status?: ReviewStatus;
  publishedAt?: string | null;
  readingTimeMinutes?: number | null;
  coverImage?: string | null;
  scoreBreakdown?: Record<string, number> | string | null;
  verdict?: string | null;
  pros?: string | null;
  cons?: string | null;
  faqText?: string | null;
  faqItems?: FaqPayload[];
  ctaBlocks?: string | null;
  relatedReviewIds?: string[];
  relatedProductIds?: string[];
  seoTitle?: string | null;
  seoDescription?: string | null;
  seoOgImage?: string | null;
  canonicalUrl?: string | null;
  noindex?: boolean;
};

function toCleanText(value: string | null | undefined): string | null {
  if (value === null || value === undefined) return null;
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function normalizeUniqueIds(ids: string[] | undefined): string[] {
  if (!ids?.length) return [];
  const unique = new Set<string>();
  for (const id of ids) {
    const trimmed = id.trim();
    if (trimmed) unique.add(trimmed);
  }
  return Array.from(unique);
}

function normalizeFaqItems(items: FaqPayload[] | undefined): Array<{ question: string; answer: string; sortOrder: number }> {
  if (!items?.length) return [];
  return items
    .map((item, index) => ({
      question: item.question?.trim() ?? "",
      answer: item.answer?.trim() ?? "",
      sortOrder: Number.isFinite(item.sortOrder) ? Number(item.sortOrder) : index,
    }))
    .filter((item) => item.question && item.answer)
    .sort((a, b) => a.sortOrder - b.sortOrder);
}

function normalizeScoreBreakdown(
  scoreBreakdown: ReviewPayload["scoreBreakdown"],
): string | null {
  if (scoreBreakdown === null || scoreBreakdown === undefined) return null;
  if (typeof scoreBreakdown === "string") {
    const trimmed = scoreBreakdown.trim();
    if (!trimmed) return null;
    const parsed = JSON.parse(trimmed);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      throw new Error("Score breakdown phải là JSON object hợp lệ.");
    }
    return JSON.stringify(parsed);
  }
  if (!scoreBreakdown || Array.isArray(scoreBreakdown)) {
    throw new Error("Score breakdown phải là object hợp lệ.");
  }
  return JSON.stringify(scoreBreakdown);
}

function normalizeContentBlocks(blocks: ReviewBlock[] | undefined): string | null {
  if (!blocks?.length) return null;
  const normalized = blocks
    .map((block) => ({
      type: block.type,
      content: block.content?.trim() ?? "",
    }))
    .filter((block) => Boolean(block.type) && block.content);
  if (normalized.length === 0) return null;
  return JSON.stringify(normalized);
}

function normalizeReadingTimeMinutes(value: number | null | undefined): number | null {
  if (value === null || value === undefined) return null;
  if (!Number.isFinite(value)) return null;
  const normalized = Math.floor(value);
  return normalized > 0 ? normalized : null;
}

function normalizePublishedAt(value: string | null | undefined): Date | null {
  if (!value) return null;
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return null;
  return parsed;
}

function validatePublishedReview(payload: {
  title: string;
  slug: string;
  productId: string;
  summary: string | null;
  content: string | null;
  score: number | null;
  author: string | null;
}) {
  const missing: string[] = [];
  if (!payload.title) missing.push("title");
  if (!payload.slug) missing.push("slug");
  if (!payload.productId) missing.push("productId");
  if (!payload.summary) missing.push("summary");
  if (!payload.content) missing.push("content");
  if (payload.score === null) missing.push("score");
  if (!payload.author) missing.push("author");
  if (missing.length > 0) {
    return `Published review thiếu field bắt buộc: ${missing.join(", ")}.`;
  }
  if (payload.score !== null && (payload.score < 0 || payload.score > 10)) {
    return "Score phải trong khoảng 0-10.";
  }
  return null;
}

function getOrderBy(sort: string, dir: string): Prisma.ReviewOrderByWithRelationInput {
  const direction: Prisma.SortOrder = dir === "asc" ? "asc" : "desc";
  if (sort === "publishedAt") return { publishedAt: direction };
  if (sort === "score") return { score: direction };
  return { updatedAt: direction };
}

export async function GET(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;

  const { page, limit, skip } = normalizePaginationParams(request.nextUrl.searchParams);
  const q = request.nextUrl.searchParams.get("q")?.trim() ?? "";
  const status = request.nextUrl.searchParams.get("status") ?? "ALL";
  const categoryId = request.nextUrl.searchParams.get("categoryId")?.trim() ?? "";
  const author = request.nextUrl.searchParams.get("author")?.trim() ?? "";
  const sort = request.nextUrl.searchParams.get("sort") ?? "updatedAt";
  const dir = request.nextUrl.searchParams.get("dir") ?? "desc";

  const where: Prisma.ReviewWhereInput = {};

  if (q) {
    where.OR = [
      { title: { contains: q } },
      { slug: { contains: q } },
      { product: { name: { contains: q } } },
    ];
  }
  if (status !== "ALL") where.status = status as ReviewStatus;
  if (categoryId) where.categoryId = categoryId;
  if (author) where.author = { contains: author };

  const [items, total] = await Promise.all([
    prisma.review.findMany({
      where,
      include: {
        product: { select: { id: true, name: true } },
        category: { select: { id: true, name: true } },
      },
      orderBy: getOrderBy(sort, dir),
      skip,
      take: limit,
    }),
    prisma.review.count({ where }),
  ]);

  return NextResponse.json(ok({ items, total, page, limit }));
}

export async function POST(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;

  const body = (await request.json()) as ReviewPayload;
  const title = body.title?.trim() ?? "";
  const slug = body.slug?.trim() ?? "";
  const productId = body.productId?.trim() ?? "";
  const status = body.status ?? "DRAFT";

  if (!title || !slug || !productId) {
    return NextResponse.json(fail("Thiếu trường bắt buộc."), { status: 400 });
  }

  const existed = await prisma.review.findUnique({ where: { slug } });
  if (existed) return NextResponse.json(fail("Slug đã tồn tại."), { status: 400 });

  const normalizedAuthor = toCleanText(body.author) ?? "ReviewX";
  const normalizedSummary = toCleanText(body.summary);
  const normalizedContent = toCleanText(body.content);
  const normalizedScore = body.score ?? null;
  const publishedValidationError =
    status === "PUBLISHED"
      ? validatePublishedReview({
          title,
          slug,
          productId,
          summary: normalizedSummary,
          content: normalizedContent,
          score: normalizedScore,
          author: normalizedAuthor,
        })
      : null;
  if (publishedValidationError) {
    return NextResponse.json(fail(publishedValidationError), { status: 400 });
  }

  const faqItems = normalizeFaqItems(body.faqItems);
  const relatedReviewIds = normalizeUniqueIds(body.relatedReviewIds);
  const relatedProductIds = normalizeUniqueIds(body.relatedProductIds);

  let scoreBreakdown: string | null = null;
  try {
    scoreBreakdown = normalizeScoreBreakdown(body.scoreBreakdown);
  } catch (error) {
    return NextResponse.json(
      fail(error instanceof Error ? error.message : "Score breakdown không hợp lệ."),
      { status: 400 },
    );
  }

  const created = await prisma.review.create({
    data: {
      title,
      slug,
      productId,
      categoryId: toCleanText(body.categoryId),
      author: normalizedAuthor,
      summary: normalizedSummary,
      content: normalizedContent,
      contentBlocks: normalizeContentBlocks(body.contentBlocks),
      score: normalizedScore,
      status,
      publishedAt:
        status === "PUBLISHED"
          ? normalizePublishedAt(body.publishedAt) ?? new Date()
          : normalizePublishedAt(body.publishedAt),
      readingTimeMinutes: normalizeReadingTimeMinutes(body.readingTimeMinutes),
      coverImage: toCleanText(body.coverImage),
      scoreBreakdown,
      verdict: toCleanText(body.verdict),
      pros: toCleanText(body.pros),
      cons: toCleanText(body.cons),
      faqText: toCleanText(body.faqText),
      ctaBlocks: toCleanText(body.ctaBlocks),
      relatedReviewIds: relatedReviewIds.length ? JSON.stringify(relatedReviewIds) : null,
      relatedProductIds: relatedProductIds.length ? JSON.stringify(relatedProductIds) : null,
      seoTitle: toCleanText(body.seoTitle),
      seoDescription: toCleanText(body.seoDescription),
      seoOgImage: toCleanText(body.seoOgImage),
      canonicalUrl: toCleanText(body.canonicalUrl),
      noindex: Boolean(body.noindex),
      faqs: faqItems.length
        ? {
            create: faqItems.map((item) => ({
              question: item.question,
              answer: item.answer,
              sortOrder: item.sortOrder,
            })),
          }
        : undefined,
    },
    include: { faqs: { orderBy: { sortOrder: "asc" } } },
  });

  return NextResponse.json(ok(created));
}
