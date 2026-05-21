"use client";

import { useMemo, useState } from "react";
import { PageContainer, SectionHeader } from "@/components/ui";

type ConvertStatus = "PENDING" | "RESOLVED" | "CONVERTED" | "FAILED" | "CAPTCHA_REQUIRED" | "DUPLICATE";

type ShortLinkItem = {
  id: string;
  inputUrl: string;
  resolvedUrl: string | null;
  platform: string;
  shopId: string | null;
  itemId: string | null;
  internalTrackingUrl: string | null;
  status: ConvertStatus;
  errorMessage: string | null;
  createdAt: string;
  affiliateLink?: { affiliateUrl: string; internalUrl: string } | null;
};

type ApiResponse<T> = { success: true; data: T } | { success: false; error: string };

function statusClass(status: ConvertStatus) {
  if (status === "CONVERTED") return "border-emerald-200 bg-emerald-50 text-emerald-700";
  if (status === "RESOLVED") return "border-blue-200 bg-blue-50 text-blue-700";
  if (status === "PENDING") return "border-amber-200 bg-amber-50 text-amber-700";
  if (status === "DUPLICATE") return "border-purple-200 bg-purple-50 text-purple-700";
  if (status === "CAPTCHA_REQUIRED") return "border-orange-200 bg-orange-50 text-orange-700";
  return "border-red-200 bg-red-50 text-red-700";
}

export default function ShortLinkConverterPage() {
  const [inputUrl, setInputUrl] = useState("");
  const [result, setResult] = useState<ShortLinkItem | null>(null);
  const [error, setError] = useState("");
  const [history, setHistory] = useState<ShortLinkItem[]>([]);
  const [loading, setLoading] = useState(false);
  const canConvert = useMemo(() => inputUrl.trim().length > 0 && !loading, [inputUrl, loading]);

  async function loadHistory() {
    const res = await fetch("/api/admin/tools/short-link-converter?limit=10&page=1", { cache: "no-store" });
    const json = (await res.json()) as ApiResponse<{ items: ShortLinkItem[] }>;
    if (json.success) setHistory(json.data.items);
  }


  function clearAll() {
    setInputUrl("");
    setResult(null);
    setError("");
  }

  async function convertLink() {
    setLoading(true);
    setError("");
    setResult(null);
    try {
      const res = await fetch("/api/admin/tools/short-link-converter", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ inputUrl: inputUrl.trim() }),
      });
      const json = (await res.json()) as ApiResponse<ShortLinkItem>;
      if (!json.success) {
        setError(json.error || "Chuyển đổi thất bại.");
        return;
      }
      setResult(json.data);
      if (json.data.status === "FAILED" || json.data.status === "CAPTCHA_REQUIRED") {
        setError(json.data.errorMessage || "Chuyển đổi thất bại.");
      }
      await loadHistory();
    } catch {
      setError("Không thể gọi API chuyển đổi.");
    } finally {
      setLoading(false);
    }
  }

  async function copyText(value?: string | null) {
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
    } catch {}
  }

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
        <SectionHeader title="Chuyển link rút gọn Shopee" subtitle="Dán link Shopee để chuẩn hóa và tạo đường dẫn tracking nội bộ." />

        <div className="mt-4 space-y-3">
          <input
            value={inputUrl}
            onChange={(e) => setInputUrl(e.target.value)}
            placeholder="Dán link s.shopee.vn / shopee.ee / shp.ee / shopee.vn"
            className="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-900 outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          />

          <div className="flex flex-wrap gap-2">
            <button type="button" onClick={convertLink} disabled={!canConvert} className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50">
              {loading ? "Đang convert..." : "Convert link"}
            </button>
            <button type="button" onClick={loadHistory} disabled={loading} className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 disabled:opacity-50">Load history</button>
            <button type="button" onClick={clearAll} className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700">Clear</button>
          </div>

          {error ? (
            <div className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              <p className="font-semibold">Lỗi chuyển đổi</p>
              <p className="mt-1">{error}</p>
            </div>
          ) : null}

          {result ? (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
              <div className="mb-3 flex flex-wrap items-center gap-2">
                <span className={`rounded-full border px-2.5 py-1 text-xs font-semibold ${statusClass(result.status)}`}>{result.status}</span>
              </div>
              <p><span className="font-semibold text-slate-900">inputUrl:</span> {result.inputUrl}</p>
              <p className="mt-1"><span className="font-semibold text-slate-900">resolvedUrl:</span> {result.resolvedUrl ?? ""}</p>
              <p className="mt-1"><span className="font-semibold text-slate-900">platform:</span> {result.platform}</p>
              <p className="mt-1"><span className="font-semibold text-slate-900">shopId:</span> {result.shopId ?? ""}</p>
              <p className="mt-1"><span className="font-semibold text-slate-900">itemId:</span> {result.itemId ?? ""}</p>
              <p className="mt-1"><span className="font-semibold text-slate-900">internalTrackingUrl:</span> {result.internalTrackingUrl ?? ""}</p>
              <p className="mt-1"><span className="font-semibold text-slate-900">errorMessage:</span> {result.errorMessage ?? ""}</p>

              <div className="mt-3 flex flex-wrap gap-2">
                <button type="button" onClick={() => copyText(result.affiliateLink?.affiliateUrl)} disabled={!result.affiliateLink?.affiliateUrl} className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 disabled:opacity-50">Copy affiliate URL</button>
                <button type="button" onClick={() => copyText(result.internalTrackingUrl)} disabled={!result.internalTrackingUrl} className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 disabled:opacity-50">Copy internal URL</button>
                {result.resolvedUrl ? <a href={result.resolvedUrl} target="_blank" rel="noreferrer" className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700">Open product</a> : null}
                {result.internalTrackingUrl ? <a href={result.internalTrackingUrl} target="_blank" rel="noreferrer" className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700">Open internal link</a> : null}
              </div>
            </div>
          ) : null}
        </div>
      </section>

      <section className="mt-6 rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm">
        <h2 className="text-sm font-semibold text-slate-900">Lịch sử chuyển đổi</h2>
        <div className="mt-3 overflow-x-auto">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-600">
                <th className="px-2 py-2 font-semibold">Input</th>
                <th className="px-2 py-2 font-semibold">Platform</th>
                <th className="px-2 py-2 font-semibold">Status</th>
                <th className="px-2 py-2 font-semibold">Internal URL</th>
              </tr>
            </thead>
            <tbody>
              {history.length === 0 ? (
                <tr>
                  <td className="px-2 py-3 text-slate-500" colSpan={4}>Chưa có lịch sử chuyển đổi.</td>
                </tr>
              ) : (
                history.map((item) => (
                  <tr key={item.id} className="border-b border-slate-100">
                    <td className="px-2 py-2 text-slate-700">{item.inputUrl}</td>
                    <td className="px-2 py-2 text-slate-700">{item.platform}</td>
                    <td className="px-2 py-2 text-slate-700"><span className={`rounded-full border px-2 py-0.5 text-xs font-semibold ${statusClass(item.status)}`}>{item.status}</span></td>
                    <td className="px-2 py-2 text-slate-700">{item.internalTrackingUrl ?? ""}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </PageContainer>
  );
}
