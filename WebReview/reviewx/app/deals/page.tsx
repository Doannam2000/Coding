"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { DealCard } from "@/components/deal-card";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer, SectionHeader } from "@/components/ui";

type DealItem = {
  id: string;
  productName: string;
  image: string;
  category: string;
  platform: string;
  currentPriceLabel: string;
  oldPriceLabel: string;
  discountPercent: number;
  expiryLabel: string;
  endTime: string;
  isExpired: boolean;
  hasAffiliate: boolean;
  hasCoupon: boolean;
  couponCode?: string;
};

function parseDiscount(discount: string) {
  const m = discount.match(/\d+/);
  return m ? Number(m[0]) : 0;
}

export default function DealsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [deals, setDeals] = useState<DealItem[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [platforms, setPlatforms] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);

  const searchQuery = searchParams.get("q") ?? "";
  const categoryFilter = searchParams.get("category") ?? "all";
  const platformFilter = searchParams.get("platform") ?? "all";
  const discountFilter = searchParams.get("minDiscount") ?? "all";
  const minPrice = searchParams.get("minPrice") ?? "";
  const maxPrice = searchParams.get("maxPrice") ?? "";
  const statusFilter = (searchParams.get("status") ?? "active") as "all" | "active" | "expired";
  const couponFilter = searchParams.get("couponOnly") === "true";
  const sortBy = (searchParams.get("sort") ?? "newest") as "newest" | "biggest-discount" | "lowest-price" | "ending-soon";

  function updateQuery(updates: Record<string, string | null>) {
    const params = new URLSearchParams(searchParams.toString());
    Object.entries(updates).forEach(([key, value]) => {
      if (value === null || value === "" || value === "all" || value === "false") {
        params.delete(key);
      } else {
        params.set(key, value);
      }
    });
    router.push(`/deals?${params.toString()}`, { scroll: false });
  }

  async function loadDeals() {
    setIsLoading(true);
    setError("");
    try {
      const params = new URLSearchParams();
      params.set("limit", "100");
      params.set("page", "1");
      if (searchQuery) params.set("q", searchQuery);
      if (categoryFilter !== "all") params.set("category", categoryFilter);
      if (platformFilter !== "all") params.set("platform", platformFilter);
      if (statusFilter !== "all") params.set("status", statusFilter);
      if (discountFilter !== "all") params.set("minDiscount", discountFilter);
      if (minPrice) params.set("minPrice", minPrice);
      if (maxPrice) params.set("maxPrice", maxPrice);
      if (couponFilter) params.set("couponOnly", "true");
      if (sortBy) params.set("sort", sortBy);

      const response = await fetch(`/api/public/deals?${params.toString()}`);
      const payload = await response.json();

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.error ?? "Không tải được deals");
      }

      const now = Date.now();
      const mapped = (payload.data.items ?? []).map((item: any) => {
        const end = item.endTime ? new Date(item.endTime) : null;
        const isExpired = end ? end.getTime() < now : false;
        return {
          id: item.id,
          productName: item.productName ?? item.name ?? "Sản phẩm",
          image: item.image ?? "https://images.unsplash.com/photo-1546868871-7041f2a55e12?q=80&w=1200&auto=format&fit=crop",
          category: item.category ?? "Khác",
          platform: item.platform || "Shopee",
          currentPriceLabel: item.currentPrice ?? "—",
          oldPriceLabel: item.oldPrice ?? "",
          discountPercent: parseDiscount(item.discount ?? ""),
          expiryLabel: isExpired ? "Deal đã hết hạn" : end ? `Hết hạn: ${end.toLocaleString("vi-VN")}` : "—",
          endTime: item.endTime ?? "",
          isExpired,
          hasAffiliate: Boolean(item.affiliateUrl || item.internalUrl),
          hasCoupon: Boolean(item.couponCode),
          couponCode: item.couponCode ?? undefined,
        } as DealItem;
      });

      setDeals(mapped);
      const uniqueCategories: string[] = Array.from(new Set<string>(mapped.map((d: DealItem) => d.category)));
      const uniquePlatforms: string[] = Array.from(new Set<string>(mapped.map((d: DealItem) => d.platform)));
      setCategories(uniqueCategories);
      setPlatforms(uniquePlatforms);
    } catch (e: any) {
      setError(e.message || "Không tải được deals");
      setDeals([]);
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    loadDeals();
  }, [searchQuery, categoryFilter, platformFilter, discountFilter, minPrice, maxPrice, statusFilter, couponFilter, sortBy]);

  function resetFilters() {
    router.push("/deals");
  }

  const activeFilterChips = [
    searchQuery ? `Từ khóa: ${searchQuery}` : null,
    categoryFilter !== "all" ? `Danh mục: ${categoryFilter}` : null,
    platformFilter !== "all" ? `Nền tảng: ${platformFilter}` : null,
    discountFilter !== "all" ? `Giảm từ ${discountFilter}%` : null,
    minPrice ? `Giá từ ${minPrice}` : null,
    maxPrice ? `Giá đến ${maxPrice}` : null,
    statusFilter !== "all" ? (statusFilter === "active" ? "Đang hoạt động" : "Đã hết hạn") : null,
    couponFilter ? "Có coupon" : null,
  ].filter(Boolean) as string[];

  function removeChip(chip: string) {
    if (chip.startsWith("Từ khóa:")) updateQuery({ q: null });
    if (chip.startsWith("Danh mục:")) updateQuery({ category: null });
    if (chip.startsWith("Nền tảng:")) updateQuery({ platform: null });
    if (chip.startsWith("Giảm từ")) updateQuery({ minDiscount: null });
    if (chip.startsWith("Giá từ")) updateQuery({ minPrice: null });
    if (chip.startsWith("Giá đến")) updateQuery({ maxPrice: null });
    if (chip === "Đang hoạt động" || chip === "Đã hết hạn") updateQuery({ status: null });
    if (chip === "Có coupon") updateQuery({ couponOnly: null });
  }

  const itemListSchema = {
    "@context": "https://schema.org",
    "@type": "ItemList",
    name: "Deal Shopee hot",
    itemListElement: deals.map((deal, index) => ({
      "@type": "ListItem",
      position: index + 1,
      url: `https://reviewx.vn/go/deal/${deal.id}`,
      name: deal.productName,
    })),
  };

  return (
    <PageContainer>
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(itemListSchema) }} />
      <section className="rounded-3xl border border-slate-200/70 bg-gradient-to-r from-orange-100 via-pink-100 to-blue-100 p-6 shadow-sm sm:p-8">
        <SectionHeader title="Deal Shopee hot" subtitle="Tổng hợp deal nổi bật theo danh mục, mức giảm và thời điểm cập nhật mới nhất" />
        <p className="mt-3 text-sm font-medium text-slate-700">Cập nhật realtime từ DB</p>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm">
        <div className="flex items-center gap-3 md:hidden">
          <input
            value={searchQuery}
            onChange={(e) => updateQuery({ q: e.target.value })}
            placeholder="Tìm deal"
            className="flex-1 rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          />
          <button onClick={() => setMobileFilterOpen(true)} className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-100">
            Bộ lọc
          </button>
        </div>

        <div className="mt-3 hidden grid-cols-1 gap-3 sm:grid-cols-2 lg:grid lg:grid-cols-4">
          <input
            value={searchQuery}
            onChange={(e) => updateQuery({ q: e.target.value })}
            placeholder="Tìm theo tên sản phẩm hoặc danh mục"
            className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          />
          <select value={categoryFilter} onChange={(e) => updateQuery({ category: e.target.value })} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100">
            <option value="all">Tất cả danh mục</option>
            {categories.map((cat) => (
              <option key={cat} value={cat}>
                {cat}
              </option>
            ))}
          </select>
          <select value={platformFilter} onChange={(e) => updateQuery({ platform: e.target.value })} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100">
            <option value="all">Tất cả nền tảng</option>
            {platforms.map((plat) => (
              <option key={plat} value={plat}>
                {plat}
              </option>
            ))}
          </select>
          <select value={discountFilter} onChange={(e) => updateQuery({ minDiscount: e.target.value })} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100">
            <option value="all">Tất cả mức giảm</option>
            <option value="10">Giảm từ 10%</option>
            <option value="20">Giảm từ 20%</option>
            <option value="30">Giảm từ 30%</option>
            <option value="50">Giảm từ 50%</option>
          </select>
          <div className="grid grid-cols-2 gap-2">
            <input value={minPrice} onChange={(e) => updateQuery({ minPrice: e.target.value })} inputMode="numeric" placeholder="Giá tối thiểu" className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100" />
            <input value={maxPrice} onChange={(e) => updateQuery({ maxPrice: e.target.value })} inputMode="numeric" placeholder="Giá tối đa" className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100" />
          </div>
          <select value={statusFilter} onChange={(e) => updateQuery({ status: e.target.value })} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100">
            <option value="active">Deal đang hoạt động</option>
            <option value="expired">Deal hết hạn</option>
            <option value="all">Tất cả trạng thái</option>
          </select>
          <select value={sortBy} onChange={(e) => updateQuery({ sort: e.target.value })} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100">
            <option value="newest">Mới nhất</option>
            <option value="biggest-discount">Giảm mạnh nhất</option>
            <option value="lowest-price">Giá thấp nhất</option>
            <option value="ending-soon">Sắp hết hạn</option>
          </select>
          <label className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2.5 text-sm text-slate-700">
            <input type="checkbox" checked={couponFilter} onChange={(e) => updateQuery({ couponOnly: e.target.checked ? "true" : null })} className="h-4 w-4 rounded border-slate-300 text-blue-600" />
            Chỉ deal có coupon
          </label>
          <button onClick={resetFilters} className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-100">
            Reset filters
          </button>
        </div>

        {activeFilterChips.length > 0 && (
          <div className="mt-3 flex gap-2 overflow-x-auto">
            {activeFilterChips.map((chip) => (
              <button key={chip} onClick={() => removeChip(chip)} className="whitespace-nowrap rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700 transition hover:bg-blue-100">
                {chip} ×
              </button>
            ))}
          </div>
        )}
      </section>

      {mobileFilterOpen && (
        <div className="fixed inset-0 z-50 bg-black/30 p-4 md:hidden" onClick={() => setMobileFilterOpen(false)}>
          <div className="mx-auto mt-8 max-w-md rounded-2xl border border-slate-200 bg-white p-4" onClick={(e) => e.stopPropagation()}>
            <p className="text-sm font-semibold text-slate-900">Bộ lọc deals</p>
            <div className="mt-3 grid gap-2">
              <select value={categoryFilter} onChange={(e) => updateQuery({ category: e.target.value })} className="rounded-xl border border-slate-200 px-3 py-3 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100">
                <option value="all">Tất cả danh mục</option>
                {categories.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
              <select value={platformFilter} onChange={(e) => updateQuery({ platform: e.target.value })} className="rounded-xl border border-slate-200 px-3 py-3 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100">
                <option value="all">Tất cả nền tảng</option>
                {platforms.map((plat) => (
                  <option key={plat} value={plat}>
                    {plat}
                  </option>
                ))}
              </select>
              <select value={discountFilter} onChange={(e) => updateQuery({ minDiscount: e.target.value })} className="rounded-xl border border-slate-200 px-3 py-3 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100">
                <option value="all">Tất cả mức giảm</option>
                <option value="10">Giảm từ 10%</option>
                <option value="20">Giảm từ 20%</option>
                <option value="30">Giảm từ 30%</option>
                <option value="50">Giảm từ 50%</option>
              </select>
            </div>
            <div className="mt-4 grid grid-cols-2 gap-2">
              <button onClick={() => setMobileFilterOpen(false)} className="rounded-xl bg-blue-600 px-3 py-3 text-sm font-semibold text-white transition hover:bg-blue-700">
                Áp dụng
              </button>
              <button onClick={resetFilters} className="rounded-xl border border-slate-200 bg-white px-3 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-100">
                Clear
              </button>
            </div>
          </div>
        </div>
      )}

      <section className="mt-6">
        {error ? (
          <ErrorState title="Không tải được deals" message={error} />
        ) : isLoading ? (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            <LoadingSkeleton className="h-56 w-full rounded-2xl" />
            <LoadingSkeleton className="h-56 w-full rounded-2xl" />
            <LoadingSkeleton className="h-56 w-full rounded-2xl" />
          </div>
        ) : deals.length === 0 ? (
          <EmptyState title="Chưa có deal active" message="Hãy đổi bộ lọc danh mục hoặc mức giảm để xem thêm deal mới." />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {deals.map((deal) => (
              <DealCard
                key={deal.id}
                id={deal.id}
                productName={deal.productName}
                image={deal.image}
                category={deal.category}
                platform={deal.platform}
                discountPercent={deal.discountPercent}
                currentPriceLabel={deal.currentPriceLabel}
                oldPriceLabel={deal.oldPriceLabel}
                expiryLabel={deal.expiryLabel}
                endTime={deal.endTime}
                isExpired={deal.isExpired}
                hasAffiliate={deal.hasAffiliate}
                hasCoupon={deal.hasCoupon}
                couponCode={deal.couponCode}
              />
            ))}
          </div>
        )}
      </section>
    </PageContainer>
  );
}
