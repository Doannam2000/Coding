"use client";

export const dynamic = "force-dynamic";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer, ShopeeCTAButton } from "@/components/ui";

type DealStatus = "Active" | "Expired" | "Draft";

type Deal = {
  id: string;
  productId: string;
  product: { name: string };
  affiliateLinkId: string;
  affiliateLink: { internalUrl: string };
  currentPrice: string;
  oldPrice: string;
  discount: string;
  couponCode: string | null;
  startTime: string;
  endTime: string;
  status: DealStatus;
};

type ProductOption = { id: string; name: string };
type LinkOption = { id: string; label: string; internalUrl: string; productId: string };

type FormState = {
  id?: string;
  productId: string;
  affiliateLinkId: string;
  currentPrice: string;
  oldPrice: string;
  discount: string;
  couponCode: string;
  startTime: string;
  endTime: string;
  status: DealStatus;
};

const initialForm: FormState = {
  productId: "",
  affiliateLinkId: "",
  currentPrice: "",
  oldPrice: "",
  discount: "",
  couponCode: "",
  startTime: "",
  endTime: "",
  status: "Draft",
};

function statusClass(status: DealStatus) {
  if (status === "Active") return "border-emerald-200 bg-emerald-50 text-emerald-700";
  if (status === "Expired") return "border-red-200 bg-red-50 text-red-700";
  return "border-amber-200 bg-amber-50 text-amber-700";
}

function autoCalcDiscount(currentPrice: string, oldPrice: string): string {
  const current = Number(currentPrice.replace(/[^0-9]/g, ""));
  const old = Number(oldPrice.replace(/[^0-9]/g, ""));
  if (!current || !old || old <= current) return "";
  const discountPercent = Math.round(((old - current) / old) * 100);
  return `-${discountPercent}%`;
}

export default function AdminDealsPage() {
  const [deals, setDeals] = useState<Deal[]>([]);
  const [products, setProducts] = useState<ProductOption[]>([]);
  const [links, setLinks] = useState<LinkOption[]>([]);
  const [form, setForm] = useState<FormState>(initialForm);
  const [message, setMessage] = useState("");
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | DealStatus>("ALL");

  function fieldClass(key: keyof FormState) {
    return `h-11 w-full rounded-xl border px-3 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 ${errors[key] ? "border-red-300 bg-red-50" : "border-slate-200"}`;
  }

  const filteredLinks = useMemo(
    () => (form.productId ? links.filter((l) => l.productId === form.productId) : links),
    [form.productId, links],
  );

  const loadDeals = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      const params = new URLSearchParams({ limit: "100" });
      if (query.trim()) params.set("q", query.trim());
      if (statusFilter !== "ALL") params.set("status", statusFilter);
      const response = await fetch(`/api/admin/deals?${params.toString()}`);
      const payload = (await response.json()) as { success: boolean; data?: { items: Deal[] }; error?: string };
      if (!response.ok || !payload.success || !payload.data) throw new Error(payload.error ?? "Không tải được deals");
      setDeals(payload.data.items);
    } catch (e) {
      setDeals([]);
      setLoadError(e instanceof Error ? e.message : "Không tải được deals");
    } finally {
      setLoading(false);
    }
  }, [query, statusFilter]);

  async function loadDependencies() {
    const [pRes, lRes] = await Promise.all([
      fetch("/api/admin/products?limit=200"),
      fetch("/api/admin/affiliate-links?limit=200"),
    ]);
    const pPayload = (await pRes.json()) as { success: boolean; data?: { items: Array<{ id: string; name: string }> } };
    const lPayload = (await lRes.json()) as {
      success: boolean;
      data?: { items: Array<{ id: string; label: string; internalUrl: string; productId: string }> };
    };
    if (pRes.ok && pPayload.success && pPayload.data) setProducts(pPayload.data.items);
    if (lRes.ok && lPayload.success && lPayload.data) setLinks(lPayload.data.items);
  }

  useEffect(() => {
    void (async () => {
      await Promise.all([loadDeals(), loadDependencies()]);
    })();
  }, [query, statusFilter, loadDeals]);

  function resetForm() {
    setForm(initialForm);
    setErrors({});
  }

  async function saveDeal() {
    const nextErrors: Partial<Record<keyof FormState, string>> = {};
    if (!form.productId.trim()) nextErrors.productId = "Product là bắt buộc.";
    if (!form.affiliateLinkId.trim()) nextErrors.affiliateLinkId = "Affiliate link là bắt buộc.";
    if (!form.currentPrice.trim()) nextErrors.currentPrice = "Current price là bắt buộc.";
    if (!form.oldPrice.trim()) nextErrors.oldPrice = "Old price là bắt buộc.";
    if (!form.discount.trim()) nextErrors.discount = "Discount là bắt buộc.";
    if (!form.startTime.trim()) nextErrors.startTime = "Start time là bắt buộc.";
    if (!form.endTime.trim()) nextErrors.endTime = "End time là bắt buộc.";

    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      setMessage("Vui lòng điền đầy đủ thông tin deal.");
      return;
    }

    setErrors({});
    setSaving(true);
    setMessage("");

    try {
      const payload = {
        productId: form.productId,
        affiliateLinkId: form.affiliateLinkId,
        currentPrice: form.currentPrice,
        oldPrice: form.oldPrice,
        discount: form.discount,
        couponCode: form.couponCode.trim() || null,
        startTime: form.startTime,
        endTime: form.endTime,
        status: form.status,
      };

      const response = form.id
        ? await fetch(`/api/admin/deals/${form.id}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
          })
        : await fetch("/api/admin/deals", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
          });

      const result = (await response.json()) as { success: boolean; error?: string };
      if (!response.ok || !result.success) throw new Error(result.error ?? "Không lưu được deal.");

      setMessage(form.id ? "Đã cập nhật deal." : "Đã tạo deal mới.");
      resetForm();
      await loadDeals();
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "Không lưu được deal.");
    } finally {
      setSaving(false);
    }
  }

  function editDeal(item: Deal) {
    setForm({
      id: item.id,
      productId: item.productId,
      affiliateLinkId: item.affiliateLinkId,
      currentPrice: item.currentPrice,
      oldPrice: item.oldPrice,
      discount: item.discount,
      couponCode: item.couponCode ?? "",
      startTime: item.startTime.slice(0, 16),
      endTime: item.endTime.slice(0, 16),
      status: item.status,
    });
    setMessage("Đang chỉnh sửa deal.");
  }

  async function markExpired(id: string) {
    const ok = window.confirm("Bạn có chắc muốn đánh dấu deal này là hết hạn?");
    if (!ok) return;
    const response = await fetch(`/api/admin/deals/${id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "Expired" }),
    });
    const result = (await response.json()) as { success: boolean; error?: string };
    if (!response.ok || !result.success) {
      setMessage(result.error ?? "Không cập nhật được trạng thái deal.");
      return;
    }
    setMessage("Đã đánh dấu deal hết hạn.");
    await loadDeals();
  }

  async function deleteDeal(id: string) {
    const ok = window.confirm("Bạn có chắc muốn xóa deal này?");
    if (!ok) return;
    try {
      const response = await fetch(`/api/admin/deals/${id}`, { method: "DELETE" });
      const result = (await response.json()) as { success: boolean; error?: string };
      if (!response.ok || !result.success) throw new Error(result.error ?? "Không xóa được deal.");
      setMessage("Đã xóa deal.");
      await loadDeals();
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "Không xóa được deal.");
    }
  }

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Admin Deals</h1>
            <p className="mt-1 text-sm text-slate-600">Quản lý deal Shopee, giá và lịch chạy deal.</p>
          </div>
          <Link href="/admin" className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">Về dashboard</Link>
        </div>

        {message ? <div className="mt-4 rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-blue-700">{message}</div> : null}
        {loadError ? <div className="mt-4"><ErrorState title="Không tải được deal list" message={loadError} /></div> : null}

        <div className="mt-4 grid gap-2 lg:grid-cols-4">
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Tìm deal theo sản phẩm, giá, discount..." className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 lg:col-span-3" />
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as "ALL" | DealStatus)} className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100">
            <option value="ALL">Tất cả trạng thái</option>
            <option value="Active">Active</option>
            <option value="Expired">Expired</option>
            <option value="Draft">Draft</option>
          </select>
        </div>

        <div className="mt-4 grid gap-3 lg:grid-cols-2">
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Attach product</span><select value={form.productId} onChange={(e) => { setForm((p) => ({ ...p, productId: e.target.value, affiliateLinkId: "" })); setErrors((prev) => ({ ...prev, productId: undefined })); }} className={fieldClass("productId")}><option value="">Chọn sản phẩm</option>{products.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</select>{errors.productId ? <span className="text-xs text-red-600">{errors.productId}</span> : null}</label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Attach affiliate link</span><select value={form.affiliateLinkId} onChange={(e) => setForm((p) => ({ ...p, affiliateLinkId: e.target.value }))} className={fieldClass("affiliateLinkId")}><option value="">Chọn affiliate link</option>{filteredLinks.map((l) => <option key={l.id} value={l.id}>{l.label}</option>)}</select>{errors.affiliateLinkId ? <span className="text-xs text-red-600">{errors.affiliateLinkId}</span> : null}</label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Current price</span><input value={form.currentPrice} onChange={(e) => { const v = e.target.value; setForm((p) => ({ ...p, currentPrice: v, discount: autoCalcDiscount(v, p.oldPrice) })); }} className={fieldClass("currentPrice")} /></label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Old price</span><input value={form.oldPrice} onChange={(e) => { const v = e.target.value; setForm((p) => ({ ...p, oldPrice: v, discount: autoCalcDiscount(p.currentPrice, v) })); }} className={fieldClass("oldPrice")} /></label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Discount</span><input value={form.discount} onChange={(e) => setForm((p) => ({ ...p, discount: e.target.value }))} placeholder="Tự động tính khi nhập giá" className={fieldClass("discount")} /></label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Coupon code</span><input value={form.couponCode} onChange={(e) => setForm((p) => ({ ...p, couponCode: e.target.value }))} placeholder="Mã giảm giá (nếu có)" className={fieldClass("couponCode")} /></label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Status</span><select value={form.status} onChange={(e) => setForm((p) => ({ ...p, status: e.target.value as DealStatus }))} className="h-11 w-full rounded-xl border border-slate-200 px-3 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"><option value="Active">Active</option><option value="Expired">Expired</option><option value="Draft">Draft</option></select></label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Start time</span><input type="datetime-local" value={form.startTime} onChange={(e) => setForm((p) => ({ ...p, startTime: e.target.value }))} className={fieldClass("startTime")} /></label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">End time</span><input type="datetime-local" value={form.endTime} onChange={(e) => setForm((p) => ({ ...p, endTime: e.target.value }))} className={fieldClass("endTime")} /></label>
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          <button type="button" onClick={saveDeal} disabled={saving} className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-100 disabled:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">{saving ? "Đang lưu..." : form.id ? "Update" : "Create"}</button>
          <button type="button" onClick={resetForm} className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">Reset</button>
        </div>

        {loading ? (
          <div className="mt-4 grid gap-2">
            <LoadingSkeleton className="h-12 w-full" />
            <LoadingSkeleton className="h-12 w-full" />
          </div>
        ) : deals.length === 0 ? (
          <div className="mt-4"><EmptyState title="Chưa có deal" message="Tạo deal mới để bắt đầu." /></div>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full min-w-[980px] text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-600">
                  <th className="px-2 py-2 font-semibold">Product</th>
                  <th className="px-2 py-2 font-semibold">Current</th>
                  <th className="px-2 py-2 font-semibold">Old</th>
                  <th className="px-2 py-2 font-semibold">Discount</th>
                  <th className="px-2 py-2 font-semibold">Coupon</th>
                  <th className="px-2 py-2 font-semibold">Start</th>
                  <th className="px-2 py-2 font-semibold">End</th>
                  <th className="px-2 py-2 font-semibold">Status</th>
                  <th className="px-2 py-2 font-semibold">Affiliate</th>
                  <th className="px-2 py-2 font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {deals.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100">
                    <td className="px-2 py-2 text-slate-800">{item.product?.name ?? "-"}</td>
                    <td className="px-2 py-2 text-slate-700">{item.currentPrice}</td>
                    <td className="px-2 py-2 text-slate-700">{item.oldPrice}</td>
                    <td className="px-2 py-2 text-slate-700">{item.discount}</td>
                    <td className="px-2 py-2 text-slate-700">{item.couponCode || "-"}</td>
                    <td className="px-2 py-2 text-slate-700">{new Date(item.startTime).toLocaleString("vi-VN")}</td>
                    <td className="px-2 py-2 text-slate-700">{new Date(item.endTime).toLocaleString("vi-VN")}</td>
                    <td className="px-2 py-2"><span className={`rounded-full border px-2 py-0.5 text-xs font-semibold ${statusClass(item.status)}`}>{item.status}</span></td>
                    <td className="px-2 py-2">{item.affiliateLink ? <span className="text-xs text-emerald-600">OK</span> : <span className="rounded-full border border-amber-200 bg-amber-50 px-1.5 py-0.5 text-xs font-semibold text-amber-700">Missing</span>}</td>
                    <td className="px-2 py-2">
                      <div className="flex flex-wrap gap-1">
                        <button type="button" onClick={() => editDeal(item)} className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">Edit</button>
                        <button type="button" onClick={() => markExpired(item.id)} className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs font-semibold text-red-700 transition hover:bg-red-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500">Mark expired</button>
                        <button type="button" onClick={() => deleteDeal(item.id)} className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs font-semibold text-red-700 transition hover:bg-red-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500">Delete</button>
                        <ShopeeCTAButton href={item.affiliateLink?.internalUrl || "#"}>Preview deal card</ShopeeCTAButton>
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
