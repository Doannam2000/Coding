"use client";

import Image from "next/image";
import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { EmptyState, PageContainer, SectionHeader } from "@/components/ui";
import { ComparisonTable } from "@/components/comparison-table";

type CompareProduct = {
  id: string;
  name: string;
  image: string;
  priceLabel: string;
  oldPriceLabel?: string;
  discountLabel?: string;
  worthScore: number;
  category: string;
  rating?: number;
  soldCount?: number;
  brand?: string;
  specifications?: string[];
  pros?: string[];
  cons?: string[];
  bestFor?: string[];
};

const allProducts: CompareProduct[] = [
  {
    id: "sony-wh-ch520",
    name: "Tai nghe Sony WH-CH520",
    image: "https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=240&q=80",
    priceLabel: "790.000Ä‘",
    oldPriceLabel: "990.000Ä‘",
    discountLabel: "-20%",
    worthScore: 8.6,
    specifications: ["Bluetooth 5.2", "Pin 50 giá»", "Sáº¡c USB-C"],
    category: "CÃ´ng nghá»‡",
    rating: 4.8,
    soldCount: 3200,
    brand: "Sony",
    pros: ["Pin ráº¥t lÃ¢u", "Káº¿t ná»‘i á»•n Ä‘á»‹nh"],
    cons: ["KhÃ´ng chá»‘ng á»“n chá»§ Ä‘á»™ng"],
    bestFor: ["Há»c táº­p", "LÃ m viá»‡c"],
  },
  {
    id: "jbl-go-3",
    name: "Loa Bluetooth JBL Go 3",
    image: "https://images.unsplash.com/photo-1545454675-3531b543be5d?auto=format&fit=crop&w=240&q=80",
    priceLabel: "690.000Ä‘",
    oldPriceLabel: "890.000Ä‘",
    discountLabel: "-22%",
    worthScore: 8.2,
    specifications: ["Chuáº©n IP67", "Bluetooth 5.1", "Pin 5 giá»"],
    category: "CÃ´ng nghá»‡",
    rating: 4.7,
    soldCount: 5100,
    brand: "JBL",
    pros: ["Nhá» gá»n", "Ã‚m lÆ°á»£ng tá»‘t"],
    cons: ["Bass á»Ÿ má»©c cÆ¡ báº£n"],
    bestFor: ["Du lá»‹ch", "PhÃ²ng nhá»"],
  },
  {
    id: "logitech-m331",
    name: "Chuá»™t Logitech M331",
    image: "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?auto=format&fit=crop&w=240&q=80",
    priceLabel: "320.000Ä‘",
    oldPriceLabel: "450.000Ä‘",
    discountLabel: "-29%",
    worthScore: 8.1,
    specifications: ["Silent click", "DPI 1000", "Pin 18 thÃ¡ng"],
    category: "CÃ´ng nghá»‡",
    rating: 4.9,
    soldCount: 8200,
    brand: "Logitech",
    pros: ["Báº¥m Ãªm", "Pin lÃ¢u"],
    cons: ["KhÃ´ng cÃ³ Bluetooth"],
    bestFor: ["VÄƒn phÃ²ng"],
  },
  {
    id: "aula-f75",
    name: "BÃ n phÃ­m cÆ¡ Aula F75",
    image: "https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=240&q=80",
    priceLabel: "1.290.000Ä‘",
    oldPriceLabel: "1.590.000Ä‘",
    discountLabel: "-19%",
    worthScore: 7.8,
    specifications: ["75% layout", "Hot-swap", "Káº¿t ná»‘i 3 cháº¿ Ä‘á»™"],
    category: "Gaming",
    rating: 4.6,
    soldCount: 1700,
    brand: "Aula",
    pros: ["Layout gá»n", "Keycap Ä‘áº¹p"],
    cons: ["Pháº§n má»m cÃ²n háº¡n cháº¿"],
    bestFor: ["Gaming", "Setup gá»n"],
  },
  {
    id: "lock-lock-airfryer",
    name: "Ná»“i chiÃªn Lock&Lock",
    image: "https://images.unsplash.com/photo-1585515656973-fdb26398d08f?auto=format&fit=crop&w=240&q=80",
    priceLabel: "2.190.000Ä‘",
    oldPriceLabel: "2.690.000Ä‘",
    discountLabel: "-19%",
    worthScore: 8.4,
    specifications: ["Dung tÃ­ch 5.2L", "CÃ´ng suáº¥t 1800W", "8 cháº¿ Ä‘á»™ náº¥u"],
    category: "Gia dá»¥ng",
    rating: 4.7,
    soldCount: 2900,
    brand: "Lock&Lock",
    pros: ["Dung tÃ­ch lá»›n", "Dá»… vá»‡ sinh"],
    cons: ["Chiáº¿m diá»‡n tÃ­ch"],
    bestFor: ["Gia Ä‘Ã¬nh"],
  },
  {
    id: "deerma-vacuum-mini",
    name: "MÃ¡y hÃºt bá»¥i mini Deerma",
    image: "https://images.unsplash.com/photo-1558317374-067fb5f30001?auto=format&fit=crop&w=240&q=80",
    priceLabel: "590.000Ä‘",
    oldPriceLabel: "790.000Ä‘",
    discountLabel: "-25%",
    worthScore: 8.0,
    specifications: ["Lá»±c hÃºt 13kPa", "Pin 30 phÃºt", "Äáº§u hÃºt khe háº¹p"],
    category: "NhÃ  cá»­a",
    rating: 4.5,
    soldCount: 2400,
    brand: "Deerma",
    pros: ["Nháº¹", "GiÃ¡ há»£p lÃ½"],
    cons: ["Lá»±c hÃºt má»©c vá»«a"],
    bestFor: ["PhÃ²ng nhá»", "Xe hÆ¡i"],
  },
];

export default function ComparePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [query, setQuery] = useState("");
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [limitMessage, setLimitMessage] = useState(false);
  const [showAllSpecs, setShowAllSpecs] = useState(false);
  const [onlyDifferences, setOnlyDifferences] = useState(false);
  const [isPreparingTable, setIsPreparingTable] = useState(false);
  const [hasCompareError, setHasCompareError] = useState(false);

  const selectedProducts = useMemo(() => allProducts.filter((p) => selectedIds.includes(p.id)), [selectedIds]);

  useEffect(() => {
    const raw = searchParams.get("items") ?? "";
    if (!raw) return;
    const ids = raw.split(",").map((item) => item.trim()).filter(Boolean).slice(0, 4);
    const valid = ids.filter((id) => allProducts.some((product) => product.id === id));
    if (valid.length > 0) {
      setSelectedIds((prev) => (prev.length === valid.length && prev.every((id, idx) => id === valid[idx]) ? prev : valid));
    }
  }, [searchParams]);

  const normalizedQuery = query.trim().toLowerCase();
  const isSearching = Boolean(query.trim()) && query.trim().length < 2;

  const suggestions = useMemo(() => {
    if (!normalizedQuery || isSearching) return [];
    return allProducts
      .filter((p) => !selectedIds.includes(p.id))
      .filter((p) => {
        const name = p.name.toLowerCase();
        const brand = (p.brand ?? "").toLowerCase();
        const category = p.category.toLowerCase();
        return name.includes(normalizedQuery) || brand.includes(normalizedQuery) || category.includes(normalizedQuery);
      })
      .slice(0, 6);
  }, [isSearching, normalizedQuery, selectedIds]);

  const shouldShowNoSuggestion = Boolean(normalizedQuery) && !isSearching && suggestions.length === 0;

  const priceValue = (product: CompareProduct) => Number.parseInt(product.priceLabel.replace(/[^0-9]/g, ""), 10) || Number.POSITIVE_INFINITY;
  const discountValue = (product: CompareProduct) => Number.parseInt((product.discountLabel ?? "").replace(/[^0-9]/g, ""), 10) || 0;

  const lowestPrice = useMemo(() => Math.min(...selectedProducts.map((p) => priceValue(p))), [selectedProducts]);
  const highestScore = useMemo(() => Math.max(...selectedProducts.map((p) => p.worthScore)), [selectedProducts]);
  const highestDiscount = useMemo(() => Math.max(...selectedProducts.map((p) => discountValue(p))), [selectedProducts]);

  const textOrUpdating = (value?: string) => value ?? "Äang cáº­p nháº­t";
  const listOrUpdating = (value?: string[]) => (value && value.length > 0 ? value.join(", ") : "Äang cáº­p nháº­t");

  const isLowestPrice = (product: CompareProduct) => priceValue(product) === lowestPrice;
  const isHighestScore = (product: CompareProduct) => product.worthScore === highestScore;
  const isHighestDiscount = (product: CompareProduct) => discountValue(product) === highestDiscount;

  const cellHighlightClass = (active: boolean) => (active ? "rounded-lg bg-emerald-50 text-emerald-700 font-semibold" : "");

  function shownSpecs(specs?: string[]) {
    if (!specs || specs.length === 0) return "Äang cáº­p nháº­t";
    if (showAllSpecs) return specs.join(", ");
    return specs.slice(0, 2).join(", ");
  }

  function addProduct(productId: string) {
    setLimitMessage(false);
    if (!allProducts.some((product) => product.id === productId)) return;
    if (selectedIds.includes(productId)) { setQuery(""); return; }
    if (selectedIds.length >= 4) { setLimitMessage(true); return; }
    setSelectedIds((prev) => {
      const next = [...prev, productId];
      router.replace(`/so-sanh?items=${next.join(",")}`, { scroll: false });
      return next;
    });
    setQuery("");
  }

  function removeProduct(productId: string) {
    const ok = window.confirm("Báº¡n cÃ³ cháº¯c muá»‘n bá» sáº£n pháº©m nÃ y khá»i danh sÃ¡ch so sÃ¡nh?");
    if (!ok) return;
    setSelectedIds((prev) => {
      const next = prev.filter((id) => id !== productId);
      router.replace(next.length > 0 ? `/so-sanh?items=${next.join(",")}` : "/so-sanh", { scroll: false });
      return next;
    });
    setLimitMessage(false);
  }

  function clearAll() {
    const ok = window.confirm("Báº¡n cÃ³ cháº¯c muá»‘n xÃ³a toÃ n bá»™ danh sÃ¡ch so sÃ¡nh?");
    if (!ok) return;
    setSelectedIds([]);
    router.replace("/so-sanh", { scroll: false });
    setLimitMessage(false);
  }

  function handleToggleDifferences() {
    setIsPreparingTable(true);
    setOnlyDifferences((prev) => !prev);
    window.setTimeout(() => setIsPreparingTable(false), 120);
  }

  function handleToggleSpecs() {
    setShowAllSpecs((prev) => !prev);
  }

  function retryComparison() {
    setHasCompareError(false);
    setIsPreparingTable(false);
  }

  function winnerBadge(product: CompareProduct) {
    const winners = [isLowestPrice(product), isHighestScore(product), isHighestDiscount(product)].filter(Boolean).length;
    return winners > 0 ? `ðŸ† ${winners} tiÃªu chÃ­` : null;
  }

  function productChipSummary(product: CompareProduct) {
    const badge = winnerBadge(product);
    return badge ? `${product.name} (${badge})` : product.name;
  }

  const showDiffBadge = onlyDifferences;

  if (selectedProducts.length === 0) {
    return (
      <PageContainer>
        <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
          <SectionHeader title="So sÃ¡nh sáº£n pháº©m" subtitle="ThÃªm tá»‘i Ä‘a 4 sáº£n pháº©m Ä‘á»ƒ so sÃ¡nh nhanh theo cÃ¡c tiÃªu chÃ­ chÃ­nh" />

          <div className="mt-4 space-y-3">
            <label htmlFor="compare-search" className="text-sm font-semibold text-slate-700">
              ThÃªm sáº£n pháº©m vÃ o danh sÃ¡ch so sÃ¡nh
            </label>
            <input
              id="compare-search"
              value={query}
              onChange={(e) => { setQuery(e.target.value); setLimitMessage(false); }}
              placeholder="Nháº­p tÃªn sáº£n pháº©m cáº§n so sÃ¡nh"
              className="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            />

            {limitMessage ? <p className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">Báº¡n chá»‰ cÃ³ thá»ƒ so sÃ¡nh tá»‘i Ä‘a 4 sáº£n pháº©m.</p> : null}
            {isSearching ? <div className="rounded-2xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600">Äang táº£i gá»£i Ã½ sáº£n pháº©m...</div> : null}

            {suggestions.length > 0 ? (
              <div className="rounded-2xl border border-slate-200 bg-white p-2">
                <ul className="space-y-1">
                  {suggestions.map((item) => (
                    <li key={item.id}>
                      <button type="button" onClick={() => addProduct(item.id)} className="flex w-full items-center gap-3 rounded-xl px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-slate-100">
                        <Image src={item.image} alt={item.name} width={48} height={48} className="size-12 rounded-lg object-cover" loading="lazy" unoptimized />
                        <span className="min-w-0 flex-1">
                          <span className="block truncate font-semibold text-slate-900">{item.name}</span>
                          <span className="block truncate text-xs text-slate-500">{textOrUpdating(item.brand)} â€¢ {item.category} â€¢ Äiá»ƒm {item.worthScore} â€¢ {item.priceLabel}</span>
                        </span>
                        <span className="shrink-0 text-xs font-semibold text-blue-600">ThÃªm</span>
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}

            {shouldShowNoSuggestion ? <div className="rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">KhÃ´ng tÃ¬m tháº¥y sáº£n pháº©m phÃ¹ há»£p.</div> : null}
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-2" />
        </section>

        <section className="mt-6 space-y-4">
          <EmptyState title="ChÆ°a cÃ³ sáº£n pháº©m Ä‘á»ƒ so sÃ¡nh" message="HÃ£y nháº­p tÃªn sáº£n pháº©m á»Ÿ trÃªn, sau Ä‘Ã³ chá»n tá»‘i Ä‘a 4 sáº£n pháº©m Ä‘á»ƒ báº¯t Ä‘áº§u so sÃ¡nh." />
        </section>
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
        <SectionHeader title="So sÃ¡nh sáº£n pháº©m" subtitle="ThÃªm tá»‘i Ä‘a 4 sáº£n pháº©m Ä‘á»ƒ so sÃ¡nh nhanh theo cÃ¡c tiÃªu chÃ­ chÃ­nh" />

        <div className="mt-4 space-y-3">
          <label htmlFor="compare-search" className="text-sm font-semibold text-slate-700">
            ThÃªm sáº£n pháº©m vÃ o danh sÃ¡ch so sÃ¡nh
          </label>
          <input
            id="compare-search"
            value={query}
            onChange={(e) => { setQuery(e.target.value); setLimitMessage(false); }}
            placeholder="Nháº­p tÃªn sáº£n pháº©m cáº§n so sÃ¡nh"
            className="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          />

          {limitMessage ? <p className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">Báº¡n chá»‰ cÃ³ thá»ƒ so sÃ¡nh tá»‘i Ä‘a 4 sáº£n pháº©m.</p> : null}
          {isSearching ? <div className="rounded-2xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600">Äang táº£i gá»£i Ã½ sáº£n pháº©m...</div> : null}

          {suggestions.length > 0 ? (
            <div className="rounded-2xl border border-slate-200 bg-white p-2">
              <ul className="space-y-1">
                {suggestions.map((item) => (
                  <li key={item.id}>
                    <button type="button" onClick={() => addProduct(item.id)} className="flex w-full items-center gap-3 rounded-xl px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-slate-100">
                      <Image src={item.image} alt={item.name} width={48} height={48} className="size-12 rounded-lg object-cover" loading="lazy" unoptimized />
                      <span className="min-w-0 flex-1">
                        <span className="block truncate font-semibold text-slate-900">{item.name}</span>
                        <span className="block truncate text-xs text-slate-500">{(item.brand ?? "Äang cáº­p nháº­t")} â€¢ {item.category} â€¢ Äiá»ƒm {item.worthScore} â€¢ {item.priceLabel}</span>
                      </span>
                      <span className="shrink-0 text-xs font-semibold text-blue-600">ThÃªm</span>
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}

          {shouldShowNoSuggestion ? <div className="rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">KhÃ´ng tÃ¬m tháº¥y sáº£n pháº©m phÃ¹ há»£p.</div> : null}
        </div>

        <div className="mt-4 flex flex-wrap items-center gap-2">
          {selectedProducts.map((product) => (
            <span key={product.id} className="inline-flex items-center gap-2 rounded-full border border-blue-200 bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-700">
              {productChipSummary(product)}
              <button type="button" onClick={() => removeProduct(product.id)} className="rounded-full border border-blue-200 px-1.5 py-0.5 text-[10px] leading-none transition hover:bg-blue-100">X</button>
            </span>
          ))}

          {selectedProducts.length > 0 ? (
            <button type="button" onClick={() => { const url = `${window.location.origin}/so-sanh?items=${selectedIds.join(",")}`; navigator.clipboard?.writeText(url); }} className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-100">Sao ch?p link</button>
          ) : null}

          {selectedProducts.length > 0 ? (
            <button type="button" onClick={clearAll} className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-100">Clear all</button>
          ) : null}
        </div>
      </section>

      <section className="mt-6 space-y-4">
        <ComparisonTable
          products={selectedProducts}
          onRemove={removeProduct}
          onSpecsToggle={handleToggleSpecs}
          onDiffToggle={handleToggleDifferences}
          onlyDifferences={onlyDifferences}
          showAllSpecs={showAllSpecs}
          isLoading={isPreparingTable}
          hasError={hasCompareError}
          onRetry={retryComparison}
          cellHighlightClass={cellHighlightClass}
          isLowestPrice={isLowestPrice}
          isHighestScore={isHighestScore}
          isHighestDiscount={isHighestDiscount}
          textOrUpdating={textOrUpdating}
          listOrUpdating={listOrUpdating}
          showSpecsToggle={true}
          showDiffToggle={true}
          differenceBadgeText={showDiffBadge ? "Äang lá»c khÃ¡c biá»‡t" : ""}
          tableLegend="Ã” xanh lÃ  sáº£n pháº©m tháº¯ng theo tá»«ng tiÃªu chÃ­."
        />
      </section>
    </PageContainer>
  );
}

