"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { ProductCard } from "@/components/product-card";
import { ReviewCard } from "@/components/review-card";
import { DealCard } from "@/components/deal-card";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer } from "@/components/ui";

type ProductStatus = "dang-mua" | "can-nhac" | "khong-khuyen-nghi";

type CategoryProduct = {
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
  status: ProductStatus;
  image: string;
};

type CategoryReview = {
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

type CategoryDeal = {
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

type CategoryPageClientProps = {
  slug: string;
  categoryFromDb: { slug: string; name: string; icon: string; description: string };
  productsFromDb?: CategoryProduct[];
  reviewsFromDb?: CategoryReview[];
  dealsFromDb?: CategoryDeal[];
};

const categoryIcons: Record<string, string> = {
  "cong-nghe": "💻",
  "gia-dung": "🏠",
  "lam-dep": "💄",
  "me-va-be": "👶",
  "nha-cua": "🏡",
  "do-bep": "🍳",
  "gaming": "🎮",
  "the-thao": "⚽",
  "sach": "📚",
};

export default function CategoryPageClient({
  slug,
  categoryFromDb,
  productsFromDb = [],
  reviewsFromDb = [],
  dealsFromDb = [],
}: CategoryPageClientProps) {
  const [priceRange, setPriceRange] = useState("all");
  const [minRating, setMinRating] = useState("all");
  const [minSold, setMinSold] = useState("all");
  const [minWorth, setMinWorth] = useState("all");
  const [brand, setBrand] = useState("all");
  const [mallOnly, setMallOnly] = useState(false);
  const [useCase, setUseCase] = useState("all");
  const [sortBy, setSortBy] = useState("worth");
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);
  const [visibleCount, setVisibleCount] = useState(3);
  const [isLoading] = useState(false);
  const [loadError] = useState("");

  const category = useMemo(() => ({
    slug: categoryFromDb.slug,
    icon: categoryFromDb.icon || categoryIcons[slug] || "🛒",
    name: categoryFromDb.name,
    description: categoryFromDb.description || "",
    productCount: productsFromDb.length,
    seoIntro: categoryFromDb.description || "",
    popularSubcategories: [] as string[],
    products: productsFromDb,
    reviews: reviewsFromDb,
    deals: dealsFromDb,
  }), [categoryFromDb, productsFromDb, reviewsFromDb, dealsFromDb]);

  const filteredReviews = useMemo(() => category.reviews, [category]);
  const filteredDeals = useMemo(() => category.deals, [category]);
  const categoryProducts = category.products;
  const hasAnyCategoryContent = categoryProducts.length > 0 || filteredReviews.length > 0 || filteredDeals.length > 0;
  const brands = Array.from(new Set(categoryProducts.map((p) => p.brand)));
  const useCases = Array.from(new Set(categoryProducts.map((p) => p.useCase)));

  const selectedFilters = [
    priceRange !== "all" ? `Gia: ${priceRange}` : null,
    minRating !== "all" ? `Rating: ${minRating}+` : null,
    minSold !== "all" ? `Da ban: ${minSold}+` : null,
    minWorth !== "all" ? `Worth: ${minWorth}+` : null,
    brand !== "all" ? `Brand: ${brand}` : null,
    mallOnly ? "Shopee Mall" : null,
    useCase !== "all" ? `Use case: ${useCase}` : null,
  ].filter(Boolean) as string[];

  const filteredProducts = (() => {
    let products = [...categoryProducts];
    if (priceRange !== "all") {
      products = products.filter((p) => {
        if (priceRange === "duoi-500") return p.price < 500000;
        if (priceRange === "500-1m") return p.price >= 500000 && p.price <= 1000000;
        if (priceRange === "tren-1m") return p.price > 1000000;
        return true;
      });
    }
    if (minRating !== "all") products = products.filter((p) => p.rating >= Number(minRating));
    if (minSold !== "all") products = products.filter((p) => p.soldCount >= Number(minSold));
    if (minWorth !== "all") products = products.filter((p) => p.worthScore >= Number(minWorth));
    if (brand !== "all") products = products.filter((p) => p.brand === brand);
    if (mallOnly) products = products.filter((p) => p.shopeeMall);
    if (useCase !== "all") products = products.filter((p) => p.useCase === useCase);
    if (sortBy === "worth") products.sort((a, b) => b.worthScore - a.worthScore);
    if (sortBy === "price-low") products.sort((a, b) => a.price - b.price);
    if (sortBy === "rating") products.sort((a, b) => b.rating - a.rating);
    if (sortBy === "sold") products.sort((a, b) => b.soldCount - a.soldCount);
    if (sortBy === "updated") products.sort((a, b) => a.name.localeCompare(b.name));
    return products;
  })();

  function resetFilters() {
    setPriceRange("all");
    setMinRating("all");
    setMinSold("all");
    setMinWorth("all");
    setBrand("all");
    setMallOnly(false);
    setUseCase("all");
    setSortBy("worth");
  }

  function applyFiltersMobile() {
    setMobileFilterOpen(false);
  }

  if (loadError) {
    return (
      <PageContainer>
        <ErrorState title="Khong tai duoc danh muc" message={loadError} />
      </PageContainer>
    );
  }

  if (isLoading) {
    return (
      <PageContainer>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <LoadingSkeleton className="h-56 w-full rounded-2xl" />
          <LoadingSkeleton className="h-56 w-full rounded-2xl" />
          <LoadingSkeleton className="h-56 w-full rounded-2xl" />
        </div>
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <section className="overflow-hidden rounded-3xl border border-slate-200/70 bg-white/90 p-5 shadow-sm sm:p-8">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-bold text-slate-900">{category.name}</h1>
        </div>

        <div className="mb-6 flex flex-wrap items-center gap-2">
          <select value={priceRange} onChange={(e) => setPriceRange(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-2 text-sm">
            <option value="all">Gia</option>
            <option value="duoi-500">Duoi 500k</option>
            <option value="500-1m">500k - 1 trieu</option>
            <option value="tren-1m">Tren 1 trieu</option>
          </select>
          <select value={brand} onChange={(e) => setBrand(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-2 text-sm">
            <option value="all">Brand</option>
            {brands.map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-2 text-sm">
            <option value="worth">Dang mua nhat</option>
            <option value="price-low">Gia thap</option>
            <option value="rating">Rating cao</option>
            <option value="sold">Ban chay</option>
            <option value="updated">Moi cap nhat</option>
          </select>
        </div>

        {selectedFilters.length > 0 && (
          <div className="mb-4 flex flex-wrap gap-2">
            {selectedFilters.map((f) => <span key={f} className="rounded-full bg-blue-100 px-3 py-1 text-xs text-blue-700">{f}</span>)}
          </div>
        )}

        <button className="lg:hidden rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700" onClick={() => setMobileFilterOpen(true)}>Bo loc</button>

        {mobileFilterOpen && (
          <div className="fixed inset-0 z-50 bg-black/30 p-4 md:hidden" onClick={() => setMobileFilterOpen(false)}>
            <div className="mx-auto mt-10 max-w-md rounded-2xl border border-slate-200 bg-white p-4" onClick={(e) => e.stopPropagation()}>
              <p className="text-sm font-semibold text-slate-900">Bo loc di dong</p>
              <div className="mt-3 grid gap-2">
                <select value={priceRange} onChange={(e) => setPriceRange(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-3 text-sm"><option value="all">Gia</option><option value="duoi-500">Duoi 500k</option><option value="500-1m">500k - 1 trieu</option><option value="tren-1m">Tren 1 trieu</option></select>
                <select value={brand} onChange={(e) => setBrand(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-3 text-sm"><option value="all">Brand</option>{brands.map((item) => <option key={item} value={item}>{item}</option>)}</select>
                <select value={sortBy} onChange={(e) => setSortBy(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-3 text-sm"><option value="worth">Dang mua nhat</option><option value="price-low">Gia thap</option><option value="rating">Rating cao</option><option value="sold">Ban chay</option><option value="updated">Moi cap nhat</option></select>
              </div>
              <div className="mt-4 grid grid-cols-2 gap-2">
                <button onClick={applyFiltersMobile} className="rounded-xl bg-blue-600 px-3 py-3 text-sm font-semibold text-white">Apply</button>
                <button onClick={resetFilters} className="rounded-xl border border-slate-200 bg-white px-3 py-3 text-sm font-semibold text-slate-700">Clear</button>
              </div>
            </div>
          </div>
        )}

        {!hasAnyCategoryContent ? (
          <section className="mt-8"><EmptyState title="Danh muc chua co noi dung" message="Hien chua co san pham, review hoac deal cong khai trong danh muc nay." /></section>
        ) : null}

        <section className="mt-8">
          <h2 className="mb-3 text-xl font-semibold text-slate-900">San pham trong danh muc</h2>
          {filteredProducts.length === 0 ? (
            <EmptyState title="Khong co san pham phu hop" message="Hay noi dieu kien loc de xem them ket qua." />
          ) : (
            <div className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {filteredProducts.slice(0, visibleCount).map((product) => (
                  <ProductCard key={product.id} id={product.id} name={product.name} image={product.image} brand={product.brand} useCase={product.useCase} priceLabel={product.priceLabel} score={product.worthScore} shopeeMall={product.shopeeMall} />
                ))}
              </div>
              {visibleCount < filteredProducts.length && (
                <button onClick={() => setVisibleCount((count) => count + 3)} className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700">
                  Xem them
                </button>
              )}
            </div>
          )}
        </section>

        <section className="mt-8">
          <h2 className="mb-3 text-xl font-semibold text-slate-900">Review noi bat</h2>
          {filteredReviews.length === 0 ? (
            <EmptyState title="Chua co review" message="Danh muc nay chua co review cong khai." />
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {filteredReviews.map((review) => <ReviewCard key={review.slug} {...review} />)}
            </div>
          )}
        </section>

        <section className="mt-8">
          <h2 className="mb-3 text-xl font-semibold text-slate-900">Deal lien quan</h2>
          {filteredDeals.length === 0 ? (
            <EmptyState title="Chua co deal" message="Danh muc nay chua co deal phu hop." />
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {filteredDeals.map((deal) => <DealCard key={deal.id} {...deal} />)}
            </div>
          )}
        </section>
        </section>
        </PageContainer>
    );
  }