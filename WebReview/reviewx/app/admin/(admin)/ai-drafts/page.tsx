"use client";

export const dynamic = "force-dynamic";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer } from "@/components/ui";

type DraftStatus = "DRAFT" | "APPROVED" | "REJECTED";

type AIDraft = {
  id: string;
  productId: string;
  productName: string;
  quickVerdict: string;
  seoTitle: string;
  seoDescription: string;
  articleDraft: string;
  status: DraftStatus;
};

type ProductOption = { id: string; name: string };

const initialForm = { id: "", productId: "", quickVerdict: "", seoTitle: "", seoDescription: "", articleDraft: "" };

export default function AdminAIDraftsPage() {
  const [drafts, setDrafts] = useState<AIDraft[]>([]);
  const [products, setProducts] = useState<ProductOption[]>([]);
  const [form, setForm] = useState(initialForm);
  const [selectedId, setSelectedId] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const selectedDraft = useMemo(() => drafts.find((d) => d.id === selectedId) || null, [drafts, selectedId]);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [dRes, pRes] = await Promise.all([fetch("/api/admin/ai-drafts"), fetch("/api/admin/products?limit=200")]);
      const dPayload = (await dRes.json()) as { success: boolean; data?: { items: Array<{ id: string; productId: string; product: { name: string }; summary: string | null; seoTitle: string | null; seoDescription: string | null; content: string | null; status: "DRAFT" | "PUBLISHED" | "ARCHIVED" }> }; error?: string };
      const pPayload = (await pRes.json()) as { success: boolean; data?: { items: Array<{ id: string; name: string }> } };
      if (!dRes.ok || !dPayload.success || !dPayload.data) throw new Error(dPayload.error ?? "Không tải được drafts");
      setDrafts(dPayload.data.items.map((r) => ({ id: r.id, productId: r.productId, productName: r.product?.name ?? "-", quickVerdict: r.summary ?? "", seoTitle: r.seoTitle ?? "", seoDescription: r.seoDescription ?? "", articleDraft: r.content ?? "", status: r.status === "PUBLISHED" ? "APPROVED" : r.status === "ARCHIVED" ? "REJECTED" : "DRAFT" })));
      if (pRes.ok && pPayload.success && pPayload.data) setProducts(pPayload.data.items);
      if (!selectedId && dPayload.data.items[0]) setSelectedId(dPayload.data.items[0].id);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Không tải được AI drafts");
      setDrafts([]);
    } finally {
      setLoading(false);
    }
  }, [selectedId]);

  useEffect(() => {
    void (async () => {
      await loadData();
    })();
  }, [loadData]);

  function resetForm() { setForm(initialForm); }

  function generateDraft() {
    if (!form.productId) { setMessage("Vui lòng chọn sản phẩm trước khi generate draft."); return; }
    const p = products.find((x) => x.id === form.productId)?.name ?? "sản phẩm";
    setForm((prev) => ({ ...prev, quickVerdict: prev.quickVerdict || `Gợi ý mua cho ${p} nếu ưu tiên giá/hiệu năng cân bằng.`, seoTitle: prev.seoTitle || `Review ${p}: Đáng mua không?`, seoDescription: prev.seoDescription || `Đánh giá nhanh ${p} với ưu nhược điểm và gợi ý mua.`, articleDraft: prev.articleDraft || `## Tổng quan\n${p} là lựa chọn phù hợp cho nhu cầu phổ thông...` }));
    setMessage("Đã generate AI draft.");
  }

  async function saveDraft() {
    if (!form.productId || !form.quickVerdict || !form.articleDraft) { setMessage("Thiếu dữ liệu bắt buộc để lưu draft."); return; }
    const payload = { productId: form.productId, quickVerdict: form.quickVerdict, seoTitle: form.seoTitle, seoDescription: form.seoDescription, articleDraft: form.articleDraft };
    const response = form.id ? await fetch(`/api/admin/ai-drafts/${form.id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ action: "update", ...payload }) }) : await fetch("/api/admin/ai-drafts", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });
    const result = (await response.json()) as { success: boolean; error?: string; data?: { id: string } };
    if (!response.ok || !result.success) { setMessage(result.error ?? "Không lưu được draft."); return; }
    setMessage(form.id ? "Đã cập nhật draft." : "Đã tạo draft mới.");
    await loadData();
    if (!form.id && result.data?.id) setSelectedId(result.data.id);
    resetForm();
  }

  function editDraft(item: AIDraft) {
    setForm({ id: item.id, productId: item.productId, quickVerdict: item.quickVerdict, seoTitle: item.seoTitle, seoDescription: item.seoDescription, articleDraft: item.articleDraft });
    setMessage("Đang chỉnh sửa draft.");
  }

  async function approveDraft(id: string) {
    const response = await fetch(`/api/admin/ai-drafts/${id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ action: "approve" }) });
    const result = (await response.json()) as { success: boolean; error?: string };
    if (!response.ok || !result.success) { setMessage(result.error ?? "Không approve được draft."); return; }
    setMessage("Đã approve draft.");
    await loadData();
  }

  async function rejectDraft(id: string) {
    const response = await fetch(`/api/admin/ai-drafts/${id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ action: "reject" }) });
    const result = (await response.json()) as { success: boolean; error?: string };
    if (!response.ok || !result.success) { setMessage(result.error ?? "Không reject được draft."); return; }
    setMessage("Đã reject draft.");
    await loadData();
  }

  function createReviewFromApproved(id: string) {
    const target = drafts.find((d) => d.id === id);
    if (!target || target.status !== "APPROVED") { setMessage("Chỉ tạo review từ draft đã approve."); return; }
    setMessage("Review đã được tạo qua luồng approve (published).");
  }

  return <PageContainer><section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6"><div className="flex flex-wrap items-center justify-between gap-3"><div><h1 className="text-2xl font-bold tracking-tight text-slate-900">Admin AI Drafts</h1><p className="mt-1 text-sm text-slate-600">Tạo bản nháp AI để biên tập thủ công trước khi xuất bản.</p></div><Link href="/admin" className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">Về dashboard</Link></div>{message ? <div className="mt-4 rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-blue-700">{message}</div> : null}{error ? <div className="mt-4"><ErrorState title="Không tải được AI drafts" message={error} /></div> : null}<div className="mt-4 grid gap-3 lg:grid-cols-2"><label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Product selector</span><select value={form.productId} onChange={(e) => setForm((p) => ({ ...p, productId: e.target.value }))} className="h-11 w-full rounded-xl border border-slate-200 px-3"><option value="">Chọn sản phẩm</option>{products.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label><label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Generated SEO title</span><input value={form.seoTitle} onChange={(e) => setForm((p) => ({ ...p, seoTitle: e.target.value }))} className="h-11 w-full rounded-xl border border-slate-200 px-3" /></label><label className="space-y-1 text-sm lg:col-span-2"><span className="font-semibold text-slate-800">Generated quick verdict</span><textarea rows={2} value={form.quickVerdict} onChange={(e) => setForm((p) => ({ ...p, quickVerdict: e.target.value }))} className="w-full rounded-xl border border-slate-200 px-3 py-2" /></label><label className="space-y-1 text-sm lg:col-span-2"><span className="font-semibold text-slate-800">Generated SEO description</span><input value={form.seoDescription} onChange={(e) => setForm((p) => ({ ...p, seoDescription: e.target.value }))} className="h-11 w-full rounded-xl border border-slate-200 px-3" /></label><label className="space-y-1 text-sm lg:col-span-2"><span className="font-semibold text-slate-800">Generated article draft</span><textarea rows={5} value={form.articleDraft} onChange={(e) => setForm((p) => ({ ...p, articleDraft: e.target.value }))} className="w-full rounded-xl border border-slate-200 px-3 py-2" /></label></div><div className="mt-4 flex flex-wrap gap-2"><button type="button" onClick={generateDraft} className="rounded-xl border border-indigo-200 bg-indigo-50 px-4 py-2 text-sm font-semibold text-indigo-700">Generate draft</button><button type="button" onClick={saveDraft} className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-700">{form.id ? "Update" : "Create"}</button><button type="button" onClick={resetForm} className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700">Reset</button></div>{loading ? <div className="mt-4 grid gap-2"><LoadingSkeleton className="h-12 w-full" /><LoadingSkeleton className="h-12 w-full" /></div> : drafts.length === 0 ? <div className="mt-4"><EmptyState title="Chưa có AI draft" message="Generate draft để bắt đầu." /></div> : <div className="mt-4 overflow-x-auto"><table className="w-full min-w-[980px] text-left text-sm"><thead><tr className="border-b border-slate-200 text-slate-600"><th className="px-2 py-2 font-semibold">Draft ID</th><th className="px-2 py-2 font-semibold">Product</th><th className="px-2 py-2 font-semibold">Quick verdict</th><th className="px-2 py-2 font-semibold">Status</th><th className="px-2 py-2 font-semibold">Actions</th></tr></thead><tbody>{drafts.map((item) => <tr key={item.id} className="border-b border-slate-100"><td className="px-2 py-2 text-slate-800">{item.id}</td><td className="px-2 py-2 text-slate-700">{item.productName}</td><td className="px-2 py-2 text-slate-700"><span className="block max-w-[280px] truncate">{item.quickVerdict}</span></td><td className="px-2 py-2 text-slate-700">{item.status}</td><td className="px-2 py-2"><div className="flex flex-wrap gap-1"><button type="button" onClick={() => { setSelectedId(item.id); editDraft(item); }} className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700">Edit draft</button><button type="button" onClick={() => approveDraft(item.id)} className="rounded-lg border border-emerald-200 bg-emerald-50 px-2 py-1 text-xs font-semibold text-emerald-700">Approve</button><button type="button" onClick={() => rejectDraft(item.id)} className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs font-semibold text-red-700">Reject</button><button type="button" onClick={() => createReviewFromApproved(item.id)} className="rounded-lg border border-blue-200 bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700">Create review</button></div></td></tr>)}</tbody></table></div>}{selectedDraft ? <div className="mt-4 rounded-2xl border border-slate-200 p-4"><p className="text-sm font-semibold text-slate-900">Preview selected draft</p><p className="mt-2 text-sm text-slate-700">{selectedDraft.quickVerdict}</p><p className="mt-2 text-xs text-slate-500">AI output là bản nháp, cần admin review trước khi publish.</p></div> : null}</section></PageContainer>;
}
