"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer } from "@/components/ui";

type ReviewStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

type ReviewRow = {
  id: string;
  title: string;
  slug: string;
  author: string | null;
  score: number | null;
  status: ReviewStatus;
  publishedAt: string | null;
  updatedAt: string;
  product: { id: string; name: string } | null;
  category: { id: string; name: string } | null;
};

type OptionItem = { id: string; name: string };

function statusClass(status: ReviewStatus) {
  if (status === "PUBLISHED") return "border-emerald-200 bg-emerald-50 text-emerald-700";
  if (status === "ARCHIVED") return "border-slate-300 bg-slate-100 text-slate-700";
  return "border-amber-200 bg-amber-50 text-amber-700";
}

function formatDate(value: string | null) {
  if (!value) return "—";
  return new Date(value).toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function AdminReviewsPage() {
  const [rows, setRows] = useState<ReviewRow[]>([]);
  const [categories, setCategories] = useState<OptionItem[]>([]);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<"ALL" | ReviewStatus>("ALL");
  const [categoryId, setCategoryId] = useState("");
  const [author, setAuthor] = useState("");
  const [sort, setSort] = useState<"publishedAt" | "updatedAt" | "score">("updatedAt");
  const [dir, setDir] = useState<"asc" | "desc">("desc");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [toast, setToast] = useState("");
  const [pendingId, setPendingId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ limit: "100", sort, dir });
      if (query.trim()) params.set("q", query.trim());
      if (status !== "ALL") params.set("status", status);
      if (categoryId) params.set("categoryId", categoryId);
      if (author.trim()) params.set("author", author.trim());
      const response = await fetch(`/api/admin/reviews?${params.toString()}`);
      const payload = (await response.json()) as {
        success: boolean;
        data?: { items: ReviewRow[] };
        error?: string;
      };
      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.error ?? "Không tải được reviews.");
      }
      setRows(payload.data.items);
    } catch (cause) {
      setRows([]);
      setError(cause instanceof Error ? cause.message : "Không tải được reviews.");
    } finally {
      setLoading(false);
    }
  }, [author, categoryId, dir, query, sort, status]);

  const loadCategories = useCallback(async () => {
    try {
      const response = await fetch("/api/admin/categories?limit=200");
      const payload = (await response.json()) as {
        success: boolean;
        data?: { items: OptionItem[] };
      };
      if (response.ok && payload.success && payload.data) {
        setCategories(payload.data.items);
      } else {
        setCategories([]);
      }
    } catch {
      setCategories([]);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    void loadCategories();
  }, [loadCategories]);

  const items = useMemo(() => rows, [rows]);

  async function updateStatus(id: string, nextStatus: ReviewStatus, successMessage: string) {
    setPendingId(id);
    setError("");
    setToast("");
    try {
      const response = await fetch(`/api/admin/reviews/${id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status: nextStatus }),
      });
      const payload = (await response.json()) as { success: boolean; error?: string };
      if (!response.ok || !payload.success) {
        throw new Error(payload.error ?? "Không cập nhật được review.");
      }
      setToast(successMessage);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không cập nhật được review.");
    } finally {
      setPendingId(null);
    }
  }

  async function togglePublish(id: string, currentStatus: ReviewStatus) {
    if (currentStatus === "ARCHIVED") return;
    const nextStatus = currentStatus === "PUBLISHED" ? "DRAFT" : "PUBLISHED";
    await updateStatus(
      id,
      nextStatus,
      nextStatus === "PUBLISHED" ? "Đã publish review." : "Đã chuyển review về draft.",
    );
  }

  async function archiveReview(id: string) {
    if (!window.confirm("Bạn có chắc muốn archive review này?")) return;
    await updateStatus(id, "ARCHIVED", "Đã archive review.");
  }

  async function deleteReview(id: string) {
    if (!window.confirm("Bạn có chắc muốn xóa review này?")) return;
    setPendingId(id);
    setError("");
    setToast("");
    try {
      const response = await fetch(`/api/admin/reviews/${id}`, { method: "DELETE" });
      const payload = (await response.json()) as { success: boolean; error?: string };
      if (!response.ok || !payload.success) {
        throw new Error(payload.error ?? "Không xóa được review.");
      }
      setToast("Đã xóa review.");
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không xóa được review.");
    } finally {
      setPendingId(null);
    }
  }

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Admin Reviews</h1>
            <p className="mt-1 text-sm text-slate-600">Quản lý bài review, trạng thái và nội dung xuất bản.</p>
          </div>
          <div className="flex items-center gap-2">
            <Link
              href="/admin/reviews/new"
              className="rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-100"
            >
              + New Review
            </Link>
            <Link
              href="/admin"
              className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
            >
              Về dashboard
            </Link>
          </div>
        </div>

        <div className="mt-4 grid gap-2 lg:grid-cols-6">
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search title / slug / product"
            className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 lg:col-span-2"
          />
          <select
            value={status}
            onChange={(event) => setStatus(event.target.value as "ALL" | ReviewStatus)}
            className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          >
            <option value="ALL">Tất cả status</option>
            <option value="DRAFT">Draft</option>
            <option value="PUBLISHED">Published</option>
            <option value="ARCHIVED">Archived</option>
          </select>
          <select
            value={categoryId}
            onChange={(event) => setCategoryId(event.target.value)}
            className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          >
            <option value="">Tất cả category</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
          <input
            value={author}
            onChange={(event) => setAuthor(event.target.value)}
            placeholder="Filter author"
            className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          />
          <select
            value={sort}
            onChange={(event) => setSort(event.target.value as "publishedAt" | "updatedAt" | "score")}
            className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          >
            <option value="updatedAt">Sort: Updated</option>
            <option value="publishedAt">Sort: Published</option>
            <option value="score">Sort: Score</option>
          </select>
          <select
            value={dir}
            onChange={(event) => setDir(event.target.value as "asc" | "desc")}
            className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          >
            <option value="desc">Giảm dần</option>
            <option value="asc">Tăng dần</option>
          </select>
        </div>

        {error ? (
          <div className="mt-4">
            <ErrorState title="Lỗi tải reviews" message={error} />
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
            <EmptyState title="Không có review phù hợp" message="Hãy đổi từ khóa hoặc bộ lọc." />
          </div>
        ) : null}

        {!loading && items.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full min-w-[1200px] text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-600">
                  <th className="px-2 py-2 font-semibold">Title</th>
                  <th className="px-2 py-2 font-semibold">Slug</th>
                  <th className="px-2 py-2 font-semibold">Product</th>
                  <th className="px-2 py-2 font-semibold">Category</th>
                  <th className="px-2 py-2 font-semibold">Author</th>
                  <th className="px-2 py-2 font-semibold">Score</th>
                  <th className="px-2 py-2 font-semibold">Status</th>
                  <th className="px-2 py-2 font-semibold">Published</th>
                  <th className="px-2 py-2 font-semibold">Updated</th>
                  <th className="px-2 py-2 font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((row) => (
                  <tr key={row.id} className="border-b border-slate-100">
                    <td className="px-2 py-2 text-slate-800">{row.title}</td>
                    <td className="px-2 py-2 text-slate-700">{row.slug}</td>
                    <td className="px-2 py-2 text-slate-700">{row.product?.name ?? "—"}</td>
                    <td className="px-2 py-2 text-slate-700">{row.category?.name ?? "—"}</td>
                    <td className="px-2 py-2 text-slate-700">{row.author ?? "ReviewX"}</td>
                    <td className="px-2 py-2 text-slate-700">
                      {row.score !== null ? row.score.toFixed(1) : "—"}
                    </td>
                    <td className="px-2 py-2">
                      <span className={`rounded-full border px-2 py-0.5 text-xs font-semibold ${statusClass(row.status)}`}>
                        {row.status}
                      </span>
                    </td>
                    <td className="px-2 py-2 text-slate-700">{formatDate(row.publishedAt)}</td>
                    <td className="px-2 py-2 text-slate-700">{formatDate(row.updatedAt)}</td>
                    <td className="px-2 py-2">
                      <div className="flex flex-wrap gap-1">
                        <Link
                          href={`/admin/reviews/${row.id}`}
                          className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700"
                        >
                          Edit
                        </Link>
                        <Link
                          href={`/review/${row.slug}`}
                          target="_blank"
                          className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700"
                        >
                          Preview
                        </Link>
                        <button
                          type="button"
                          onClick={() => void togglePublish(row.id, row.status)}
                          disabled={pendingId === row.id || row.status === "ARCHIVED"}
                          className="rounded-lg border border-blue-200 bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700 disabled:opacity-60"
                        >
                          Publish/unpublish
                        </button>
                        <button
                          type="button"
                          onClick={() => void archiveReview(row.id)}
                          disabled={pendingId === row.id || row.status === "ARCHIVED"}
                          className="rounded-lg border border-slate-300 bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-700 disabled:opacity-60"
                        >
                          Archive
                        </button>
                        <button
                          type="button"
                          onClick={() => void deleteReview(row.id)}
                          disabled={pendingId === row.id}
                          className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs font-semibold text-red-700 disabled:opacity-60"
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}

        {toast ? (
          <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
            {toast}
          </div>
        ) : null}
      </section>
    </PageContainer>
  );
}
