import type { MetadataRoute } from "next";
import { prisma } from "@/lib/prisma";

function normalizeBaseUrl() {
  const fallback = "https://reviewx.vn";
  const fromEnv = process.env.NEXT_PUBLIC_SITE_URL?.trim();
  if (!fromEnv) return fallback;
  return fromEnv.endsWith("/") ? fromEnv.slice(0, -1) : fromEnv;
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const baseUrl = normalizeBaseUrl();
  const now = new Date();

  const [products, reviews, categories] = await Promise.all([
    prisma.product.findMany({
      where: { status: "PUBLISHED" },
      select: { slug: true, updatedAt: true },
      orderBy: { updatedAt: "desc" },
      take: 1000,
    }),
    prisma.review.findMany({
      where: { status: "PUBLISHED" },
      select: { slug: true, updatedAt: true },
      orderBy: { updatedAt: "desc" },
      take: 1000,
    }),
    prisma.category.findMany({
      where: { status: "PUBLISHED" },
      select: { slug: true, updatedAt: true },
      orderBy: { updatedAt: "desc" },
      take: 1000,
    }),
  ]);

  const staticPages: MetadataRoute.Sitemap = [
    { url: `${baseUrl}/`, lastModified: now },
    { url: `${baseUrl}/deals`, lastModified: now },
    { url: `${baseUrl}/so-sanh`, lastModified: now },
    { url: `${baseUrl}/tim-kiem`, lastModified: now },
    { url: `${baseUrl}/cong-cu/chon-san-pham`, lastModified: now },
    { url: `${baseUrl}/danh-muc`, lastModified: now },
    { url: `${baseUrl}/about`, lastModified: now },
    { url: `${baseUrl}/affiliate-policy`, lastModified: now },
    { url: `${baseUrl}/contact`, lastModified: now },
  ];

  const categoryPages: MetadataRoute.Sitemap = categories.map((category) => ({
    url: `${baseUrl}/danh-muc/${category.slug}`,
    lastModified: category.updatedAt,
  }));

  const productPages: MetadataRoute.Sitemap = products.map((product) => ({
    url: `${baseUrl}/san-pham/${product.slug}`,
    lastModified: product.updatedAt,
  }));

  const reviewPages: MetadataRoute.Sitemap = reviews.map((review) => ({
    url: `${baseUrl}/review/${review.slug}`,
    lastModified: review.updatedAt,
  }));

  return [...staticPages, ...categoryPages, ...productPages, ...reviewPages];
}
