"use client";

export const dynamic = "force-dynamic";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer } from "@/components/ui";

type Category = {
  id: string;
  name: string;
  slug: string;
  parentId: string | null;
  icon: string | null;
  description: string | null;
  seoTitle: string | null;
  seoDescription: string | null;
  sortOrder: number;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
};

type FormState = {
  id?: string;
  name: string;
  slug: string;
  parentId: string;
  icon: string;
  description: string;
  seoTitle: string;
  seoDescription: string;
  sortOrder: string;
};

const initialForm: FormState = { name: "", slug: "", parentId: "", icon: "", description: "", seoTitle: "", seoDescription: "", sortOrder: "" };

function slugify(value: string) {
  return value.toLowerCase().trim().replace(/[^a-z0-9\s-]/g, "").replace(/\s+/g, "-");
}

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [form, setForm] = useState<FormState>(initialForm);
  const [message, setMessage] = useState("");
  const [, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const parentOptions = useMemo(() => categories.filter((c) => c.status !== "ARCHIVED"), [categories]);

  async function loadCategories() {
    setLoading(true);
    setLoadError("");
    try {
      const response = await fetch("/api/admin/categories?limit=300");
      const payload = (await response.json()) as { success: boolean; data?: { items: Category[] }; error?: string };
      if (!response.ok || !payload.success || !payload.data) throw new Error(payload.error ?? "Không tải được categories");
      setCategories(payload.data.items);
    } catch (e) {
      setCategories([]);
      setLoadError(e instanceof Error ? e.message : "Không tải được categories");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void (async () => {
      await loadCategories();
    })();
  }, []);

  function resetForm() { setForm(initialForm); setErrors({}); }

  async function saveCategory() {
    const nextErrors: Partial<Record<keyof FormState, string>> = {};
    if (!form.name.trim()) nextErrors.name = "Name là bắt buộc.";
    if (!form.slug.trim()) nextErrors.slug = "Slug là bắt buộc.";
    if (!form.description.trim()) nextErrors.description = "Description là bắt buộc.";
    if (!form.seoTitle.trim()) nextErrors.seoTitle = "SEO title là bắt buộc.";
    if (!form.seoDescription.trim()) nextErrors.seoDescription = "SEO description là bắt buộc.";
    if (!form.sortOrder.trim()) nextErrors.sortOrder = "Sort order là bắt buộc.";
    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      setMessage("Vui lòng điền đầy đủ các trường bắt buộc.");
      return;
    }

    const payload = {
      name: form.name,
      slug: form.slug,
      parentId: form.parentId,
      icon: form.icon,
      description: form.description,
      seoTitle: form.seoTitle,
      seoDescription: form.seoDescription,
      sortOrder: Number(form.sortOrder),
    };

    const response = form.id
      ? await fetch(`/api/admin/categories/${form.id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) })
      : await fetch("/api/admin/categories", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });

    const result = (await response.json()) as { success: boolean; error?: string };
    if (!response.ok || !result.success) {
      setMessage(result.error ?? "Không lưu được category.");
      return;
    }

    setMessage(form.id ? "Đã cập nhật category." : "Đã tạo category mới.");
    resetForm();
    await loadCategories();
  }

  function editCategory(item: Category) {
    setForm({ id: item.id, name: item.name, slug: item.slug, parentId: item.parentId || "", icon: item.icon || "", description: item.description || "", seoTitle: item.seoTitle || "", seoDescription: item.seoDescription || "", sortOrder: String(item.sortOrder) });
    setMessage("Đang chỉnh sửa category.");
  }

  async function archiveCategory(id: string) {
    const ok = window.confirm("Bạn có chắc muốn archive category này?");
    if (!ok) return;
    const response = await fetch(`/api/admin/categories/${id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ action: "archive" }) });
    const result = (await response.json()) as { success: boolean; error?: string };
    if (!response.ok || !result.success) { setMessage(result.error ?? "Không archive được category."); return; }
    setMessage("Đã archive category.");
    await loadCategories();
  }

  async function deleteCategory(id: string) {
    const ok = window.confirm("Bạn có chắc muốn xóa category này?");
    if (!ok) return;
    const response = await fetch(`/api/admin/categories/${id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ action: "delete" }) });
    const result = (await response.json()) as { success: boolean; error?: string };
    if (!response.ok || !result.success) { setMessage(result.error ?? "Không xóa được category."); return; }
    setMessage("Đã xóa category.");
    await loadCategories();
  }

  return <PageContainer><section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6"><div className="flex flex-wrap items-center justify-between gap-3"><div><h1 className="text-2xl font-bold tracking-tight text-slate-900">Admin Categories</h1><p className="mt-1 text-sm text-slate-600">Quản lý cấu trúc danh mục và metadata SEO.</p></div><Link href="/admin" className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">Về dashboard</Link></div>{message ? <div className="mt-4 rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-blue-700">{message}</div> : null}{loadError ? <div className="mt-4"><ErrorState title="Không tải được categories" message={loadError} /></div> : null}<div className="mt-4 grid gap-3 lg:grid-cols-2"><label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Name</span><input value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value, slug: p.slug || slugify(e.target.value) }))} className="h-11 w-full rounded-xl border border-slate-200 px-3" /></label><label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Slug</span><input value={form.slug} onChange={(e) => setForm((p) => ({ ...p, slug: e.target.value }))} className="h-11 w-full rounded-xl border border-slate-200 px-3" /></label><label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Parent category</span><select value={form.parentId} onChange={(e) => setForm((p) => ({ ...p, parentId: e.target.value }))} className="h-11 w-full rounded-xl border border-slate-200 px-3"><option value="">Không có parent</option>{parentOptions.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label><label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Icon</span><input value={form.icon} onChange={(e) => setForm((p) => ({ ...p, icon: e.target.value }))} placeholder="vd: 💻" className="h-11 w-full rounded-xl border border-slate-200 px-3" /></label><label className="space-y-1 text-sm lg:col-span-2"><span className="font-semibold text-slate-800">Description</span><textarea value={form.description} onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))} rows={2} className="w-full rounded-xl border border-slate-200 px-3 py-2" /></label><label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">SEO title</span><input value={form.seoTitle} onChange={(e) => setForm((p) => ({ ...p, seoTitle: e.target.value }))} className="h-11 w-full rounded-xl border border-slate-200 px-3" /></label><label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">SEO description</span><input value={form.seoDescription} onChange={(e) => setForm((p) => ({ ...p, seoDescription: e.target.value }))} className="h-11 w-full rounded-xl border border-slate-200 px-3" /></label><label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Sort order</span><input value={form.sortOrder} onChange={(e) => setForm((p) => ({ ...p, sortOrder: e.target.value }))} className="h-11 w-full rounded-xl border border-slate-200 px-3" /></label></div><div className="mt-4 flex flex-wrap gap-2"><button type="button" onClick={saveCategory} className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-700">{form.id ? "Update" : "Create"}</button><button type="button" onClick={resetForm} className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700">Reset</button></div>{loading ? <div className="mt-4 grid gap-2"><LoadingSkeleton className="h-12 w-full" /><LoadingSkeleton className="h-12 w-full" /></div> : categories.length === 0 ? <div className="mt-4"><EmptyState title="Chưa có category" message="Tạo category mới để bắt đầu." /></div> : <div className="mt-4 overflow-x-auto"><table className="w-full min-w-[920px] text-left text-sm"><thead><tr className="border-b border-slate-200 text-slate-600"><th className="px-2 py-2 font-semibold">Name</th><th className="px-2 py-2 font-semibold">Slug</th><th className="px-2 py-2 font-semibold">Parent</th><th className="px-2 py-2 font-semibold">Icon</th><th className="px-2 py-2 font-semibold">Sort</th><th className="px-2 py-2 font-semibold">Status</th><th className="px-2 py-2 font-semibold">Actions</th></tr></thead><tbody>{categories.map((item) => <tr key={item.id} className="border-b border-slate-100"><td className="px-2 py-2 text-slate-800">{item.name}</td><td className="px-2 py-2 text-slate-700">{item.slug}</td><td className="px-2 py-2 text-slate-700">{categories.find((c) => c.id === item.parentId)?.name || "-"}</td><td className="px-2 py-2 text-slate-700">{item.icon || "-"}</td><td className="px-2 py-2 text-slate-700">{item.sortOrder}</td><td className="px-2 py-2 text-slate-700">{item.status}</td><td className="px-2 py-2"><div className="flex flex-wrap gap-1"><button type="button" onClick={() => editCategory(item)} className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700">Edit</button><button type="button" onClick={() => archiveCategory(item.id)} className="rounded-lg border border-amber-200 bg-amber-50 px-2 py-1 text-xs font-semibold text-amber-700">Archive</button><button type="button" onClick={() => deleteCategory(item.id)} className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs font-semibold text-red-700">Delete</button></div></td></tr>)}</tbody></table></div>}</section></PageContainer>;
}
