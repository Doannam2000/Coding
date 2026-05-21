"use client";

export const dynamic = "force-dynamic";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer } from "@/components/ui";

type Platform = "Shopee" | "Lazada" | "Tiki";
type AffiliateLinkStatus = "ACTIVE" | "INACTIVE" | "BROKEN";

type AffiliateLink = {
  id: string;
  label: string;
  productId: string;
  reviewId: string | null;
  platform: Platform;
  originalUrl: string;
  affiliateUrl: string;
  internalUrl: string;
  clickCount: number;
  status: AffiliateLinkStatus;
};

type FormState = {
  id?: string;
  label: string;
  productId: string;
  reviewId: string;
  platform: Platform;
  originalUrl: string;
  affiliateUrl: string;
  status: AffiliateLinkStatus;
};

const initialForm: FormState = {
  label: "",
  productId: "",
  reviewId: "",
  platform: "Shopee",
  originalUrl: "",
  affiliateUrl: "",
  status: "ACTIVE",
};

function statusClass(status: AffiliateLinkStatus) {
  if (status === "ACTIVE") return "border-emerald-200 bg-emerald-50 text-emerald-700";
  if (status === "BROKEN") return "border-red-200 bg-red-50 text-red-700";
  return "border-slate-300 bg-slate-100 text-slate-700";
}

export default function AdminAffiliateLinksPage() {
  const [links, setLinks] = useState<AffiliateLink[]>([]);
  const [query, setQuery] = useState("");
  const [platform, setPlatform] = useState<"ALL" | Platform>("ALL");
  const [active, setActive] = useState<"ALL" | "ACTIVE" | "INACTIVE">("ALL");
  const [form, setForm] = useState<FormState>(initialForm);
  const [message, setMessage] = useState("");
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [saving, setSaving] = useState(false);

  function fieldClass(key: keyof FormState) {
    return `h-11 w-full rounded-xl border px-3 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 ${errors[key] ? "border-red-300 bg-red-50" : "border-slate-200"}`;
  }

  const loadLinks = useCallback(async () => {
    setLoading(true);
    setLoadError(false);
    try {
      const params = new URLSearchParams();
      if (query.trim()) params.set("q", query.trim());
      if (platform !== "ALL") params.set("platform", platform);
      if (active !== "ALL") params.set("status", active);
      params.set("limit", "100");
      const response = await fetch(`/api/admin/affiliate-links?${params.toString()}`);
      const payload = (await response.json()) as { success: boolean; data?: { items: AffiliateLink[] }; error?: string };
      if (!response.ok || !payload.success || !payload.data) throw new Error(payload.error ?? "Không tải được dữ liệu");
      setLinks(payload.data.items);
    } catch {
      setLoadError(true);
      setLinks([]);
    } finally {
      setLoading(false);
    }
  }, [active, platform, query]);

  useEffect(() => {
    void (async () => {
      await loadLinks();
    })();
  }, [loadLinks]);

  const filtered = useMemo(() => links, [links]);

  function resetForm() {
    setForm(initialForm);
    setErrors({});
  }

  async function saveLink() {
    const nextErrors: Partial<Record<keyof FormState, string>> = {};
    if (!form.label.trim()) nextErrors.label = "Label là bắt buộc.";
    if (!form.productId.trim()) nextErrors.productId = "Product là bắt buộc.";
    if (!form.originalUrl.trim()) nextErrors.originalUrl = "Original URL là bắt buộc.";
    if (!form.affiliateUrl.trim()) nextErrors.affiliateUrl = "Affiliate URL là bắt buộc.";

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      setMessage("Vui lòng điền đủ thông tin link affiliate.");
      return;
    }

    setErrors({});
    setMessage("");
    setSaving(true);

    try {
      const payload = {
        label: form.label,
        productId: form.productId,
        reviewId: form.reviewId,
        platform: form.platform,
        originalUrl: form.originalUrl,
        affiliateUrl: form.affiliateUrl,
        status: form.status,
      };

      const response = form.id
        ? await fetch(`/api/admin/affiliate-links/${form.id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) })
        : await fetch("/api/admin/affiliate-links", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });

      const result = (await response.json()) as { success: boolean; error?: string };
      if (!response.ok || !result.success) throw new Error(result.error ?? "Không lưu được affiliate link.");

      setMessage(form.id ? "Đã cập nhật affiliate link." : "Đã tạo affiliate link mới.");
      resetForm();
      await loadLinks();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Không lưu được affiliate link.");
    } finally {
      setSaving(false);
    }
  }

  function editLink(item: AffiliateLink) {
    setForm({
      id: item.id,
      label: item.label,
      productId: item.productId,
      reviewId: item.reviewId ?? "",
      platform: item.platform,
      originalUrl: item.originalUrl,
      affiliateUrl: item.affiliateUrl,
      status: item.status,
    });
    setMessage("Đang chỉnh sửa affiliate link.");
  }

  async function disableLink(id: string) {
    const ok = window.confirm("Bạn có chắc muốn disable link này?");
    if (!ok) return;
    try {
      const response = await fetch(`/api/admin/affiliate-links/${id}`, { method: "DELETE" });
      const result = (await response.json()) as { success: boolean; error?: string };
      if (!response.ok || !result.success) throw new Error(result.error ?? "Không thể disable link.");
      setMessage("Đã disable affiliate link.");
      await loadLinks();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Không thể disable link.");
    }
  }

  async function copyInternalUrl(value: string) {
    try {
      await navigator.clipboard.writeText(value);
      setMessage("Đã copy internal URL.");
    } catch {
      setMessage("Không thể copy internal URL.");
    }
  }

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Admin Affiliate Links</h1>
            <p className="mt-1 text-sm text-slate-600">Quản lý link affiliate và link tracking nội bộ.</p>
          </div>
          <Link href="/admin" className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">Về dashboard</Link>
        </div>

        {message ? <div className="mt-4 rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-blue-700">{message}</div> : null}
        {loadError ? <div className="mt-4"><ErrorState title="Không tải được affiliate links" message="Vui lòng thử lại sau." /></div> : null}

        <div className="mt-4 grid gap-2 lg:grid-cols-4">
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Tìm theo label, productId, reviewId, internal URL" className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 lg:col-span-2" />
          <select value={platform} onChange={(e) => setPlatform(e.target.value as "ALL" | Platform)} className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100">
            <option value="ALL">Tất cả platform</option>
            <option value="Shopee">Shopee</option>
            <option value="Lazada">Lazada</option>
            <option value="Tiki">Tiki</option>
          </select>
          <select value={active} onChange={(e) => setActive(e.target.value as "ALL" | "ACTIVE" | "INACTIVE")} className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100">
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </div>

        <div className="mt-4 grid gap-3 lg:grid-cols-2">
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Label</span><input value={form.label} onChange={(e) => { setForm((p) => ({ ...p, label: e.target.value })); setErrors((prev) => ({ ...prev, label: undefined })); }} className={fieldClass("label")} />{errors.label ? <span className="text-xs text-red-600">{errors.label}</span> : null}</label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Platform</span><select value={form.platform} onChange={(e) => setForm((p) => ({ ...p, platform: e.target.value as Platform }))} className="h-11 w-full rounded-xl border border-slate-200 px-3 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"><option value="Shopee">Shopee</option><option value="Lazada">Lazada</option><option value="Tiki">Tiki</option></select></label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Attach product</span><input value={form.productId} onChange={(e) => setForm((p) => ({ ...p, productId: e.target.value }))} placeholder="prd-..." className={fieldClass("productId")} /></label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Attach review</span><input value={form.reviewId} onChange={(e) => setForm((p) => ({ ...p, reviewId: e.target.value }))} placeholder="rvw-... (optional)" className="h-11 w-full rounded-xl border border-slate-200 px-3 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100" /></label>
          <label className="space-y-1 text-sm lg:col-span-2"><span className="font-semibold text-slate-800">Original URL</span><input value={form.originalUrl} onChange={(e) => setForm((p) => ({ ...p, originalUrl: e.target.value }))} placeholder="https://..." className={fieldClass("originalUrl")} /></label>
          <label className="space-y-1 text-sm lg:col-span-2"><span className="font-semibold text-slate-800">Affiliate URL</span><input value={form.affiliateUrl} onChange={(e) => setForm((p) => ({ ...p, affiliateUrl: e.target.value }))} placeholder="https://..." className={fieldClass("affiliateUrl")} /></label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Status</span><select value={form.status} onChange={(e) => setForm((p) => ({ ...p, status: e.target.value as AffiliateLinkStatus }))} className="h-11 w-full rounded-xl border border-slate-200 px-3 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"><option value="ACTIVE">ACTIVE</option><option value="INACTIVE">INACTIVE</option><option value="BROKEN">BROKEN</option></select></label>
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          <button type="button" disabled={saving} onClick={saveLink} className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-700 disabled:opacity-60">{saving ? "Đang lưu..." : form.id ? "Update" : "Create"}</button>
          <button type="button" onClick={resetForm} className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700">Reset</button>
        </div>

        {loading ? (
          <div className="mt-4 grid gap-2">
            <LoadingSkeleton className="h-12 w-full" />
            <LoadingSkeleton className="h-12 w-full" />
            <LoadingSkeleton className="h-12 w-full" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="mt-4"><EmptyState title="Chưa có affiliate link" message="Tạo link mới để bắt đầu." /></div>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full min-w-[1200px] text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-600">
                  <th className="px-2 py-2 font-semibold">Label</th>
                  <th className="px-2 py-2 font-semibold">Platform</th>
                  <th className="px-2 py-2 font-semibold">Product</th>
                  <th className="px-2 py-2 font-semibold">Review</th>
                  <th className="px-2 py-2 font-semibold">Original URL</th>
                  <th className="px-2 py-2 font-semibold">Affiliate URL</th>
                  <th className="px-2 py-2 font-semibold">Internal URL</th>
                  <th className="px-2 py-2 font-semibold">Clicks</th>
                  <th className="px-2 py-2 font-semibold">Status</th>
                  <th className="px-2 py-2 font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100 align-top">
                    <td className="px-2 py-2 text-slate-800">{item.label}</td>
                    <td className="px-2 py-2 text-slate-700">{item.platform}</td>
                    <td className="px-2 py-2 text-slate-700">{item.productId}</td>
                    <td className="px-2 py-2 text-slate-700">{item.reviewId || "-"}</td>
                    <td className="px-2 py-2 text-slate-700"><span className="block max-w-[180px] truncate">{item.originalUrl}</span></td>
                    <td className="px-2 py-2 text-slate-700"><span className="block max-w-[180px] truncate">{item.affiliateUrl}</span></td>
                    <td className="px-2 py-2 text-slate-700"><span className="block max-w-[170px] truncate">{item.internalUrl}</span></td>
                    <td className="px-2 py-2 text-slate-700">{item.clickCount}</td>
                    <td className="px-2 py-2"><span className={`rounded-full border px-2 py-0.5 text-xs font-semibold ${statusClass(item.status)}`}>{item.status}</span></td>
                    <td className="px-2 py-2">
                      <div className="flex flex-wrap gap-1">
                        <button type="button" onClick={() => editLink(item)} className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700">Edit</button>
                        <button type="button" onClick={() => copyInternalUrl(item.internalUrl)} className="rounded-lg border border-blue-200 bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700">Copy internal URL</button>
                        <button type="button" onClick={() => disableLink(item.id)} className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs font-semibold text-red-700">Disable</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </PageContainer>
  );
}
