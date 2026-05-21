import { NextRequest, NextResponse } from "next/server";
import type { Prisma } from "@prisma/client";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";

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

type ReviewPatchPayload = {
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
  scoreBreakdown: ReviewPatchPayload["scoreBreakdown"],
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

export async function GET(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const row = await prisma.review.findUnique({
    where: { id },
    include: {
      product: { select: { id: true, name: true } },
      category: { select: { id: true, name: true } },
      faqs: { orderBy: { sortOrder: "asc" } },
    },
  });
  if (!row) return NextResponse.json(fail("Không tìm thấy review."), { status: 404 });
  return NextResponse.json(ok(row));
}

export async function DELETE(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;

  const row = await prisma.review.findUnique({ where: { id } });
  if (!row) return NextResponse.json(fail("Không tìm thấy review."), { status: 404 });

  await prisma.$transaction(async (tx) => {
    await tx.fAQ.deleteMany({ where: { reviewId: id } });
    await tx.reviewInsight.deleteMany({ where: { reviewId: id } });
    await tx.affiliateLink.updateMany({
      where: { reviewId: id },
      data: { reviewId: null },
    });
    await tx.clickEvent.updateMany({
      where: { reviewId: id },
      data: { reviewId: null },
    });
    await tx.review.delete({ where: { id } });
  });
  return NextResponse.json(ok({ id }));
}

export async function PATCH(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const body = (await request.json()) as ReviewPatchPayload;
  const exists = await prisma.review.findUnique({
    where: { id },
    include: { faqs: true },
  });
  if (!exists) return NextResponse.json(fail("Không tìm thấy review."), { status: 404 });

  const nextSlug = body.slug !== undefined ? body.slug.trim() : exists.slug;
  if (!nextSlug) return NextResponse.json(fail("Slug là bắt buộc."), { status: 400 });

  if (nextSlug !== exists.slug) {
    const sameSlug = await prisma.review.findUnique({ where: { slug: nextSlug } });
    if (sameSlug && sameSlug.id !== id) {
      return NextResponse.json(fail("Slug đã tồn tại."), { status: 400 });
    }
  }

  const nextTitle = body.title !== undefined ? body.title.trim() : exists.title;
  const nextProductId = body.productId !== undefined ? body.productId.trim() : exists.productId;
  const nextAuthor = body.author !== undefined ? toCleanText(body.author) : exists.author;
  const nextSummary = body.summary !== undefined ? toCleanText(body.summary) : exists.summary;
  const nextContent = body.content !== undefined ? toCleanText(body.content) : exists.content;
  const nextScore = body.score !== undefined ? body.score : exists.score;
  const nextStatus = body.status ?? exists.status;

  if (nextStatus === "PUBLISHED") {
    const validationError = validatePublishedReview({
      title: nextTitle,
      slug: nextSlug,
      productId: nextProductId,
      summary: nextSummary,
      content: nextContent,
      score: nextScore,
      author: nextAuthor,
    });
    if (validationError) {
      return NextResponse.json(fail(validationError), { status: 400 });
    }
  }

  let normalizedScoreBreakdown: string | null | undefined;
  try {
    normalizedScoreBreakdown =
      body.scoreBreakdown !== undefined ? normalizeScoreBreakdown(body.scoreBreakdown) : undefined;
  } catch (error) {
    return NextResponse.json(
      fail(error instanceof Error ? error.message : "Score breakdown không hợp lệ."),
      { status: 400 },
    );
  }

  const faqItems = body.faqItems !== undefined ? normalizeFaqItems(body.faqItems) : null;
  const relatedReviewIds =
    body.relatedReviewIds !== undefined ? normalizeUniqueIds(body.relatedReviewIds) : null;
  const relatedProductIds =
    body.relatedProductIds !== undefined ? normalizeUniqueIds(body.relatedProductIds) : null;

  const data: Prisma.ReviewUncheckedUpdateInput = {
    title: nextTitle,
    slug: nextSlug,
    productId: nextProductId,
    categoryId:
      body.categoryId !== undefined ? toCleanText(body.categoryId) : exists.categoryId,
    author: nextAuthor,
    summary: nextSummary,
    content: nextContent,
    score: nextScore,
    status: nextStatus,
    publishedAt:
      body.publishedAt !== undefined
        ? normalizePublishedAt(body.publishedAt)
        : nextStatus === "PUBLISHED" && !exists.publishedAt
          ? new Date()
          : exists.publishedAt,
    readingTimeMinutes:
      body.readingTimeMinutes !== undefined
        ? normalizeReadingTimeMinutes(body.readingTimeMinutes)
        : exists.readingTimeMinutes,
    coverImage:
      body.coverImage !== undefined ? toCleanText(body.coverImage) : exists.coverImage,
    scoreBreakdown:
      normalizedScoreBreakdown !== undefined ? normalizedScoreBreakdown : exists.scoreBreakdown,
    verdict: body.verdict !== undefined ? toCleanText(body.verdict) : exists.verdict,
    pros: body.pros !== undefined ? toCleanText(body.pros) : exists.pros,
    cons: body.cons !== undefined ? toCleanText(body.cons) : exists.cons,
    faqText: body.faqText !== undefined ? toCleanText(body.faqText) : exists.faqText,
    ctaBlocks: body.ctaBlocks !== undefined ? toCleanText(body.ctaBlocks) : exists.ctaBlocks,
    contentBlocks:
      body.contentBlocks !== undefined ? normalizeContentBlocks(body.contentBlocks) : exists.contentBlocks,
    relatedReviewIds:
      relatedReviewIds !== null
        ? relatedReviewIds.length
          ? JSON.stringify(relatedReviewIds)
          : null
        : exists.relatedReviewIds,
    relatedProductIds:
      relatedProductIds !== null
        ? relatedProductIds.length
          ? JSON.stringify(relatedProductIds)
          : null
        : exists.relatedProductIds,
    seoTitle: body.seoTitle !== undefined ? toCleanText(body.seoTitle) : exists.seoTitle,
    seoDescription:
      body.seoDescription !== undefined
        ? toCleanText(body.seoDescription)
        : exists.seoDescription,
    seoOgImage:
      body.seoOgImage !== undefined ? toCleanText(body.seoOgImage) : exists.seoOgImage,
    canonicalUrl:
      body.canonicalUrl !== undefined ? toCleanText(body.canonicalUrl) : exists.canonicalUrl,
    noindex: body.noindex !== undefined ? Boolean(body.noindex) : exists.noindex,
  };

  const updated = await prisma.$transaction(async (tx) => {
    if (faqItems !== null) {
      await tx.fAQ.deleteMany({ where: { reviewId: id } });
      if (faqItems.length) {
        await tx.fAQ.createMany({
          data: faqItems.map((item) => ({
            reviewId: id,
            question: item.question,
            answer: item.answer,
            sortOrder: item.sortOrder,
          })),
        });
      }
    }

    return tx.review.update({
      where: { id },
      data,
      include: {
        product: { select: { id: true, name: true } },
        category: { select: { id: true, name: true } },
        faqs: { orderBy: { sortOrder: "asc" } },
      },
    });
  });

  return NextResponse.json(ok(updated));
}
