"use client";

export const dynamic = "force-dynamic";

import Link from "next/link";
import { useEffect, useState } from "react";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer } from "@/components/ui";

type Brand = {
  id: string;
  name: string;
  slug: string;
  logo: string | null;
  website: string | null;
  description: string | null;
  trustScore: number;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
};

type FormState = {
  id?: string;
  name: string;
  slug: string;
  logo: string;
  website: string;
  description: string;
  trustScore: string;
};

const initialForm: FormState = { name: "", slug: "", logo: "", website: "", description: "", trustScore: "" };

export default function AdminBrandsPage() {
  const [brands, setBrands] = useState<Brand[]>([]);
  const [form, setForm] = useState<FormState>(initialForm);
  const [message, setMessage] = useState("");
  const [errors, setErrors] = useState<Partial<Record<keyof FormState, string>>>({});
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  function fieldClass(key: keyof FormState) {
    return `h-11 w-full rounded-xl border px-3 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 ${errors[key] ? "border-red-300 bg-red-50" : "border-slate-200"}`;
  }

  async function loadBrands() {
    setLoading(true);
    setLoadError("");
    try {
      const response = await fetch("/api/admin/brands?limit=200");
      const payload = (await response.json()) as { success: boolean; data?: { items: Brand[] }; error?: string };
      if (!response.ok || !payload.success || !payload.data) throw new Error(payload.error ?? "Không tải được brands");
      setBrands(payload.data.items);
    } catch (e) {
      setBrands([]);
      setLoadError(e instanceof Error ? e.message : "Không tải được brands");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void (async () => {
      await loadBrands();
    })();
  }, []);

  function resetForm() {
    setForm(initialForm);
    setErrors({});
  }

  async function saveBrand() {
    const nextErrors: Partial<Record<keyof FormState, string>> = {};
    if (!form.name.trim()) nextErrors.name = "Brand name là bắt buộc.";
    if (!form.slug.trim()) nextErrors.slug = "Slug là bắt buộc.";
    if (!form.trustScore.trim()) nextErrors.trustScore = "Trust score là bắt buộc.";
    if (Object.keys(nextErrors).length > 0) {
      setErrors(nextErrors);
      setMessage("Vui lòng điền đầy đủ các trường thương hiệu.");
      return;
    }

    const payload = {
      name: form.name,
      slug: form.slug,
      icon: form.logo,
      description: form.description,
    };

    const response = form.id
      ? await fetch(`/api/admin/brands/${form.id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) })
      : await fetch("/api/admin/brands", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });

    const result = (await response.json()) as { success: boolean; error?: string };
    if (!response.ok || !result.success) {
      setMessage(result.error ?? "Không lưu được thương hiệu.");
      return;
    }

    setMessage(form.id ? "Đã cập nhật thương hiệu." : "Đã tạo thương hiệu mới.");
    resetForm();
    await loadBrands();
  }

  function editBrand(item: Brand) {
    setForm({
      id: item.id,
      name: item.name,
      slug: item.slug,
      logo: item.logo ?? "",
      website: item.website ?? "",
      description: item.description ?? "",
      trustScore: String(item.trustScore ?? 0),
    });
    setMessage("Đang chỉnh sửa thương hiệu.");
  }

  async function archiveBrand(id: string) {
    const ok = window.confirm("Bạn có chắc muốn archive thương hiệu này?");
    if (!ok) return;
    const response = await fetch(`/api/admin/brands/${id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ action: "archive" }) });
    const result = (await response.json()) as { success: boolean; error?: string };
    if (!response.ok || !result.success) {
      setMessage(result.error ?? "Không archive được thương hiệu.");
      return;
    }
    setMessage("Đã archive thương hiệu.");
    await loadBrands();
  }

  async function deleteBrand(id: string) {
    const ok = window.confirm("Bạn có chắc muốn xóa thương hiệu này?");
    if (!ok) return;
    const response = await fetch(`/api/admin/brands/${id}`, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ action: "delete" }) });
    const result = (await response.json()) as { success: boolean; error?: string };
    if (!response.ok || !result.success) {
      setMessage(result.error ?? "Không xóa được thương hiệu.");
      return;
    }
    setMessage("Đã xóa thương hiệu.");
    await loadBrands();
  }

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Admin Brands</h1>
            <p className="mt-1 text-sm text-slate-600">Quản lý thương hiệu sản phẩm và độ tin cậy.</p>
          </div>
          <Link href="/admin" className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">Về dashboard</Link>
        </div>

        {message ? <div className="mt-4 rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-blue-700">{message}</div> : null}
        {loadError ? <div className="mt-4"><ErrorState title="Không tải được brands" message={loadError} /></div> : null}

        <div className="mt-4 grid gap-3 lg:grid-cols-2">
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Brand name</span><input value={form.name} onChange={(e) => { setForm((p) => ({ ...p, name: e.target.value })); setErrors((prev) => ({ ...prev, name: undefined })); }} className={fieldClass("name")} />{errors.name ? <span className="text-xs text-red-600">{errors.name}</span> : null}</label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Slug</span><input value={form.slug} onChange={(e) => { setForm((p) => ({ ...p, slug: e.target.value })); setErrors((prev) => ({ ...prev, slug: undefined })); }} className={fieldClass("slug")} />{errors.slug ? <span className="text-xs text-red-600">{errors.slug}</span> : null}</label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Logo</span><input value={form.logo} onChange={(e) => setForm((p) => ({ ...p, logo: e.target.value }))} placeholder="emoji hoặc URL" className="h-11 w-full rounded-xl border border-slate-200 px-3 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100" /></label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Website</span><input value={form.website} onChange={(e) => setForm((p) => ({ ...p, website: e.target.value }))} placeholder="https://..." className="h-11 w-full rounded-xl border border-slate-200 px-3 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100" /></label>
          <label className="space-y-1 text-sm"><span className="font-semibold text-slate-800">Brand trust score</span><input value={form.trustScore} onChange={(e) => setForm((p) => ({ ...p, trustScore: e.target.value }))} className={fieldClass("trustScore")} /></label>
          <label className="space-y-1 text-sm lg:col-span-2"><span className="font-semibold text-slate-800">Description</span><textarea value={form.description} onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))} rows={3} className="w-full rounded-xl border border-slate-200 px-3 py-2 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100" /></label>
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          <button type="button" onClick={saveBrand} className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-700">{form.id ? "Update" : "Create"}</button>
          <button type="button" onClick={resetForm} className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700">Reset</button>
        </div>

        {loading ? (
          <div className="mt-4 grid gap-2">
            <LoadingSkeleton className="h-12 w-full" />
            <LoadingSkeleton className="h-12 w-full" />
          </div>
        ) : brands.length === 0 ? (
          <div className="mt-4"><EmptyState title="Chưa có thương hiệu" message="Tạo thương hiệu mới để bắt đầu." /></div>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-600">
                  <th className="px-2 py-2 font-semibold">Name</th>
                  <th className="px-2 py-2 font-semibold">Slug</th>
                  <th className="px-2 py-2 font-semibold">Logo</th>
                  <th className="px-2 py-2 font-semibold">Website</th>
                  <th className="px-2 py-2 font-semibold">Trust score</th>
                  <th className="px-2 py-2 font-semibold">Status</th>
                  <th className="px-2 py-2 font-semibold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {brands.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100">
                    <td className="px-2 py-2 text-slate-800">{item.name}</td>
                    <td className="px-2 py-2 text-slate-700">{item.slug}</td>
                    <td className="px-2 py-2 text-slate-700">{item.logo ?? ""}</td>
                    <td className="px-2 py-2 text-slate-700">{item.website ?? ""}</td>
                    <td className="px-2 py-2 text-slate-700">{item.trustScore}</td>
                    <td className="px-2 py-2 text-slate-700">{item.status}</td>
                    <td className="px-2 py-2">
                      <div className="flex flex-wrap gap-1">
                        <button type="button" onClick={() => editBrand(item)} className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700">Edit</button>
                        <button type="button" onClick={() => archiveBrand(item.id)} className="rounded-lg border border-amber-200 bg-amber-50 px-2 py-1 text-xs font-semibold text-amber-700">Archive</button>
                        <button type="button" onClick={() => deleteBrand(item.id)} className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs font-semibold text-red-700">Delete</button>
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
