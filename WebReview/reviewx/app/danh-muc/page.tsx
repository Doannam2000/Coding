import { CategoryCard } from "@/components/category-card";
import { EmptyState, PageContainer } from "@/components/ui";
import { prisma } from "@/lib/prisma";

export const dynamic = "force-dynamic";

export default async function CategoriesPage() {
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

  if (categories.length === 0) {
    return (
      <PageContainer>
        <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
          <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Danh mục</h1>
          <p className="mt-3 text-sm leading-6 text-slate-600">
            Khám phá các nhóm sản phẩm theo từng danh mục. Dữ liệu danh mục được đồng bộ trực tiếp từ cơ sở dữ liệu.
          </p>
          <div className="mt-6">
            <EmptyState title="Chưa có danh mục" message="Hiện chưa có danh mục nào được xuất bản. Vui lòng quay lại sau." />
          </div>
        </section>
      </PageContainer>
    );
  }

  const categoryIds = categories.map((category) => category.id);
  const products = await prisma.product.findMany({
    where: { categoryId: { in: categoryIds } },
    select: { id: true, categoryId: true },
  });
  const deals = await prisma.deal.groupBy({
    by: ["productId"],
    where: { status: "ACTIVE" },
    _count: { _all: true },
  });

  const productToCategory = new Map(products.map((item) => [item.id, item.categoryId]));
  const categoryDealCount = new Map<string, number>();

  for (const row of deals) {
    const categoryId = productToCategory.get(row.productId);
    if (!categoryId) continue;
    categoryDealCount.set(categoryId, (categoryDealCount.get(categoryId) ?? 0) + row._count._all);
  }

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Danh mục</h1>
        <p className="mt-3 text-sm leading-6 text-slate-600">
          Khám phá các nhóm sản phẩm theo từng danh mục. Dữ liệu danh mục được đồng bộ trực tiếp từ cơ sở dữ liệu.
        </p>
        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {categories.map((category) => (
            <CategoryCard
              key={category.id}
              id={category.id}
              slug={category.slug}
              name={category.name}
              icon={category.icon}
              image={category.image}
              description={category.description}
              productCount={category._count.products}
              reviewCount={category._count.reviews}
              dealCount={categoryDealCount.get(category.id) ?? 0}
            />
          ))}
        </div>
      </section>
    </PageContainer>
  );
}
