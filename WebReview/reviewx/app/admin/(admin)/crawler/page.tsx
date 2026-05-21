"use client";

export const dynamic = "force-dynamic";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer } from "@/components/ui";

type CrawlerJobStatus = "PENDING" | "RUNNING" | "SUCCESS" | "FAILED" | "RETRYING" | "CAPTCHA_REQUIRED";
type CrawlerJobType = "PRODUCT_BY_URL" | "PRODUCT_BY_KEYWORD";

type CrawlerJob = {
  id: string;
  type: CrawlerJobType;
  input: string;
  status: CrawlerJobStatus;
  logs: string | null;
  rawResult: string | null;
  createdAt: string;
};

function statusClass(status: CrawlerJobStatus) {
  if (status === "SUCCESS") return "border-emerald-200 bg-emerald-50 text-emerald-700";
  if (status === "FAILED") return "border-red-200 bg-red-50 text-red-700";
  if (status === "RUNNING") return "border-blue-200 bg-blue-50 text-blue-700";
  if (status === "RETRYING") return "border-purple-200 bg-purple-50 text-purple-700";
  if (status === "CAPTCHA_REQUIRED") return "border-amber-200 bg-amber-50 text-amber-700";
  return "border-slate-300 bg-slate-100 text-slate-700";
}

export default function AdminCrawlerPage() {
  const [jobs, setJobs] = useState<CrawlerJob[]>([]);
  const [urlInput, setUrlInput] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [selectedId, setSelectedId] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const selectedJob = useMemo(() => jobs.find((job) => job.id === selectedId) || null, [jobs, selectedId]);

  const loadJobs = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await fetch("/api/admin/crawler?limit=100");
      const payload = (await response.json()) as { success: boolean; data?: { items: CrawlerJob[] }; error?: string };
      if (!response.ok || !payload.success || !payload.data) throw new Error(payload.error ?? "Không tải được crawler jobs");
      setJobs(payload.data.items);
      if (!selectedId && payload.data.items[0]) setSelectedId(payload.data.items[0].id);
    } catch (e) {
      setJobs([]);
      setError(e instanceof Error ? e.message : "Không tải được crawler jobs");
    } finally {
      setLoading(false);
    }
  }, [selectedId]);

  useEffect(() => {
    void (async () => {
      await loadJobs();
    })();
  }, [loadJobs]);

  async function createJob(type: CrawlerJobType, input: string) {
    if (!input.trim()) {
      setMessage("Vui lòng nhập input cho crawler job.");
      return;
    }
    const response = await fetch("/api/admin/crawler", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ type, input: input.trim() }),
    });
    const payload = (await response.json()) as { success: boolean; data?: CrawlerJob; error?: string };
    if (!response.ok || !payload.success || !payload.data) {
      setMessage(payload.error ?? "Không tạo được crawler job.");
      return;
    }
    setMessage("Đã tạo crawler job.");
    if (type === "PRODUCT_BY_URL") setUrlInput("");
    if (type === "PRODUCT_BY_KEYWORD") setKeywordInput("");
    await loadJobs();
    setSelectedId(payload.data.id);
  }

  async function patchJob(id: string, data: { status?: CrawlerJobStatus; appendLog?: string; rawResult?: string }) {
    const response = await fetch(`/api/admin/crawler/${id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    const payload = (await response.json()) as { success: boolean; error?: string };
    if (!response.ok || !payload.success) {
      setMessage(payload.error ?? "Không cập nhật được job.");
      return false;
    }
    await loadJobs();
    return true;
  }

  async function retryFailedJob(id: string) {
    const ok = await patchJob(id, { status: "RETRYING", appendLog: "Retry requested" });
    if (ok) setMessage("Đã retry job thất bại/challenge.");
  }

  async function cancelPendingJob(id: string) {
    const ok = await patchJob(id, { status: "FAILED", appendLog: "Cancelled from admin" });
    if (ok) setMessage("Đã cancel pending job.");
  }

  async function createDraftFromRaw(id: string) {
    const ok = await patchJob(id, { appendLog: "Draft product created from raw source" });
    if (ok) setMessage("Đã tạo product draft từ raw data.");
  }

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Admin Crawler</h1>
            <p className="mt-1 text-sm text-slate-600">Crawler job flow với trạng thái đồng bộ từ DB.</p>
          </div>
          <Link href="/admin" className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">Về dashboard</Link>
        </div>

        {message ? <div className="mt-4 rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-blue-700">{message}</div> : null}
        {error ? <div className="mt-4"><ErrorState title="Không tải được crawler jobs" message={error} /></div> : null}

        <div className="mt-4 grid gap-3 lg:grid-cols-2">
          <div className="rounded-2xl border border-slate-200 p-3">
            <p className="text-sm font-semibold text-slate-900">Create job by product URL</p>
            <div className="mt-2 flex gap-2">
              <input value={urlInput} onChange={(e) => setUrlInput(e.target.value)} placeholder="https://shopee.vn/..." className="h-10 flex-1 rounded-xl border border-slate-200 px-3 text-sm" />
              <button type="button" onClick={() => createJob("PRODUCT_BY_URL", urlInput)} className="rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-xs font-semibold text-blue-700">Create</button>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 p-3">
            <p className="text-sm font-semibold text-slate-900">Create job by keyword</p>
            <div className="mt-2 flex gap-2">
              <input value={keywordInput} onChange={(e) => setKeywordInput(e.target.value)} placeholder="tai nghe bluetooth" className="h-10 flex-1 rounded-xl border border-slate-200 px-3 text-sm" />
              <button type="button" onClick={() => createJob("PRODUCT_BY_KEYWORD", keywordInput)} className="rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-xs font-semibold text-blue-700">Create</button>
            </div>
          </div>
        </div>

        {loading ? <div className="mt-4"><LoadingSkeleton className="h-12 w-full" /></div> : null}
        {!loading && jobs.length === 0 ? <div className="mt-4"><EmptyState title="Chưa có crawler job" message="Tạo job mới để bắt đầu workflow." /></div> : null}

        {!loading && jobs.length > 0 ? (
          <>
            <div className="mt-4 overflow-x-auto">
              <table className="w-full min-w-[980px] text-left text-sm">
                <thead><tr className="border-b border-slate-200 text-slate-600"><th className="px-2 py-2 font-semibold">Job ID</th><th className="px-2 py-2 font-semibold">Type</th><th className="px-2 py-2 font-semibold">Input</th><th className="px-2 py-2 font-semibold">Status</th><th className="px-2 py-2 font-semibold">Created</th><th className="px-2 py-2 font-semibold">Actions</th></tr></thead>
                <tbody>
                  {jobs.map((job) => (
                    <tr key={job.id} className="border-b border-slate-100">
                      <td className="px-2 py-2 text-slate-800">{job.id}</td>
                      <td className="px-2 py-2 text-slate-700">{job.type}</td>
                      <td className="px-2 py-2 text-slate-700"><span className="block max-w-[220px] truncate">{job.input}</span></td>
                      <td className="px-2 py-2"><span className={`rounded-full border px-2 py-0.5 text-xs font-semibold ${statusClass(job.status)}`}>{job.status}</span></td>
                      <td className="px-2 py-2 text-slate-700">{new Date(job.createdAt).toLocaleString("vi-VN")}</td>
                      <td className="px-2 py-2"><div className="flex flex-wrap gap-1"><button type="button" onClick={() => setSelectedId(job.id)} className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700">View</button><button type="button" onClick={() => retryFailedJob(job.id)} className="rounded-lg border border-purple-200 bg-purple-50 px-2 py-1 text-xs font-semibold text-purple-700">Retry</button><button type="button" onClick={() => cancelPendingJob(job.id)} className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs font-semibold text-red-700">Cancel pending</button><button type="button" onClick={() => createDraftFromRaw(job.id)} className="rounded-lg border border-emerald-200 bg-emerald-50 px-2 py-1 text-xs font-semibold text-emerald-700">Create draft</button></div></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {selectedJob ? (
              <div className="mt-4 grid gap-3 lg:grid-cols-2">
                <div className="rounded-2xl border border-slate-200 p-3"><p className="text-sm font-semibold text-slate-900">Job logs</p><pre className="mt-2 whitespace-pre-wrap rounded-xl border border-slate-100 bg-slate-50 p-3 text-xs text-slate-700">{selectedJob.logs || ""}</pre></div>
                <div className="rounded-2xl border border-slate-200 p-3"><p className="text-sm font-semibold text-slate-900">Raw result JSON viewer</p><pre className="mt-2 overflow-x-auto rounded-xl border border-slate-100 bg-slate-50 p-3 text-xs text-slate-700">{selectedJob.rawResult || ""}</pre></div>
              </div>
            ) : null}
          </>
        ) : null}
      </section>
    </PageContainer>
  );
}
