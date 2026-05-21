"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ErrorState, LoadingSkeleton } from "@/components/ui";
import { RichTextEditor } from "@/components/rich-text-editor";

type ReviewStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
type ReviewBlockType =
  | "heading"
  | "paragraph"
  | "image"
  | "spec_table"
  | "pros_cons"
  | "score_breakdown"
  | "cta_shopee"
  | "faq";

type ReviewFormState = {
  title: string;
  slug: string;
  productId: string;
  categoryId: string;
  author: string;
  status: ReviewStatus;
  publishedAt: string;
  readingTimeMinutes: string;
  summary: string;
  content: string;
  coverImage: string;
  score: string;
  verdict: string;
  pros: string;
  cons: string;
  scoreBreakdown: {
    design: string;
    performance: string;
    features: string;
    priceValue: string;
    durability: string;
    userExperience: string;
    overall: string;
  };
  faqItems: Array<{ question: string; answer: string }>;
  relatedReviewIds: string[];
  relatedProductIds: string[];
  blocks: Array<{ type: ReviewBlockType; content: string }>;
  ctaBlocks: string;
  seoTitle: string;
  seoDescription: string;
  seoOgImage: string;
  canonicalUrl: string;
  noindex: boolean;
};

type ApiResponse<T> = { success: boolean; data?: T; error?: string };

type ReviewDetail = {
  id: string;
  title: string;
  slug: string;
  productId: string;
  categoryId: string | null;
  author: string | null;
  status: ReviewStatus;
  publishedAt: string | null;
  readingTimeMinutes: number | null;
  summary: string | null;
  content: string | null;
  coverImage: string | null;
  score: number | null;
  verdict: string | null;
  pros: string | null;
  cons: string | null;
  scoreBreakdown: string | null;
  faqs: Array<{ question: string; answer: string; sortOrder: number }>;
  relatedReviewIds: string | null;
  relatedProductIds: string | null;
  contentBlocks: string | null;
  ctaBlocks: string | null;
  seoTitle: string | null;
  seoDescription: string | null;
  seoOgImage: string | null;
  canonicalUrl: string | null;
  noindex: boolean;
};

const initialState: ReviewFormState = {
  title: "",
  slug: "",
  productId: "",
  categoryId: "",
  author: "ReviewX",
  status: "DRAFT",
  publishedAt: "",
  readingTimeMinutes: "",
  summary: "",
  content: "",
  coverImage: "",
  score: "",
  verdict: "",
  pros: "",
  cons: "",
  scoreBreakdown: {
    design: "",
    performance: "",
    features: "",
    priceValue: "",
    durability: "",
    userExperience: "",
    overall: "",
  },
  faqItems: [],
  relatedReviewIds: [],
  relatedProductIds: [],
  blocks: [],
  ctaBlocks: "",
  seoTitle: "",
  seoDescription: "",
  seoOgImage: "",
  canonicalUrl: "",
  noindex: false,
};

function slugify(value: string): string {
  return value
    .toLowerCase()
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}

function toDateInput(value: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function parseJsonList(raw: string | null): string[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.map((value) => String(value)).filter(Boolean);
  } catch {
    return [];
  }
}

function parseContentBlocks(raw: string | null): Array<{ type: ReviewBlockType; content: string }> {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as Array<{ type: ReviewBlockType; content: string }>;
    if (!Array.isArray(parsed)) return [];
    return parsed
      .map((item) => ({ type: item.type, content: item.content ?? "" }))
      .filter((item) => item.type && item.content.trim());
  } catch {
    return [];
  }
}

function parseScoreBreakdown(raw: string | null): ReviewFormState["scoreBreakdown"] {
  const fallback = initialState.scoreBreakdown;
  if (!raw) return fallback;
  try {
    const parsed = JSON.parse(raw) as Record<string, number>;
    return {
      design: parsed.design?.toString() ?? "",
      performance: parsed.performance?.toString() ?? "",
      features: parsed.features?.toString() ?? "",
      priceValue: parsed.priceValue?.toString() ?? "",
      durability: parsed.durability?.toString() ?? "",
      userExperience: parsed.userExperience?.toString() ?? "",
      overall: parsed.overall?.toString() ?? "",
    };
  } catch {
    return fallback;
  }
}

function toNumberOrNull(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) return null;
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : null;
}

function isValidUrl(value: string): boolean {
  if (!value.trim()) return false;
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

function safeParseIds(raw: string | null): string[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.map((id) => String(id).trim()).filter(Boolean);
  } catch {
    return [];
  }
}

type AdminReviewFormProps = {
  mode: "create" | "edit";
  reviewId?: string;
};

export function AdminReviewForm({ mode, reviewId }: AdminReviewFormProps) {
  const router = useRouter();
  const [form, setForm] = useState<ReviewFormState>(initialState);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(mode === "edit");
  const [saving, setSaving] = useState(false);
  const [apiError, setApiError] = useState("");
  const [toast, setToast] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [slugTouched, setSlugTouched] = useState(false);
  const [products, setProducts] = useState<Array<{ id: string; name: string }>>([]);
  const [categories, setCategories] = useState<Array<{ id: string; name: string }>>([]);
  const [reviews, setReviews] = useState<Array<{ id: string; title: string }>>([]);
  const initialSnapshotRef = useRef("");

  const snapshot = useMemo(() => JSON.stringify(form), [form]);
  const isDirty = initialSnapshotRef.current !== "" && snapshot !== initialSnapshotRef.current;

  useEffect(() => {
    if (!slugTouched && mode === "create" && form.title.trim()) {
      setForm((prev) => ({ ...prev, slug: slugify(prev.title) }));
    }
  }, [form.title, mode, slugTouched]);

  useEffect(() => {
    const handler = (event: BeforeUnloadEvent) => {
      if (!isDirty) return;
      event.preventDefault();
      event.returnValue = true;
    };
    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [isDirty]);

  const loadOptions = useCallback(async () => {
    const [productsRes, categoriesRes, reviewsRes] = await Promise.all([
      fetch("/api/admin/products?limit=200"),
      fetch("/api/admin/categories?limit=200"),
      fetch("/api/admin/reviews?limit=200"),
    ]);
    const productsJson = (await productsRes.json()) as ApiResponse<{ items: Array<{ id: string; name: string }> }>;
    const categoriesJson = (await categoriesRes.json()) as ApiResponse<{ items: Array<{ id: string; name: string }> }>;
    const reviewsJson = (await reviewsRes.json()) as ApiResponse<{ items: Array<{ id: string; title: string }> }>;

    if (productsRes.ok && productsJson.success && productsJson.data) setProducts(productsJson.data.items);
    if (categoriesRes.ok && categoriesJson.success && categoriesJson.data) setCategories(categoriesJson.data.items);
    if (reviewsRes.ok && reviewsJson.success && reviewsJson.data) setReviews(reviewsJson.data.items);
  }, []);

  const loadReview = useCallback(async () => {
    if (mode !== "edit" || !reviewId) return;
    setLoading(true);
    setApiError("");
    try {
      const response = await fetch(`/api/admin/reviews/${reviewId}`);
      const payload = (await response.json()) as ApiResponse<ReviewDetail>;
      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.error ?? "Không tải được review.");
      }
      const review = payload.data;
      const mapped: ReviewFormState = {
        title: review.title,
        slug: review.slug,
        productId: review.productId,
        categoryId: review.categoryId ?? "",
        author: review.author ?? "ReviewX",
        status: review.status,
        publishedAt: toDateInput(review.publishedAt),
        readingTimeMinutes: review.readingTimeMinutes?.toString() ?? "",
        summary: review.summary ?? "",
        content: review.content ?? "",
        coverImage: review.coverImage ?? "",
        score: review.score?.toString() ?? "",
        verdict: review.verdict ?? "",
        pros: review.pros ?? "",
        cons: review.cons ?? "",
        scoreBreakdown: parseScoreBreakdown(review.scoreBreakdown),
        faqItems: review.faqs.map((faq) => ({ question: faq.question, answer: faq.answer })),
        relatedReviewIds: safeParseIds(review.relatedReviewIds),
        relatedProductIds: safeParseIds(review.relatedProductIds),
        blocks: parseContentBlocks(review.contentBlocks),
        ctaBlocks: review.ctaBlocks ?? "",
        seoTitle: review.seoTitle ?? "",
        seoDescription: review.seoDescription ?? "",
        seoOgImage: review.seoOgImage ?? "",
        canonicalUrl: review.canonicalUrl ?? "",
        noindex: review.noindex,
      };
      setForm(mapped);
      initialSnapshotRef.current = JSON.stringify(mapped);
    } catch (error) {
      setApiError(error instanceof Error ? error.message : "Không tải được review.");
    } finally {
      setLoading(false);
    }
  }, [mode, reviewId]);

  useEffect(() => {
    void loadOptions();
  }, [loadOptions]);

  useEffect(() => {
    if (mode === "create") {
      initialSnapshotRef.current = JSON.stringify(initialState);
      setLoading(false);
      return;
    }
    void loadReview();
  }, [loadReview, mode]);

  function fieldClass(field: string) {
    return `h-11 w-full rounded-xl border px-3 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 ${
      errors[field] ? "border-red-300 bg-red-50" : "border-slate-200"
    }`;
  }

  function validate(nextStatus: ReviewStatus) {
    const next: Record<string, string> = {};
    if (!form.title.trim()) next.title = "Title là bắt buộc.";
    if (!form.slug.trim()) next.slug = "Slug là bắt buộc.";
    if (!form.productId.trim()) next.productId = "Product là bắt buộc.";
    if (!form.author.trim()) next.author = "Author là bắt buộc.";

    const score = toNumberOrNull(form.score);
    if (score !== null && (score < 0 || score > 10)) next.score = "Score phải trong khoảng 0-10.";
    if (nextStatus === "PUBLISHED") {
      if (!form.summary.trim()) next.summary = "Published yêu cầu summary.";
      if (!form.content.trim()) next.content = "Published yêu cầu content.";
      if (score === null) next.score = "Published yêu cầu score.";
    }

    if (form.coverImage.trim() && !isValidUrl(form.coverImage)) {
      next.coverImage = "Cover image URL không hợp lệ.";
    }
    if (form.seoOgImage.trim() && !isValidUrl(form.seoOgImage)) {
      next.seoOgImage = "SEO OG image URL không hợp lệ.";
    }
    if (form.canonicalUrl.trim() && !isValidUrl(form.canonicalUrl)) {
      next.canonicalUrl = "Canonical URL không hợp lệ.";
    }
    return next;
  }

  function toPayload(nextStatus: ReviewStatus) {
    const score = toNumberOrNull(form.score);
    return {
      title: form.title.trim(),
      slug: form.slug.trim(),
      productId: form.productId,
      categoryId: form.categoryId || null,
      author: form.author.trim() || null,
      status: nextStatus,
      publishedAt: form.publishedAt ? new Date(form.publishedAt).toISOString() : null,
      readingTimeMinutes: toNumberOrNull(form.readingTimeMinutes),
      summary: form.summary.trim() || null,
      content: form.content.trim() || null,
      coverImage: form.coverImage.trim() || null,
      score,
      verdict: form.verdict.trim() || null,
      pros: form.pros.trim() || null,
      cons: form.cons.trim() || null,
      scoreBreakdown: {
        design: toNumberOrNull(form.scoreBreakdown.design) ?? 0,
        performance: toNumberOrNull(form.scoreBreakdown.performance) ?? 0,
        features: toNumberOrNull(form.scoreBreakdown.features) ?? 0,
        priceValue: toNumberOrNull(form.scoreBreakdown.priceValue) ?? 0,
        durability: toNumberOrNull(form.scoreBreakdown.durability) ?? 0,
        userExperience: toNumberOrNull(form.scoreBreakdown.userExperience) ?? 0,
        overall: toNumberOrNull(form.scoreBreakdown.overall) ?? score ?? 0,
      },
      faqItems: form.faqItems
        .map((item, index) => ({
          question: item.question.trim(),
          answer: item.answer.trim(),
          sortOrder: index,
        }))
        .filter((item) => item.question && item.answer),
      contentBlocks: form.blocks
        .map((block) => ({ type: block.type, content: block.content.trim() }))
        .filter((block) => block.content),
      ctaBlocks: form.ctaBlocks.trim() || null,
      relatedReviewIds: form.relatedReviewIds,
      relatedProductIds: form.relatedProductIds,
      seoTitle: form.seoTitle.trim() || null,
      seoDescription: form.seoDescription.trim() || null,
      seoOgImage: form.seoOgImage.trim() || null,
      canonicalUrl: form.canonicalUrl.trim() || null,
      noindex: form.noindex,
    };
  }

  async function submit(nextStatus: ReviewStatus) {
    const nextErrors = validate(nextStatus);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      setToast({ type: "error", message: "Form còn lỗi, vui lòng kiểm tra lại." });
      return;
    }

    setSaving(true);
    setToast(null);
    setApiError("");
    try {
      const payload = toPayload(nextStatus);
      const endpoint = mode === "create" ? "/api/admin/reviews" : `/api/admin/reviews/${reviewId}`;
      const method = mode === "create" ? "POST" : "PATCH";
      const response = await fetch(endpoint, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const result = (await response.json()) as ApiResponse<{ id: string }>;
      if (!response.ok || !result.success) {
        throw new Error(result.error ?? "Không lưu được review.");
      }
      setToast({ type: "success", message: nextStatus === "PUBLISHED" ? "Đã publish review." : "Đã lưu review." });
      initialSnapshotRef.current = JSON.stringify(form);
      if (mode === "create") {
        router.push("/admin/reviews");
      } else {
        await loadReview();
      }
    } catch (error) {
      setToast({ type: "error", message: error instanceof Error ? error.message : "Không lưu được review." });
    } finally {
      setSaving(false);
    }
  }

  async function archiveReview() {
    if (mode !== "edit" || !reviewId) return;
    const confirmed = window.confirm("Bạn có chắc muốn archive review này?");
    if (!confirmed) return;
    await submit("ARCHIVED");
  }

  async function deleteReview() {
    if (mode !== "edit" || !reviewId) return;
    const confirmed = window.confirm("Bạn có chắc muốn xóa review này?");
    if (!confirmed) return;
    setSaving(true);
    setToast(null);
    try {
      const response = await fetch(`/api/admin/reviews/${reviewId}`, { method: "DELETE" });
      const result = (await response.json()) as ApiResponse<{ id: string }>;
      if (!response.ok || !result.success) {
        throw new Error(result.error ?? "Không xóa được review.");
      }
      setToast({ type: "success", message: "Đã xóa review." });
      router.push("/admin/reviews");
    } catch (error) {
      setToast({ type: "error", message: error instanceof Error ? error.message : "Không xóa được review." });
    } finally {
      setSaving(false);
    }
  }

  function navigateList() {
    if (isDirty) {
      const confirmed = window.confirm("Bạn có thay đổi chưa lưu. Vẫn rời trang?");
      if (!confirmed) return;
    }
    router.push("/admin/reviews");
  }

  const currentUpdatedAt = mode === "edit" ? new Date().toLocaleDateString("vi-VN") : "—";

  if (loading) {
    return (
      <div className="mt-4 grid gap-2">
        <LoadingSkeleton className="h-12 w-full" />
        <LoadingSkeleton className="h-12 w-full" />
        <LoadingSkeleton className="h-12 w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={navigateList}
          className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700"
        >
          Về danh sách
        </button>
        {mode === "edit" ? (
          <Link
            href={`/review/${form.slug || reviewId}`}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700"
          >
            Preview
          </Link>
        ) : null}
      </div>

      {toast ? (
        <div
          className={`rounded-xl border px-3 py-2 text-sm ${
            toast.type === "success"
              ? "border-emerald-200 bg-emerald-50 text-emerald-700"
              : "border-red-200 bg-red-50 text-red-700"
          }`}
        >
          {toast.message}
        </div>
      ) : null}
      {apiError ? <ErrorState title="Lỗi dữ liệu review" message={apiError} /> : null}

      <div className="grid gap-3 lg:grid-cols-2">
        <label className="space-y-1 text-sm">
          <span className="font-semibold text-slate-800">Title *</span>
          <input
            value={form.title}
            onChange={(event) => setForm((prev) => ({ ...prev, title: event.target.value }))}
            className={fieldClass("title")}
          />
          {errors.title ? <span className="text-xs text-red-600">{errors.title}</span> : null}
        </label>
        <label className="space-y-1 text-sm">
          <span className="font-semibold text-slate-800">Slug *</span>
          <input
            value={form.slug}
            onChange={(event) => {
              setSlugTouched(true);
              setForm((prev) => ({ ...prev, slug: event.target.value }));
            }}
            className={fieldClass("slug")}
          />
          {errors.slug ? <span className="text-xs text-red-600">{errors.slug}</span> : null}
        </label>
        <label className="space-y-1 text-sm">
          <span className="font-semibold text-slate-800">Linked product *</span>
          <select
            value={form.productId}
            onChange={(event) => setForm((prev) => ({ ...prev, productId: event.target.value }))}
            className={fieldClass("productId")}
          >
            <option value="">Chọn product</option>
            {products.map((product) => (
              <option key={product.id} value={product.id}>
                {product.name}
              </option>
            ))}
          </select>
          {errors.productId ? <span className="text-xs text-red-600">{errors.productId}</span> : null}
        </label>
        <label className="space-y-1 text-sm">
          <span className="font-semibold text-slate-800">Category</span>
          <select
            value={form.categoryId}
            onChange={(event) => setForm((prev) => ({ ...prev, categoryId: event.target.value }))}
            className={fieldClass("categoryId")}
          >
            <option value="">Chọn category</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>
        <label className="space-y-1 text-sm">
          <span className="font-semibold text-slate-800">Author *</span>
          <input
            value={form.author}
            onChange={(event) => setForm((prev) => ({ ...prev, author: event.target.value }))}
            className={fieldClass("author")}
          />
          {errors.author ? <span className="text-xs text-red-600">{errors.author}</span> : null}
        </label>
        <label className="space-y-1 text-sm">
          <span className="font-semibold text-slate-800">Status</span>
          <select
            value={form.status}
            onChange={(event) => setForm((prev) => ({ ...prev, status: event.target.value as ReviewStatus }))}
            className={fieldClass("status")}
          >
            <option value="DRAFT">Draft</option>
            <option value="PUBLISHED">Published</option>
            <option value="ARCHIVED">Archived</option>
          </select>
        </label>
        <label className="space-y-1 text-sm">
          <span className="font-semibold text-slate-800">Published at</span>
          <input
            type="date"
            value={form.publishedAt}
            onChange={(event) => setForm((prev) => ({ ...prev, publishedAt: event.target.value }))}
            className={fieldClass("publishedAt")}
          />
        </label>
        <label className="space-y-1 text-sm">
          <span className="font-semibold text-slate-800">Updated at</span>
          <input value={currentUpdatedAt} readOnly className="h-11 w-full rounded-xl border border-slate-200 bg-slate-100 px-3 text-sm" />
        </label>
        <label className="space-y-1 text-sm">
          <span className="font-semibold text-slate-800">Reading time (minutes)</span>
          <input
            value={form.readingTimeMinutes}
            onChange={(event) => setForm((prev) => ({ ...prev, readingTimeMinutes: event.target.value }))}
            className={fieldClass("readingTimeMinutes")}
          />
        </label>
        <label className="space-y-1 text-sm">
          <span className="font-semibold text-slate-800">Score (0-10)</span>
          <input
            value={form.score}
            onChange={(event) => setForm((prev) => ({ ...prev, score: event.target.value }))}
            className={fieldClass("score")}
          />
          {errors.score ? <span className="text-xs text-red-600">{errors.score}</span> : null}
        </label>
        <label className="space-y-1 text-sm lg:col-span-2">
          <span className="font-semibold text-slate-800">Summary</span>
          <textarea
            value={form.summary}
            onChange={(event) => setForm((prev) => ({ ...prev, summary: event.target.value }))}
            rows={3}
            className={`w-full rounded-xl border px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 ${errors.summary ? "border-red-300 bg-red-50" : "border-slate-200"}`}
          />
          {errors.summary ? <span className="text-xs text-red-600">{errors.summary}</span> : null}
        </label>
      </div>

      <div className="grid gap-3 lg:grid-cols-2">
        <label className="space-y-1 text-sm">
          <span className="font-semibold text-slate-800">Cover image URL</span>
          <input
            value={form.coverImage}
            onChange={(event) => setForm((prev) => ({ ...prev, coverImage: event.target.value }))}
            className={fieldClass("coverImage")}
          />
          {errors.coverImage ? <span className="text-xs text-red-600">{errors.coverImage}</span> : null}
        </label>
        <label className="space-y-1 text-sm">
          <span className="font-semibold text-slate-800">Verdict</span>
          <input
            value={form.verdict}
            onChange={(event) => setForm((prev) => ({ ...prev, verdict: event.target.value }))}
            className={fieldClass("verdict")}
          />
        </label>
      </div>

      <label className="space-y-1 text-sm">
        <span className="font-semibold text-slate-800">Pros</span>
        <textarea
          value={form.pros}
          onChange={(event) => setForm((prev) => ({ ...prev, pros: event.target.value }))}
          rows={3}
          className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
        />
      </label>
      <label className="space-y-1 text-sm">
        <span className="font-semibold text-slate-800">Cons</span>
        <textarea
          value={form.cons}
          onChange={(event) => setForm((prev) => ({ ...prev, cons: event.target.value }))}
          rows={3}
          className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
        />
      </label>

      <RichTextEditor
        label="Nội dung bài review"
        value={form.content}
        onChange={(next) => setForm((prev) => ({ ...prev, content: next }))}
        placeholder="Nhập bài review chi tiết..."
      />
      {errors.content ? <p className="text-xs text-red-600">{errors.content}</p> : null}

      <div className="rounded-2xl border border-slate-200 p-3">
        <div className="mb-2 flex items-center justify-between">
          <p className="text-sm font-semibold text-slate-800">Review blocks</p>
          <button
            type="button"
            onClick={() =>
              setForm((prev) => ({
                ...prev,
                blocks: [...prev.blocks, { type: "paragraph", content: "" }],
              }))
            }
            className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700"
          >
            + Add block
          </button>
        </div>
        <div className="space-y-2">
          {form.blocks.map((block, index) => (
            <div key={`block-${index}`} className="rounded-xl border border-slate-200 p-2">
              <div className="grid gap-2 sm:grid-cols-[180px_minmax(0,1fr)_80px]">
                <select
                  value={block.type}
                  onChange={(event) =>
                    setForm((prev) => {
                      const next = [...prev.blocks];
                      next[index] = { ...next[index], type: event.target.value as ReviewBlockType };
                      return { ...prev, blocks: next };
                    })
                  }
                  className="h-10 rounded-lg border border-slate-200 px-3 text-sm"
                >
                  <option value="heading">Heading</option>
                  <option value="paragraph">Paragraph</option>
                  <option value="image">Image</option>
                  <option value="spec_table">Table thông số</option>
                  <option value="pros_cons">Pros/cons</option>
                  <option value="score_breakdown">Score breakdown</option>
                  <option value="cta_shopee">CTA Shopee</option>
                  <option value="faq">FAQ</option>
                </select>
                <input
                  value={block.content}
                  onChange={(event) =>
                    setForm((prev) => {
                      const next = [...prev.blocks];
                      next[index] = { ...next[index], content: event.target.value };
                      return { ...prev, blocks: next };
                    })
                  }
                  className="h-10 rounded-lg border border-slate-200 px-3 text-sm"
                  placeholder="Block content"
                />
                <button
                  type="button"
                  onClick={() =>
                    setForm((prev) => ({
                      ...prev,
                      blocks: prev.blocks.filter((_, idx) => idx !== index),
                    }))
                  }
                  className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs text-red-700"
                >
                  Xóa
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 p-3">
        <p className="mb-2 text-sm font-semibold text-slate-800">Score breakdown</p>
        <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
          {[
            { key: "design", label: "Design" },
            { key: "performance", label: "Performance" },
            { key: "features", label: "Features" },
            { key: "priceValue", label: "Price/value" },
            { key: "durability", label: "Durability" },
            { key: "userExperience", label: "User experience" },
            { key: "overall", label: "Overall" },
          ].map((item) => (
            <label key={item.key} className="space-y-1 text-xs">
              <span className="font-semibold text-slate-700">{item.label}</span>
              <input
                value={form.scoreBreakdown[item.key as keyof ReviewFormState["scoreBreakdown"]]}
                onChange={(event) =>
                  setForm((prev) => ({
                    ...prev,
                    scoreBreakdown: {
                      ...prev.scoreBreakdown,
                      [item.key]: event.target.value,
                    },
                  }))
                }
                className="h-10 w-full rounded-lg border border-slate-200 px-3 text-sm"
              />
            </label>
          ))}
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 p-3">
        <div className="mb-2 flex items-center justify-between">
          <p className="text-sm font-semibold text-slate-800">FAQ</p>
          <button
            type="button"
            onClick={() =>
              setForm((prev) => ({
                ...prev,
                faqItems: [...prev.faqItems, { question: "", answer: "" }],
              }))
            }
            className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700"
          >
            + Add FAQ
          </button>
        </div>
        <div className="space-y-2">
          {form.faqItems.map((faq, index) => (
            <div key={`faq-${index}`} className="rounded-xl border border-slate-200 p-2">
              <input
                value={faq.question}
                onChange={(event) =>
                  setForm((prev) => {
                    const next = [...prev.faqItems];
                    next[index] = { ...next[index], question: event.target.value };
                    return { ...prev, faqItems: next };
                  })
                }
                placeholder="Question"
                className="mb-2 h-10 w-full rounded-lg border border-slate-200 px-3 text-sm"
              />
              <textarea
                value={faq.answer}
                onChange={(event) =>
                  setForm((prev) => {
                    const next = [...prev.faqItems];
                    next[index] = { ...next[index], answer: event.target.value };
                    return { ...prev, faqItems: next };
                  })
                }
                placeholder="Answer"
                rows={3}
                className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
              />
              <button
                type="button"
                onClick={() =>
                  setForm((prev) => ({
                    ...prev,
                    faqItems: prev.faqItems.filter((_, idx) => idx !== index),
                  }))
                }
                className="mt-2 rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs text-red-700"
              >
                Remove
              </button>
            </div>
          ))}
        </div>
      </div>

      <div className="grid gap-3 lg:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 p-3">
          <p className="mb-2 text-sm font-semibold text-slate-800">Related reviews</p>
          <div className="max-h-44 space-y-1 overflow-auto">
            {reviews
              .filter((review) => (mode === "edit" ? review.id !== reviewId : true))
              .map((review) => (
                <label key={review.id} className="flex items-center gap-2 text-sm text-slate-700">
                  <input
                    type="checkbox"
                    checked={form.relatedReviewIds.includes(review.id)}
                    onChange={(event) =>
                      setForm((prev) => ({
                        ...prev,
                        relatedReviewIds: event.target.checked
                          ? [...prev.relatedReviewIds, review.id]
                          : prev.relatedReviewIds.filter((id) => id !== review.id),
                      }))
                    }
                  />
                  <span>{review.title}</span>
                </label>
              ))}
          </div>
        </div>
        <div className="rounded-2xl border border-slate-200 p-3">
          <p className="mb-2 text-sm font-semibold text-slate-800">Related products</p>
          <div className="max-h-44 space-y-1 overflow-auto">
            {products.map((product) => (
              <label key={product.id} className="flex items-center gap-2 text-sm text-slate-700">
                <input
                  type="checkbox"
                  checked={form.relatedProductIds.includes(product.id)}
                  onChange={(event) =>
                    setForm((prev) => ({
                      ...prev,
                      relatedProductIds: event.target.checked
                        ? [...prev.relatedProductIds, product.id]
                        : prev.relatedProductIds.filter((id) => id !== product.id),
                    }))
                  }
                />
                <span>{product.name}</span>
              </label>
            ))}
          </div>
        </div>
      </div>

      <label className="space-y-1 text-sm">
        <span className="font-semibold text-slate-800">CTA blocks</span>
        <textarea
          value={form.ctaBlocks}
          onChange={(event) => setForm((prev) => ({ ...prev, ctaBlocks: event.target.value }))}
          rows={3}
          className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm"
          placeholder='{"platform":"Shopee","label":"Xem giá tốt nhất"}'
        />
      </label>

      <div className="rounded-2xl border border-slate-200 p-3">
        <p className="mb-2 text-sm font-semibold text-slate-800">SEO</p>
        <div className="grid gap-3 lg:grid-cols-2">
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Meta title</span>
            <input
              value={form.seoTitle}
              onChange={(event) => setForm((prev) => ({ ...prev, seoTitle: event.target.value }))}
              className={fieldClass("seoTitle")}
            />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Meta description</span>
            <input
              value={form.seoDescription}
              onChange={(event) => setForm((prev) => ({ ...prev, seoDescription: event.target.value }))}
              className={fieldClass("seoDescription")}
            />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">OG image</span>
            <input
              value={form.seoOgImage}
              onChange={(event) => setForm((prev) => ({ ...prev, seoOgImage: event.target.value }))}
              className={fieldClass("seoOgImage")}
            />
            {errors.seoOgImage ? <span className="text-xs text-red-600">{errors.seoOgImage}</span> : null}
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Canonical URL</span>
            <input
              value={form.canonicalUrl}
              onChange={(event) => setForm((prev) => ({ ...prev, canonicalUrl: event.target.value }))}
              className={fieldClass("canonicalUrl")}
            />
            {errors.canonicalUrl ? <span className="text-xs text-red-600">{errors.canonicalUrl}</span> : null}
          </label>
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input
              type="checkbox"
              checked={form.noindex}
              onChange={(event) => setForm((prev) => ({ ...prev, noindex: event.target.checked }))}
            />
            <span>Noindex</span>
          </label>
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => void submit("DRAFT")}
          disabled={saving}
          className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-2 text-sm font-semibold text-amber-700 disabled:opacity-60"
        >
          {saving ? "Đang lưu..." : "Lưu draft"}
        </button>
        <button
          type="button"
          onClick={() => void submit("PUBLISHED")}
          disabled={saving}
          className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-700 disabled:opacity-60"
        >
          {saving ? "Đang publish..." : "Publish"}
        </button>
        {mode === "edit" ? (
          <>
            <button
              type="button"
              onClick={() => void archiveReview()}
              disabled={saving}
              className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 disabled:opacity-60"
            >
              Archive
            </button>
            <button
              type="button"
              onClick={() => void deleteReview()}
              disabled={saving}
              className="rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-sm font-semibold text-red-700 disabled:opacity-60"
            >
              Delete
            </button>
          </>
        ) : null}
      </div>
    </div>
  );
}
