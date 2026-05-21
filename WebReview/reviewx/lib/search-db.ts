import { prisma } from "@/lib/prisma";

type SearchItem = {
  id: string;
  type: "product" | "review" | "deal" | "category";
  title: string;
  summary: string;
  href: string;
  score?: number;
};

export async function getSearchItemsFromDb(): Promise<SearchItem[]> {
  const [products, reviews, deals, categories] = await Promise.all([
    prisma.product.findMany({ take: 24, orderBy: { updatedAt: "desc" } }),
    prisma.review.findMany({ take: 24, orderBy: { updatedAt: "desc" } }),
    prisma.deal.findMany({ take: 24, orderBy: { updatedAt: "desc" }, include: { product: true } }),
    prisma.category.findMany({ take: 24, orderBy: { updatedAt: "desc" } }),
  ]);

  return [
    ...products.map((p: { slug: string; name: string; description: string | null; worthScore: number | null }) => ({ id: p.slug, type: "product" as const, title: p.name, summary: p.description ?? "Sản phẩm từ dữ liệu thực.", href: `/san-pham/${p.slug}`, score: p.worthScore ?? undefined })),
    ...reviews.map((r: { slug: string; title: string; summary: string | null; score: number | null }) => ({ id: r.slug, type: "review" as const, title: r.title, summary: r.summary ?? "Bài review từ dữ liệu thực.", href: `/review/${r.slug}`, score: r.score ?? undefined })),
    ...deals.map((d: { id: string; product: { name: string }; currentPrice: string; oldPrice: string }) => ({ id: d.id, type: "deal" as const, title: `Deal ${d.product.name}`, summary: `Giá ${d.currentPrice} (trước đó ${d.oldPrice})`, href: "/deals" })),
    ...categories.map((c: { slug: string; name: string; description: string | null }) => ({ id: c.slug, type: "category" as const, title: `Danh mục ${c.name}`, summary: c.description ?? "Danh mục từ dữ liệu thực.", href: `/danh-muc/${c.slug}` })),
  ];
}
