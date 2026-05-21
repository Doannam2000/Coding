import { NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { ok } from "@/lib/api-response";

export async function GET() {
  const categories = await prisma.category.findMany({
    select: {
      id: true,
      name: true,
      slug: true,
      icon: true,
      image: true,
      description: true,
      sortOrder: true,
      _count: { select: { products: true, reviews: true } },
    },
    orderBy: { sortOrder: "asc" },
  });

  const categoryIds = categories.map((c) => c.id);

  // Count deals per category
  const dealCounts = await prisma.deal.groupBy({
    by: ["productId"],
    where: { status: "ACTIVE" },
    _count: { _all: true },
  });

  // Map productId -> categoryId via products
  const products = await prisma.product.findMany({
    where: { categoryId: { in: categoryIds } },
    select: { id: true, categoryId: true },
  });

  const productToCategory = new Map(products.map((p) => [p.id, p.categoryId]));
  const categoryDealCount = new Map<string, number>();
  for (const entry of dealCounts) {
    const catId = productToCategory.get(entry.productId);
    if (catId) {
      categoryDealCount.set(catId, (categoryDealCount.get(catId) || 0) + entry._count._all);
    }
  }

  const items = categories.map((c) => ({
    id: c.id,
    name: c.name,
    slug: c.slug ?? c.name.toLowerCase().replace(/\s+/g, "-"),
    icon: c.icon ?? "",
    image: c.image ?? "",
    description: c.description ?? "",
    productCount: c._count.products,
    reviewCount: c._count.reviews,
    dealCount: categoryDealCount.get(c.id) ?? 0,
  }));

  return NextResponse.json(ok({ items }));
}
