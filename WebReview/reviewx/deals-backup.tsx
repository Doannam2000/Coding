"use client";

import { useEffect, useMemo, useState } from "react";
import { DealCard } from "@/components/deal-card";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer, SectionHeader } from "@/components/ui";

type DealApiItem = {
  id: string;
  product: {
    name: string;
    category?: { name?: string } | null;
    images?: { url: string }[];
  } | null;
  currentPrice: string;
  oldPrice: string;
  discount: string;
  endTime: string;
  createdAt?: string;
  updatedAt?: string;
  status: string;
  affiliateLink?: { internalUrl?: string | null; label?: string | null } | null;
};

const PAGE_SIZE = 24;
const MAX_PAGE = 5;

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
  sortDate: string;
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
  const [allDeals, setAllDeals] = useState<DealItem[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [platformFilter, setPlatformFilter] = useState("all");
  const [discountFilter, setDiscountFilter] = useState("all");
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [statusFilter, setStatusFilter] = useState<"all" | "active" | "expired">("active");
  const [couponFilter, setCouponFilter] = useState(false);
  const [sortBy, setSortBy] = useState<"newest" | "biggest-discount" | "lowest-price" | "ending-soon">("newest");
  const [isLoading, setIsLoading] = useState(true);
  const [isApplyingFilters, setIsApplyingFilters] = useState(false);
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);
  const [error, setError] = useState("");

  async function loadDeals() {
    setIsLoading(true);
    setError("");
    try {
      const response = await fetch(`/api/public/deals?limit=${PAGE_SIZE}&page=1`);
      const payload = (await response.json()) as { success: boolean; data?: { items: any[]; total: number }; error?: string };

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.error ?? "Không tải được deals");
      }

      const now = Date.now();
      const mapped = (payload.data.items ?? []).map((item: any) => {
        const end = item.endTime ? new Date(item.endTime) : null;
        const isExpired = end ? end.getTime() < now : false;
        const platform = item.platform || "Shopee";
        return {
          id: item.id,
          productName: item.productName ?? item.name ?? "Sản phẩm",
          image: item.image ?? "https://images.unsplash.com/photo-1546868871-7041f2a55e12?q=80&w=1200&auto=format&fit=crop",
          category: item.category ?? "Khác",
          platform,
          currentPriceLabel: item.currentPrice ?? "—",
          oldPriceLabel: item.oldPrice ?? "",
          discountPercent: parseDiscount(item.discount ?? ""),
          expiryLabel: isExpired ? "Deal đã hết hạn" : end ? `Hết hạn: ${end.toLocaleString("vi-VN")}` : "—",
          endTime: item.endTime ?? "",
          sortDate: item.updatedAt ?? item.endTime ?? "",
          isExpired,
          hasAffiliate: Boolean(item.affiliateUrl || item.internalUrl),
          hasCoupon: Boolean(item.couponCode),
          couponCode: item.couponCode ?? undefined,
        } as DealItem;
      });

      setAllDeals(mapped);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Không tải được deals");
      setAllDeals([]);
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadDeals();
    }, 0);
    return () => clearTimeout(timer);
  }, []);

  const categories = useMemo(() => Array.from(new Set(allDeals.map((deal) => deal.category))), [allDeals]);
  const platforms = useMemo(() => Array.from(new Set(allDeals.map((deal) => deal.platform))), [allDeals]);

  const deals = useMemo(() => {
    let result = [...allDeals];

    if (searchQuery.trim()) {
      const query = searchQuery.trim().toLowerCase();
      result = result.filter((deal) => deal.productName.toLowerCase().includes(query) || deal.category.toLowerCase().includes(query));
    }
    if (categoryFilter !== "all") result = result.filter((deal) => deal.category === categoryFilter);
    if (platformFilter !== "all") result = result.filter((deal) => deal.platform === platformFilter);
    if (discountFilter !== "all") result = result.filter((deal) => deal.discountPercent >= Number(discountFilter));
    if (statusFilter === "active") result = result.filter((deal) => !deal.isExpired);
    if (statusFilter === "expired") result = result.filter((deal) => deal.isExpired);
    if (couponFilter) result = result.filter((deal) => deal.hasCoupon);

    if (minPrice.trim()) {
      const min = Number(minPrice);
      if (Number.isFinite(min)) {
        result = result.filter((deal) => Number(deal.currentPriceLabel.replace(/\D/g, "")) >= min);
      }
    }

    if (maxPrice.trim()) {
      const max = Number(maxPrice);
      if (Number.isFinite(max)) {
        result = result.filter((deal) => Number(deal.currentPriceLabel.replace(/\D/g, "")) <= max);
      }
    }

    if (sortBy === "newest") result.sort((a, b) => new Date(b.sortDate).getTime() - new Date(a.sortDate).getTime());
    if (sortBy === "biggest-discount") result.sort((a, b) => b.discountPercent - a.discountPercent);
    if (sortBy === "lowest-price") result.sort((a, b) => Number(a.currentPriceLabel.replace(/\D/g, "")) - Number(b.currentPriceLabel.replace(/\D/g, "")));
    if (sortBy === "ending-soon") result.sort((a, b) => new Date(a.sortDate).getTime() - new Date(b.sortDate).getTime());

    return result;
  }, [allDeals, searchQuery, categoryFilter, platformFilter, discountFilter, minPrice, maxPrice, statusFilter, couponFilter, sortBy]);

  function resetFilters() {
    setSearchQuery("");
    setCategoryFilter("all");
    setPlatformFilter("all");
    setDiscountFilter("all");
    setMinPrice("");
    setMaxPrice("");
    setStatusFilter("active");
    setCouponFilter(false);
    setSortBy("newest");
  }

  function applyMobileFilters() {
    setIsApplyingFilters(true);
    setTimeout(() => {
      setMobileFilterOpen(false);
      setIsApplyingFilters(false);
    }, 150);
  }

  function removeChip(chip: string) {
    if (chip.startsWith("Từ khóa:")) setSearchQuery("");
    if (chip.startsWith("Danh mục:")) setCategoryFilter("all");
    if (chip.startsWith("Nền tảng:")) setPlatformFilter("all");
    if (chip.startsWith("Giảm từ")) setDiscountFilter("all");
    if (chip.startsWith("Giá từ")) setMinPrice("");
    if (chip.startsWith("Giá đến")) setMaxPrice("");
    if (chip === "Đang hoạt động" || chip === "Đã hết hạn") setStatusFilter("all");
    if (chip === "Có coupon") setCouponFilter(false);
  }

  const activeFilterChips = [
    searchQuery.trim() ? `Từ khóa: ${searchQuery.trim()}` : null,
    categoryFilter !== "all" ? `Danh mục: ${categoryFilter}` : null,
    platformFilter !== "all" ? `Nền tảng: ${platformFilter}` : null,
    discountFilter !== "all" ? `Giảm từ ${discountFilter}%` : null,
    minPrice.trim() ? `Giá từ ${minPrice}` : null,
    maxPrice.trim() ? `Giá đến ${maxPrice}` : null,
    statusFilter !== "all" ? (statusFilter === "active" ? "Đang hoạt động" : "Đã hết hạn") : null,
    couponFilter ? "Có coupon" : null,
  ].filter(Boolean) as string[];

  const itemListSchema = {
    "@context": "https://schema.org",
    "@type": "ItemList",
    name: "Deal Shopee hot",
    itemListElement: deals.map((deal, index) => ({ "@type": "ListItem", position: index + 1, url: `https://reviewx.vn/recommends/${deal.id}`, name: deal.productName })),
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
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm deal"
            className="flex-1 rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          />
          <button onClick={() => setMobileFilterOpen(true)} className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">Bộ lọc</button>
        </div>

        <div className="mt-3 hidden grid-cols-1 gap-3 sm:grid-cols-2 lg:grid lg:grid-cols-4">
          <input value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} placeholder="Tìm theo tên sản phẩm hoặc danh mục" className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-100" />
          <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-100"><option value="all">Tất cả danh mục</option>{categories.map((category) => <option key={category} value={category}>{category}</option>)}</select>
          <select value={platformFilter} onChange={(e) => setPlatformFilter(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-100"><option value="all">Tất cả nền tảng</option>{platforms.map((platform) => <option key={platform} value={platform}>{platform}</option>)}</select>
          <select value={discountFilter} onChange={(e) => setDiscountFilter(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-100"><option value="all">Tất cả mức giảm</option><option value="10">Giảm từ 10%</option><option value="20">Giảm từ 20%</option><option value="30">Giảm từ 30%</option><option value="50">Giảm từ 50%</option></select>
          <div className="grid grid-cols-2 gap-2"><input value={minPrice} onChange={(e) => setMinPrice(e.target.value)} inputMode="numeric" placeholder="Giá tối thiểu" className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-100" /><input value={maxPrice} onChange={(e) => setMaxPrice(e.target.value)} inputMode="numeric" placeholder="Giá tối đa" className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-100" /></div>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as "all" | "active" | "expired")} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-100"><option value="active">Deal đang hoạt động</option><option value="expired">Deal hết hạn</option><option value="all">Tất cả trạng thái</option></select>
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value as "newest" | "biggest-discount" | "lowest-price" | "ending-soon")} className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-100"><option value="newest">Mới nhất</option><option value="biggest-discount">Giảm mạnh nhất</option><option value="lowest-price">Giá thấp nhất</option><option value="ending-soon">Sắp hết hạn</option></select>
          <label className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2.5 text-sm text-slate-700"><input type="checkbox" checked={couponFilter} onChange={(e) => setCouponFilter(e.target.checked)} className="h-4 w-4 rounded border-slate-300 text-blue-600 focus-visible:ring-2 focus-visible:ring-blue-500" />Chỉ deal có coupon</label>
          <button onClick={resetFilters} className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">Reset filters</button>
        </div>

        {activeFilterChips.length > 0 ? (
          <div className="mt-3 flex gap-2 overflow-x-auto">
            {activeFilterChips.map((chip) => (
              <button key={chip} onClick={() => removeChip(chip)} className="whitespace-nowrap rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700 transition hover:bg-blue-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">{chip} ×</button>
            ))}
          </div>
        ) : null}
      </section>

      {mobileFilterOpen ? (
        <div className="fixed inset-0 z-50 bg-black/30 p-4 md:hidden" onClick={() => setMobileFilterOpen(false)}>
          <div className="mx-auto mt-8 max-w-md rounded-2xl border border-slate-200 bg-white p-4" onClick={(e) => e.stopPropagation()}>
            <p className="text-sm font-semibold text-slate-900">Bộ lọc deals</p>
            <div className="mt-3 grid gap-2">
              <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-3 text-sm outline-none transition focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-100"><option value="all">Tất cả danh mục</option>{categories.map((category) => <option key={category} value={category}>{category}</option>)}</select>
              <select value={platformFilter} onChange={(e) => setPlatformFilter(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-3 text-sm outline-none transition focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-100"><option value="all">Tất cả nền tảng</option>{platforms.map((platform) => <option key={platform} value={platform}>{platform}</option>)}</select>
              <select value={discountFilter} onChange={(e) => setDiscountFilter(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-3 text-sm outline-none transition focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-100"><option value="all">Tất cả mức giảm</option><option value="10">Giảm từ 10%</option><option value="20">Giảm từ 20%</option><option value="30">Giảm từ 30%</option><option value="50">Giảm từ 50%</option></select>
            </div>
            <div className="mt-4 grid grid-cols-2 gap-2">
              <button onClick={applyMobileFilters} disabled={isApplyingFilters} className="rounded-xl bg-blue-600 px-3 py-3 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">{isApplyingFilters ? "Đang áp dụng..." : "Apply"}</button>
              <button onClick={resetFilters} className="rounded-xl border border-slate-200 bg-white px-3 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">Clear</button>
            </div>
          </div>
        </div>
      ) : null}

      <section className="mt-6">
        {error ? <ErrorState title="Không tải được deals" message={error} /> : null}
        {isLoading ? (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3"><LoadingSkeleton className="h-56 w-full rounded-2xl" /><LoadingSkeleton className="h-56 w-full rounded-2xl" /><LoadingSkeleton className="h-56 w-full rounded-2xl" /></div>
        ) : deals.length === 0 ? (
          <EmptyState title="Hiện chưa có deal phù hợp" message="Hãy đổi bộ lọc danh mục hoặc mức giảm để xem thêm deal mới." />
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

