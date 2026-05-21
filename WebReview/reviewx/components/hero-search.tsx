"use client";

import { useMemo, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { GlassSearchInput, GradientButton } from "./ui";

const popularKeywords = [
  "Tai nghe bluetooth",
  "Nồi chiên không dầu",
  "Bàn phím cơ",
  "Sữa rửa mặt",
  "Máy hút bụi mini",
];

function isLikelyShopeeUrl(value: string) {
  return /(shopee\.vn|s\.shopee\.vn|shp\.ee|shopee\.ee)/i.test(value);
}

export function HeroSearch() {
  const [query, setQuery] = useState("");
  const [isPending, startTransition] = useTransition();
  const router = useRouter();

  const trimmed = useMemo(() => query.trim(), [query]);
  const looksLikeShopee = isLikelyShopeeUrl(trimmed);
  const invalidUrl = trimmed.startsWith("http") && !looksLikeShopee;

  const goSearch = (keyword: string) => {
    const q = keyword.trim();
    if (!q) return;

    startTransition(() => {
      if (isLikelyShopeeUrl(q)) {
        router.push(`/tim-kiem?q=${encodeURIComponent(q)}&mode=short-link`);
        return;
      }
      router.push(`/tim-kiem?q=${encodeURIComponent(q)}`);
    });
  };

  return (
    <div className="space-y-5">
      <p className="inline-flex max-w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-1 text-xs text-slate-600 sm:text-sm">
        <span className="truncate">Review-commerce platform</span>
      </p>

      <h1 className="break-words text-4xl font-bold leading-tight text-slate-900 sm:text-5xl">
        Check trước khi mua. <br className="hidden sm:block" /> Mua gì cũng đáng!
      </h1>

      <p className="text-slate-600">Review chi tiết - So sánh khách quan - Gợi ý đáng tin cậy</p>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          goSearch(trimmed);
        }}
        className="max-w-full overflow-hidden rounded-3xl border border-slate-200/70 bg-white/80 p-2 shadow-sm backdrop-blur"
      >
        <div className="flex min-w-0 flex-col gap-2 sm:flex-row">
          <GlassSearchInput value={query} onChange={setQuery} placeholder="Bạn đang tìm sản phẩm gì?" ariaLabel="Hero search" className="border-transparent" />
          <GradientButton className="w-full sm:w-auto">{isPending ? "Đang tìm..." : "Tìm kiếm"}</GradientButton>
        </div>
      </form>

      {invalidUrl ? <p className="text-sm text-red-600">URL chưa hợp lệ. Hãy dùng link Shopee hoặc từ khóa.</p> : null}
      {looksLikeShopee ? <p className="text-sm text-amber-600">Đã nhận diện link Shopee, sẽ chuyển sang luồng xử lý short-link.</p> : null}

      <div className="flex min-w-0 max-w-full gap-2 overflow-x-auto pb-1">
        {popularKeywords.map((keyword) => (
          <button
            key={keyword}
            onClick={() => goSearch(keyword)}
            className="shrink-0 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-100 focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-1"
            type="button"
          >
            {keyword}
          </button>
        ))}
      </div>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => router.push("/deals")}
          className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800"
        >
          Xem deal hot
        </button>
        <button
          type="button"
          onClick={() => router.push("/cong-cu/chon-san-pham")}
          className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-800 transition hover:bg-slate-100"
        >
          Dùng công cụ chọn sản phẩm
        </button>
      </div>

      <p className="text-xs text-slate-500">
        Disclosure: ReviewX có thể nhận hoa hồng từ liên kết affiliate, nhưng không làm thay đổi giá bạn mua.
      </p>
    </div>
  );
}
