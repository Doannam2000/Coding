import { NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { ok } from "@/lib/api-response";

export async function GET() {
  const reviews = await prisma.review.findMany({
    where: { status: "PUBLISHED" },
    include: {
      product: { select: { name: true } },
      category: { select: { name: true } },
    },
    orderBy: { publishedAt: "desc" },
    take: 6,
  });

  const items = reviews.map((review) => ({
    slug: review.slug,
    title: review.title,
    excerpt: review.summary ?? review.title,
    category: review.category?.name ?? "",
    score: review.score,
    author: review.author ?? "ReviewX",
    publishedDate: review.publishedAt
      ? new Date(review.publishedAt).toLocaleDateString("vi-VN", {
          day: "2-digit",
          month: "2-digit",
          year: "numeric",
        })
      : "—",
    updatedDate: new Date(review.updatedAt).toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    }),
    readingTime: `${Math.max(1, review.readingTimeMinutes ?? 5)} phút đọc`,
    coverImage: review.coverImage ?? "",
  }));

  return NextResponse.json(ok({ items }));
}
