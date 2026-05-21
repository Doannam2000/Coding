"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer, ProductScoreBadge, ShopeeCTAButton, SectionHeader } from "@/components/ui";

type SearchItem = {
  id: string;
  type: "product" | "review" | "deal" | "category";
  title: string;
  summary: string;
  href: string;
  score?: number;
};

type TabValue = "all" | "product" | "review" | "deal" | "category";
type SortValue = "relevance" | "newest" | "price" | "score";

type FilterState = {
  category: string;
  priceRange: string;
  minScore: string;
  platform: string;
  discount: string;
};

const tabMap: Record<TabValue, string> = {
  all: "Táº¥t cáº£",
  product: "Sáº£n pháº©m",
  review: "Review",
  deal: "Deals",
  category: "Danh má»¥c",
};

const popularSearches = ["tai nghe bluetooth", "ná»“i chiÃªn khÃ´ng dáº§u", "bÃ n phÃ­m cÆ¡", "mÃ¡y hÃºt bá»¥i mini", "sá»¯a rá»­a máº·t"];
const RECENT_KEY = "reviewx_recent_searches";
const categories = ["CÃ´ng nghá»‡", "Gia dá»¥ng", "LÃ m Ä‘áº¹p", "Gaming"];
const platforms = ["Shopee", "Lazada", "Tiki", "Other"];
const discountOptions = ["10%+", "20%+", "30%+", "50%+"];

function parseNumber(input: string) {
  const value = Number(input);
  return Number.isFinite(value) ? value : null;
}

function inferPrice(summary: string) {
  const matched = summary.match(/(\d+[\d\.]*)\s?(k|m|triá»‡u)?/i);
  if (!matched) return null;
  const raw = matched[1].replace(/\./g, "");
  const base = parseNumber(raw);
  if (base === null) return null;
  const unit = matched[2]?.toLowerCase();
  if (unit === "k") return base * 1000;
  if (unit === "m" || unit === "triá»‡u") return base * 1_000_000;
  return base;
}

function inferDiscount(summary: string) {
  const matched = summary.match(/(\d{1,2})%/);
  if (!matched) return null;
  return parseNumber(matched[1]);
}

function inferScore(item: SearchItem) {
  if (typeof item.score === "number") return item.score;
  const matched = item.summary.match(/(\d(?:\.\d)?)/);
  if (!matched) return null;
  return parseNumber(matched[1]);
}

function inferPlatform(summary: string) {
  const lower = summary.toLowerCase();
  if (lower.includes("shopee")) return "Shopee";
  if (lower.includes("lazada")) return "Lazada";
  if (lower.includes("tiki")) return "Tiki";
  return "Other";
}

function inferCategory(summary: string) {
  const lower = summary.toLowerCase();
  if (lower.includes("gaming")) return "Gaming";
  if (lower.includes("gia dá»¥ng")) return "Gia dá»¥ng";
  if (lower.includes("lÃ m Ä‘áº¹p")) return "LÃ m Ä‘áº¹p";
  return "CÃ´ng nghá»‡";
}

function isShopeeLike(input: string) {
  return /(shopee\.vn|s\.shopee\.vn|shopee\.ee|shp\.ee)/i.test(input);
}

function isLikelyUrl(input: string) {
  return /^https?:\/\//i.test(input);
}

function defaultFilters(): FilterState {
  return { category: "", priceRange: "", minScore: "", platform: "", discount: "" };
}

export default function SearchClient({ initialQ, initialMode, allItems }: { initialQ: string; initialMode: string; allItems: SearchItem[] }) {
  const [input, setInput] = useState(initialQ);
  const [submittedQuery, setSubmittedQuery] = useState(initialQ.trim());
  const [activeTab, setActiveTab] = useState<TabValue>("all");
  const [sortBy, setSortBy] = useState<SortValue>("relevance");
  const [filters, setFilters] = useState<FilterState>(defaultFilters());
  const [isLoading, setIsLoading] = useState(false);
  const [invalidUrl, setInvalidUrl] = useState(false);
  const [hasError, setHasError] = useState(false);
  const [modeFromParams, setModeFromParams] = useState(initialMode);
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);
  const [recentSearches, setRecentSearches] = useState<string[]>(() => {
    if (typeof window === "undefined") return [];
    try {
      const raw = window.localStorage.getItem(RECENT_KEY);
      if (!raw) return [];
      const parsed = JSON.parse(raw) as string[];
      return Array.isArray(parsed) ? parsed.filter(Boolean).slice(0, 8) : [];
    } catch {
      return [];
    }
  });

  const realtimeSuggestions = useMemo(() => {
    const q = input.trim().toLowerCase();
    if (!q) return [];
    return allItems
      .filter((item) => `${item.title} ${item.summary}`.toLowerCase().includes(q))
      .slice(0, 6)
      .map((item) => item.title);
  }, [allItems, input]);

  const results = useMemo(() => {
    if (!submittedQuery) return [];
    const q = submittedQuery.toLowerCase();
    return allItems
      .filter((item) => `${item.title} ${item.summary}`.toLowerCase().includes(q))
      .filter((item) => (activeTab === "all" ? true : item.type === activeTab))
      .filter((item) => {
        const itemCategory = inferCategory(item.summary);
        const itemPlatform = inferPlatform(item.summary);
        const itemDiscount = inferDiscount(item.summary);
        const itemScore = inferScore(item);
        const itemPrice = inferPrice(item.summary);

        if (filters.category && itemCategory !== filters.category) return false;
        if (filters.platform && itemPlatform !== filters.platform) return false;
        if (filters.discount) {
          const threshold = parseNumber(filters.discount.replace("%+", ""));
          if (threshold !== null && (itemDiscount === null || itemDiscount < threshold)) return false;
        }
        if (filters.minScore) {
          const threshold = parseNumber(filters.minScore);
          if (threshold !== null && (itemScore === null || itemScore < threshold)) return false;
        }
        if (filters.priceRange === "under-500k" && (itemPrice === null || itemPrice >= 500_000)) return false;
        if (filters.priceRange === "500k-2m" && (itemPrice === null || itemPrice < 500_000 || itemPrice > 2_000_000)) return false;
        if (filters.priceRange === "over-2m" && (itemPrice === null || itemPrice <= 2_000_000)) return false;
        return true;
      })
      .sort((a, b) => {
        if (sortBy === "newest") return b.id.localeCompare(a.id);
        if (sortBy === "price") return (inferPrice(a.summary) ?? Number.MAX_SAFE_INTEGER) - (inferPrice(b.summary) ?? Number.MAX_SAFE_INTEGER);
        if (sortBy === "score") return (inferScore(b) ?? -1) - (inferScore(a) ?? -1);
        const ar = `${a.title} ${a.summary}`.toLowerCase().includes(q) ? 1 : 0;
        const br = `${b.title} ${b.summary}`.toLowerCase().includes(q) ? 1 : 0;
        return br - ar;
      });
  }, [activeTab, allItems, filters, sortBy, submittedQuery]);

  function saveRecentSearch(value: string) {
    const next = [value, ...recentSearches.filter((item) => item !== value)].slice(0, 8);
    setRecentSearches(next);
    window.localStorage.setItem(RECENT_KEY, JSON.stringify(next));
  }

  function clearRecentSearches() {
    setRecentSearches([]);
    window.localStorage.removeItem(RECENT_KEY);
  }

  function runSearch(value: string) {
    const next = value.trim();
    setInput(next);
    setSubmittedQuery("");
    setInvalidUrl(false);
    setHasError(false);
    if (!next) return;
    if (isLikelyUrl(next) && !isShopeeLike(next)) {
      setInvalidUrl(true);
      return;
    }
    setIsLoading(true);
    setTimeout(() => {
      try {
        const isShort = isShopeeLike(next);
        const url = isShort ? `/tim-kiem?q=${encodeURIComponent(next)}&mode=short-link` : `/tim-kiem?q=${encodeURIComponent(next)}`;
        window.history.replaceState(null, "", url);
        setModeFromParams(isShort ? "short-link" : "");
        setSubmittedQuery(next);
        saveRecentSearch(next);
      } catch {
        setHasError(true);
      } finally {
        setIsLoading(false);
      }
    }, 350);
  }

  const showFilters = Boolean(submittedQuery);
  const activeFilters = [filters.category, filters.priceRange, filters.minScore, filters.platform, filters.discount].filter(Boolean);

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
        <SectionHeader title="TÃ¬m kiáº¿m" subtitle="TÃ¬m nhanh sáº£n pháº©m, review, deal vÃ  danh má»¥c" />
        <form onSubmit={(e) => { e.preventDefault(); runSearch(input); }} className="mt-4 space-y-3">
          <div className="flex flex-col gap-3 sm:flex-row">
            <input value={input} onChange={(e) => setInput(e.target.value)} placeholder="Báº¡n Ä‘ang tÃ¬m gÃ¬?" className="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-base" />
            <button type="submit" className="inline-flex h-12 items-center justify-center rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 px-5 text-sm font-semibold text-white">
              {isLoading ? "Äang tÃ¬m..." : "TÃ¬m kiáº¿m"}
            </button>
          </div>

          {input.trim() ? (
            <div className="space-y-2">
              <p className="text-xs font-semibold text-slate-600">Gá»£i Ã½ realtime</p>
              {isLoading ? (
                <div className="grid gap-2 sm:grid-cols-2"><LoadingSkeleton className="h-10 w-full rounded-xl" /><LoadingSkeleton className="h-10 w-full rounded-xl" /></div>
              ) : realtimeSuggestions.length === 0 ? (
                <EmptyState title="KhÃ´ng cÃ³ gá»£i Ã½" message="HÃ£y thá»­ tá»« khÃ³a khÃ¡c Ä‘á»ƒ nháº­n gá»£i Ã½ phÃ¹ há»£p hÆ¡n." />
              ) : (
                <div className="flex flex-wrap gap-2 text-xs">
                  {realtimeSuggestions.map((item) => <button key={item} type="button" onClick={() => runSearch(item)} className="rounded-full border border-slate-200 bg-white px-3 py-1.5 font-medium text-slate-700">{item}</button>)}
                </div>
              )}
            </div>
          ) : null}

          <div className="flex flex-wrap gap-2 text-xs"><span className="font-semibold text-slate-600">TÃ¬m kiáº¿m phá»• biáº¿n:</span>{popularSearches.map((item) => <button key={item} type="button" onClick={() => runSearch(item)} className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 font-medium text-slate-700">{item}</button>)}</div>

          <div className="space-y-2 text-xs">
            <div className="flex items-center justify-between gap-2">
              <span className="font-semibold text-slate-600">Gáº§n Ä‘Ã¢y:</span>
              {recentSearches.length > 0 ? <button type="button" onClick={clearRecentSearches} className="rounded-lg border border-slate-200 bg-white px-2 py-1 font-medium text-slate-700">XÃ³a gáº§n Ä‘Ã¢y</button> : null}
            </div>
            {recentSearches.length === 0 ? <p className="text-slate-500">ChÆ°a cÃ³ lá»‹ch sá»­ tÃ¬m kiáº¿m gáº§n Ä‘Ã¢y.</p> : <div className="flex flex-wrap gap-2">{recentSearches.map((item) => <button key={item} type="button" onClick={() => runSearch(item)} className="rounded-full border border-slate-200 bg-white px-3 py-1.5 font-medium text-slate-700">{item}</button>)}</div>}
          </div>
        </form>

        <div className="mt-5 flex flex-wrap gap-2">
          {(Object.keys(tabMap) as TabValue[]).map((tab) => <button key={tab} type="button" onClick={() => setActiveTab(tab)} className={`rounded-xl px-3 py-2 text-sm font-medium ${activeTab === tab ? "bg-blue-600 text-white" : "border border-slate-200 bg-white text-slate-700"}`}>{tabMap[tab]}</button>)}
        </div>

        {showFilters ? (
          <div className="mt-4 space-y-3">
            <div className="flex items-center justify-between">
              <p className="text-sm font-semibold text-slate-800">Bá»™ lá»c & sáº¯p xáº¿p</p>
              <button type="button" onClick={() => setMobileFiltersOpen((v) => !v)} className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 sm:hidden">{mobileFiltersOpen ? "áº¨n bá»™ lá»c" : "Hiá»‡n bá»™ lá»c"}</button>
            </div>
            <div className={`${mobileFiltersOpen ? "block" : "hidden"} space-y-3 rounded-2xl border border-slate-200 bg-slate-50 p-3 sm:block`}>
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                <select value={filters.category} onChange={(e) => setFilters((prev) => ({ ...prev, category: e.target.value }))} className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"><option value="">Danh má»¥c</option>{categories.map((item) => <option key={item} value={item}>{item}</option>)}</select>
                <select value={filters.priceRange} onChange={(e) => setFilters((prev) => ({ ...prev, priceRange: e.target.value }))} className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"><option value="">Khoáº£ng giÃ¡</option><option value="under-500k">&lt; 500k</option><option value="500k-2m">500k - 2m</option><option value="over-2m">&gt; 2m</option></select>
                <select value={filters.minScore} onChange={(e) => setFilters((prev) => ({ ...prev, minScore: e.target.value }))} className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"><option value="">Äiá»ƒm tá»‘i thiá»ƒu</option><option value="6">6+</option><option value="7">7+</option><option value="8">8+</option><option value="9">9+</option></select>
                <select value={filters.platform} onChange={(e) => setFilters((prev) => ({ ...prev, platform: e.target.value }))} className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"><option value="">Ná»n táº£ng</option>{platforms.map((item) => <option key={item} value={item}>{item}</option>)}</select>
                <select value={filters.discount} onChange={(e) => setFilters((prev) => ({ ...prev, discount: e.target.value }))} className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"><option value="">Má»©c giáº£m</option>{discountOptions.map((item) => <option key={item} value={item}>{item}</option>)}</select>
                <select value={sortBy} onChange={(e) => setSortBy(e.target.value as SortValue)} className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"><option value="relevance">Sáº¯p xáº¿p: LiÃªn quan</option><option value="newest">Sáº¯p xáº¿p: Má»›i nháº¥t</option><option value="price">Sáº¯p xáº¿p: GiÃ¡</option><option value="score">Sáº¯p xáº¿p: Äiá»ƒm</option></select>
              </div>
              {activeFilters.length > 0 ? <div className="flex flex-wrap items-center gap-2"><button type="button" onClick={() => setFilters(defaultFilters())} className="rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-semibold text-slate-700">XÃ³a lá»c</button>{activeFilters.map((item, index) => <span key={`${item}-${index}`} className="rounded-full border border-blue-200 bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700">{item}</span>)}</div> : null}
            </div>
          </div>
        ) : null}

        {modeFromParams === "short-link" ? <p className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">PhÃ¡t hiá»‡n link Shopee...</p> : null}
        {invalidUrl ? <p className="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">URL khÃ´ng há»£p lá»‡ hoáº·c khÃ´ng pháº£i domain Shopee Ä‘Æ°á»£c há»— trá»£.</p> : null}

        <div className="mt-5">
          {submittedQuery ? <p className="mb-3 text-sm text-slate-600">Káº¿t quáº£ cho &quot;{submittedQuery}&quot;: <span className="font-semibold text-slate-900">{results.length} káº¿t quáº£</span></p> : null}
          {hasError ? (
            <ErrorState title="TÃ¬m kiáº¿m tháº¥t báº¡i" message="ÄÃ£ xáº£y ra lá»—i khi xá»­ lÃ½ tÃ¬m kiáº¿m. Vui lÃ²ng thá»­ láº¡i." />
          ) : isLoading ? (
            <div className="grid gap-3 sm:grid-cols-2"><LoadingSkeleton className="h-28 w-full rounded-2xl" /><LoadingSkeleton className="h-28 w-full rounded-2xl" /></div>
          ) : !submittedQuery ? (
            <EmptyState title="Nháº­p tá»« khÃ³a Ä‘á»ƒ báº¯t Ä‘áº§u" message="Báº¡n cÃ³ thá»ƒ tÃ¬m sáº£n pháº©m, review, deal hoáº·c danh má»¥c." />
          ) : results.length === 0 ? (
            <EmptyState title="KhÃ´ng cÃ³ káº¿t quáº£" message="Thá»­ tá»« khÃ³a khÃ¡c hoáº·c Ä‘á»•i tab/bá»™ lá»c Ä‘á»ƒ má»Ÿ rá»™ng pháº¡m vi tÃ¬m kiáº¿m." />
          ) : (
            <div className="grid gap-3 sm:grid-cols-2">
              {results.map((item) => (
                <article key={item.id} className="rounded-2xl border border-slate-200/70 bg-white p-4">
                  <p className="text-xs font-semibold text-slate-500">{tabMap[item.type]}</p>
                  <h3 className="mt-1 text-sm font-semibold text-slate-900">{item.title}</h3>
                  <p className="mt-1 text-sm text-slate-600">{item.summary}</p>
                  <div className="mt-3 flex flex-wrap items-center gap-2">
                    {item.score ? <ProductScoreBadge score={item.score} /> : null}
                    {item.type === "product" ? <ShopeeCTAButton href={`/go/product/${item.id}`}>Xem giÃ¡ Shopee</ShopeeCTAButton> : null}{item.type === "deal" ? <ShopeeCTAButton href={`/go/deal/${item.id}`}>Xem giÃ¡ Shopee</ShopeeCTAButton> : null}
                    <Link href={item.href} className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700">Má»Ÿ ná»™i dung</Link>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>
    </PageContainer>
  );
}

