import { NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { ok } from "@/lib/api-response";

export async function GET() {
  const products = await prisma.product.findMany({
    where: { status: "PUBLISHED", worthScore: { not: null } },
    select: {
      id: true,
      slug: true,
      name: true,
      thumbnail: true,
      pros: true,
      cons: true,
      bestFor: true,
      avoidIf: true,
      specs: true,
      worthScore: true,
      category: { select: { name: true } },
      images: { take: 1, orderBy: { sortOrder: "asc" } },
    },
    orderBy: { worthScore: "desc" },
    take: 10,
  });

  const top = products[0];
  if (!top) {
    return NextResponse.json(ok(null));
  }

  function parseItems(raw: string | null | undefined): string[] {
    if (!raw) return [];
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return raw.split("\n").filter(Boolean).slice(0, 10);
    }
  }

  const item = {
    id: top.id,
    slug: top.slug,
    name: top.name,
    score: top.worthScore ?? 0,
    coverImage: top.images[0]?.url ?? top.thumbnail ?? "",
    category: top.category?.name ?? "Công nghệ",
    pros: parseItems(top.pros),
    cons: parseItems(top.cons),
    bestFor: parseItems(top.bestFor),
    avoidFor: parseItems(top.avoidIf),
    deals: [] as Array<{ id: string; title: string; currentPrice: string; oldPrice: string; discount: string; href: string; platform: string }>,
    reviews: [] as Array<{ id: string; slug: string; title: string; score: number; coverImage: string }>,
  };

  return NextResponse.json(ok(item));
}
