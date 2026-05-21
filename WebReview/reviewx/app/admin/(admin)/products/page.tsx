"use client";

import Image from "next/image";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer } from "@/components/ui";

type ProductStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
type FilterType = "" | "missing-image" | "missing-specs" | "missing-pros-cons";
type SortType = "updatedAt" | "createdAt" | "price" | "score" | "discount" | "affiliate";

type AdminProduct = {
  id: string;
  name: string;
  slug: string;
  thumbnail: string | null;
  priceMin: number | null;
  priceMax: number | null;
  worthScore: number | null;
  status: ProductStatus;
  brand: { name: string } | null;
  category: { name: string } | null;
  images: { url: string }[];
  affiliateLinks: { id: string; status: string }[];
  updatedAt: string;
};

type ProductsApiResponse = {
  success: boolean;
  data?: {
    items: AdminProduct[];
    total: number;
    page: number;
    limit: number;
  };
  error?: string;
};

const PAGE_SIZE = 10;

function statusClass(status: ProductStatus) {
  if (status === "PUBLISHED") return "border-emerald-200 bg-emerald-50 text-emerald-700";
  if (status === "ARCHIVED") return "border-slate-300 bg-slate-100 text-slate-700";
  return "border-amber-200 bg-amber-50 text-amber-700";
}

function formatPrice(min: number | null, max: number | null) {
  if (min === null && max === null) return "—";
  if (min !== null && max !== null && min === max) return `${min.toLocaleString("vi-VN")}đ`;
  if (min !== null && max !== null) return `${min.toLocaleString("vi-VN")}đ – ${max.toLocaleString("vi-VN")}đ`;
  if (min !== null) return `Từ ${min.toLocaleString("vi-VN")}đ`;
  if (max !== null) return `Đến ${max.toLocaleString("vi-VN")}đ`;
  return "—";
}

function affiliateStatus(links: AdminProduct["affiliateLinks"]) {
  if (!links.length) return { label: "Thiếu link", className: "border-red-200 bg-red-50 text-red-700" };
  const active = links.filter((item) => item.status === "ACTIVE").length;
  if (active > 0) return { label: `${active} link active`, className: "border-emerald-200 bg-emerald-50 text-emerald-700" };
  return { label: `${links.length} link`, className: "border-amber-200 bg-amber-50 text-amber-700" };
}

function scoreClass(score: number | null) {
  if (score === null) return "border-slate-200 bg-slate-100 text-slate-600";
  if (score >= 8) return "border-emerald-200 bg-emerald-50 text-emerald-700";
  if (score >= 6) return "border-blue-200 bg-blue-50 text-blue-700";
  return "border-amber-200 bg-amber-50 text-amber-700";
}

function formatDate(value: string) {
  return new Date(value).toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function AdminProductsPage() {
  const [products, setProducts] = useState<AdminProduct[]>([]);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<"ALL" | ProductStatus>("ALL");
  const [filter, setFilter] = useState<FilterType>("");
  const [sort, setSort] = useState<SortType>("updatedAt");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("desc");
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [total, setTotal] = useState(0);

  const totalPages = Math.ceil(total / PAGE_SIZE);

  const loadProducts = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const params = new URLSearchParams({ page: String(page), limit: String(PAGE_SIZE) });
      if (query.trim()) params.set("q", query.trim());
      if (status !== "ALL") params.set("status", status);
      if (filter) params.set("filter", filter);
      params.set("sort", sort);
      params.set("dir", sortDir);

      const response = await fetch(`/api/admin/products?${params.toString()}`);
      const payload = (await response.json()) as ProductsApiResponse;

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.error ?? "Load failed");
      }

      setProducts(payload.data.items);
      setTotal(payload.data.total);
    } catch (cause) {
      setProducts([]);
      setError(cause instanceof Error ? cause.message : "Không tải được sản phẩm");
    } finally {
      setLoading(false);
    }
  }, [filter, page, query, sort, sortDir, status]);

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadProducts();
    }, 0);
    return () => clearTimeout(timer);
  }, [loadProducts]);

  const items = useMemo(() => products, [products]);

  async function togglePublish(id: string, current: ProductStatus) {
    if (current === "ARCHIVED") return;
    const next = current === "PUBLISHED" ? "DRAFT" : "PUBLISHED";
    await fetch(`/api/admin/products/${id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: next }),
    });
    await loadProducts();
  }

  async function archiveProduct(id: string) {
    const confirmed = window.confirm("Bạn có chắc muốn archive sản phẩm này?");
    if (!confirmed) return;
    await fetch(`/api/admin/products/${id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "ARCHIVED" }),
    });
    await loadProducts();
  }

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Admin Products</h1>
            <p className="mt-1 text-sm text-slate-600">
              Quản lý danh sách sản phẩm, trạng thái xuất bản và hành động nhanh.
            </p>
          </div>
          <Link
            href="/admin"
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          >
            Về dashboard
          </Link>
        </div>

        <div className="mt-4 grid gap-2 lg:grid-cols-6">
          <input
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setPage(1);
            }}
            placeholder="Tìm theo tên hoặc slug"
            className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 lg:col-span-3"
          />
          <select
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as "ALL" | ProductStatus);
              setPage(1);
            }}
            className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="DRAFT">Draft</option>
            <option value="PUBLISHED">Published</option>
            <option value="ARCHIVED">Archived</option>
          </select>
          <select
            value={filter}
            onChange={(event) => {
              setFilter(event.target.value as FilterType);
              setPage(1);
            }}
            className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          >
            <option value="">Tất cả bộ lọc</option>
            <option value="missing-image">Thiếu ảnh</option>
            <option value="missing-specs">Thiếu specs</option>
            <option value="missing-pros-cons">Thiếu pros/cons</option>
          </select>
          <select
            value={sort}
            onChange={(event) => {
              setSort(event.target.value as SortType);
              setPage(1);
            }}
            className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          >
            <option value="updatedAt">Sắp xếp: Cập nhật</option>
            <option value="createdAt">Sắp xếp: Tạo mới</option>
            <option value="price">Sắp xếp: Giá</option>
            <option value="score">Sắp xếp: Điểm</option>
            <option value="discount">Sắp xếp: Giảm giá</option>
            <option value="affiliate">Sắp xếp: Affiliate</option>
          </select>
          <select
            value={sortDir}
            onChange={(event) => {
              setSortDir(event.target.value as "asc" | "desc");
              setPage(1);
            }}
            className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          >
            <option value="desc">Giảm dần</option>
            <option value="asc">Tăng dần</option>
          </select>
          <Link
            href="/admin/products/new"
            className="h-11 rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-center text-xs font-semibold text-blue-700 hover:bg-blue-100"
          >
            + Sản phẩm
          </Link>
        </div>

        {error ? (
          <div className="mt-4">
            <ErrorState title="Lỗi tải products" message={error} />
          </div>
        ) : null}

        {loading ? (
          <div className="mt-4 grid gap-2">
            <LoadingSkeleton className="h-12 w-full" />
            <LoadingSkeleton className="h-12 w-full" />
          </div>
        ) : null}

        {!loading && items.length === 0 ? (
          <div className="mt-4">
            <EmptyState title="Không có sản phẩm phù hợp" message="Hãy đổi bộ lọc hoặc từ khóa tìm kiếm." />
          </div>
        ) : null}

        {!loading && items.length > 0 ? (
          <>
            <div className="mt-4 flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm text-slate-600">
                Hiển thị {items.length} / {total} sản phẩm
              </p>
            </div>
            <div className="mt-4 overflow-x-auto">
              <table className="w-full min-w-[1200px] text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-600">
                    <th className="px-2 py-2 font-semibold">Ảnh</th>
                    <th className="px-2 py-2 font-semibold">Sản phẩm</th>
                    <th className="px-2 py-2 font-semibold">Thương hiệu</th>
                    <th className="px-2 py-2 font-semibold">Danh mục</th>
                    <th className="px-2 py-2 font-semibold">Giá</th>
                    <th className="px-2 py-2 font-semibold">Điểm</th>
                    <th className="px-2 py-2 font-semibold">Trạng thái</th>
                    <th className="px-2 py-2 font-semibold">Affiliate</th>
                    <th className="px-2 py-2 font-semibold">Cập nhật</th>
                    <th className="px-2 py-2 font-semibold">Hành động</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((row) => {
                    const thumb = row.thumbnail ?? row.images[0]?.url ?? null;
                    const affiliate = affiliateStatus(row.affiliateLinks);

                    return (
                      <tr key={row.id} className="border-b border-slate-100">
                        <td className="px-2 py-2">
                          {thumb ? (
                            <div className="relative h-10 w-10 overflow-hidden rounded-lg border border-slate-200 bg-slate-100">
                              <Image src={thumb} alt={row.name} fill className="object-cover" unoptimized />
                            </div>
                          ) : (
                            <div className="h-10 w-10 rounded-lg border border-slate-200 bg-slate-100" />
                          )}
                        </td>
                        <td className="px-2 py-2">
                          <div className="font-medium text-slate-800">{row.name}</div>
                          <div className="text-xs text-slate-500">{row.slug}</div>
                        </td>
                        <td className="px-2 py-2 text-slate-700">{row.brand?.name ?? "-"}</td>
                        <td className="px-2 py-2 text-slate-700">{row.category?.name ?? "-"}</td>
                        <td className="px-2 py-2 text-slate-700">{formatPrice(row.priceMin, row.priceMax)}</td>
                        <td className="px-2 py-2">
                          <span className={`rounded-full border px-2 py-0.5 text-xs font-semibold ${scoreClass(row.worthScore)}`}>
                            {row.worthScore !== null ? row.worthScore.toFixed(1) : "—"}
                          </span>
                        </td>
                        <td className="px-2 py-2">
                          <span className={`rounded-full border px-2 py-0.5 text-xs font-semibold ${statusClass(row.status)}`}>
                            {row.status}
                          </span>
                        </td>
                        <td className="px-2 py-2">
                          <span className={`rounded-full border px-2 py-0.5 text-xs font-semibold ${affiliate.className}`}>
                            {affiliate.label}
                          </span>
                        </td>
                        <td className="px-2 py-2 text-xs text-slate-600">{formatDate(row.updatedAt)}</td>
                        <td className="px-2 py-2">
                          <div className="flex flex-wrap gap-1">
                            <Link
                              href={`/admin/products/${row.id}`}
                              className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700"
                            >
                              Edit
                            </Link>
                            <Link
                              href={`/san-pham/${row.slug}`}
                              target="_blank"
                              className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700"
                            >
                              Preview
                            </Link>
                            <button
                              type="button"
                              onClick={() => void togglePublish(row.id, row.status)}
                              className="rounded-lg border border-blue-200 bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700"
                            >
                              Toggle
                            </button>
                            <button
                              type="button"
                              onClick={() => void archiveProduct(row.id)}
                              className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs font-semibold text-red-700"
                            >
                              Archive
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {totalPages > 1 ? (
              <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
                <button
                  type="button"
                  onClick={() => setPage((current) => Math.max(1, current - 1))}
                  disabled={page <= 1}
                  className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-40"
                >
                  ←
                </button>
                {Array.from({ length: totalPages }, (_, index) => index + 1)
                  .filter((value) => value === 1 || value === totalPages || Math.abs(value - page) <= 2)
                  .map((value) => (
                    <button
                      key={value}
                      type="button"
                      onClick={() => setPage(value)}
                      className={`rounded-lg border px-3 py-1.5 text-sm font-semibold ${
                        page === value
                          ? "border-blue-400 bg-blue-50 text-blue-700"
                          : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                      }`}
                    >
                      {value}
                    </button>
                  ))}
                <button
                  type="button"
                  onClick={() => setPage((current) => Math.min(totalPages, current + 1))}
                  disabled={page >= totalPages}
                  className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-40"
                >
                  →
                </button>
              </div>
            ) : null}
          </>
        ) : null}
      </section>
    </PageContainer>
  );
}
