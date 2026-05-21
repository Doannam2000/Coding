"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer } from "@/components/ui";
import type { ClickEvent } from "@/lib/analytics-db";

const series = [
  { day: "05/05", clicks: 1 },
  { day: "06/05", clicks: 2 },
  { day: "07/05", clicks: 2 },
  { day: "08/05", clicks: 2 },
  { day: "09/05", clicks: 2 },
  { day: "10/05", clicks: 3 },
];

function countBy<T extends string>(rows: ClickEvent[], key: (row: ClickEvent) => T) {
  const map = new Map<T, number>();
  rows.forEach((row) => {
    const k = key(row);
    map.set(k, (map.get(k) || 0) + 1);
  });
  return Array.from(map.entries()).map(([name, clicks]) => ({ name, clicks })).sort((a, b) => b.clicks - a.clicks);
}

export default function AdminAnalyticsPage() {
  const [range, setRange] = useState<"7D" | "30D" | "90D">("7D");
  const [loading, setLoading] = useState(true);
  const [forceError, setForceError] = useState(false);
  const [events, setEvents] = useState<ClickEvent[]>([]);

  useEffect(() => {
    let cancelled = false;
    async function loadEvents() {
      setLoading(true);
      try {
        const response = await fetch(`/api/admin/analytics/events?range=${range}`);
        if (!response.ok) throw new Error("Failed to fetch analytics events");
        const payload = (await response.json()) as { events: ClickEvent[] };
        if (!cancelled) {
          setEvents(payload.events);
          setForceError(false);
        }
      } catch {
        if (!cancelled) {
          setForceError(true);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadEvents();
    return () => {
      cancelled = true;
    };
  }, [range]);

  const today = "2026-05-10";

  const metrics = useMemo(() => {
    const totalClicks = events.length;
    const clicksToday = events.filter((e) => e.date === today).length;
    return { totalClicks, clicksToday };
  }, [events]);

  const byPlatform = useMemo(() => countBy(events, (row) => row.platform), [events]);
  const byProduct = useMemo(() => countBy(events, (row) => row.product).slice(0, 5), [events]);
  const byReview = useMemo(() => countBy(events, (row) => row.review).slice(0, 5), [events]);
  const topLinks = useMemo(() => countBy(events, (row) => row.affiliateLabel).slice(0, 5), [events]);
  const topCategories = useMemo(() => countBy(events, (row) => row.category).slice(0, 5), [events]);

  const maxBar = Math.max(...series.map((point) => point.clicks), 1);

  function exportCsv() {
    const header = "id,date,platform,product,review,affiliateLabel,category";
    const body = events.map((e) => [e.id, e.date, e.platform, e.product, e.review, e.affiliateLabel, e.category].join(",")).join("\n");
    const csv = `${header}\n${body}`;
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "reviewx-analytics-clicks.csv";
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Admin Analytics</h1>
            <p className="mt-1 text-sm text-slate-600">Theo dõi hiệu suất affiliate link và hiệu quả nội dung review.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Link href="/admin" className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">Về dashboard</Link>
            <button type="button" onClick={exportCsv} className="rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-sm font-semibold text-blue-700">Export CSV</button>
          </div>
        </div>

        <div className="mt-4 flex flex-wrap items-center gap-2">
          <select value={range} onChange={(e) => setRange(e.target.value as "7D" | "30D" | "90D")} className="h-10 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100">
            <option value="7D">7 ngày</option>
            <option value="30D">30 ngày</option>
            <option value="90D">90 ngày</option>
          </select>
        </div>

        {forceError ? (
          <div className="mt-4"><ErrorState title="Analytics API lỗi" message="Không thể tải dữ liệu analytics. Vui lòng thử lại." /></div>
        ) : loading ? (
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {Array.from({ length: 4 }).map((_, idx) => (
              <LoadingSkeleton key={idx} className="h-24 w-full rounded-2xl border border-slate-200" />
            ))}
          </div>
        ) : events.length === 0 ? (
          <div className="mt-4"><EmptyState title="Chưa có click data" message="Chưa ghi nhận click event nào trong khoảng thời gian đã chọn." /></div>
        ) : (
          <>
            <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Total clicks</p>
                <p className="mt-2 text-2xl font-bold text-slate-900">{metrics.totalClicks}</p>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Clicks today</p>
                <p className="mt-2 text-2xl font-bold text-slate-900">{metrics.clicksToday}</p>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Date range</p>
                <p className="mt-2 text-2xl font-bold text-slate-900">{range}</p>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Recent click events</p>
                <p className="mt-2 text-2xl font-bold text-slate-900">{events.length}</p>
              </div>
            </div>

            <div className="mt-4 grid gap-3 lg:grid-cols-3">
              <div className="rounded-2xl border border-slate-200 bg-white p-4 lg:col-span-2">
                <p className="text-sm font-semibold text-slate-900">Clicks by day chart</p>
                <div className="mt-3 grid grid-cols-6 items-end gap-2">
                  {series.map((point) => (
                    <div key={point.day} className="space-y-1 text-center">
                      <div className="mx-auto w-full max-w-[52px] rounded-t-lg bg-blue-500/80" style={{ height: `${Math.max(18, Math.round((point.clicks / maxBar) * 120))}px` }} />
                      <p className="text-xs text-slate-500">{point.day}</p>
                      <p className="text-xs font-semibold text-slate-700">{point.clicks}</p>
                    </div>
                  ))}
                </div>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <p className="text-sm font-semibold text-slate-900">Clicks by platform</p>
                <ul className="mt-3 space-y-2">
                  {byPlatform.map((row) => (
                    <li key={row.name} className="flex items-center justify-between rounded-lg border border-slate-100 px-3 py-2 text-sm">
                      <span className="text-slate-700">{row.name}</span>
                      <span className="font-semibold text-slate-900">{row.clicks}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="mt-4 grid gap-3 lg:grid-cols-2">
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <p className="text-sm font-semibold text-slate-900">Clicks by product</p>
                <ul className="mt-3 space-y-2">
                  {byProduct.map((row) => (
                    <li key={row.name} className="flex items-center justify-between rounded-lg border border-slate-100 px-3 py-2 text-sm">
                      <span className="text-slate-700">{row.name}</span>
                      <span className="font-semibold text-slate-900">{row.clicks}</span>
                    </li>
                  ))}
                </ul>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <p className="text-sm font-semibold text-slate-900">Clicks by review</p>
                <ul className="mt-3 space-y-2">
                  {byReview.map((row) => (
                    <li key={row.name} className="flex items-center justify-between rounded-lg border border-slate-100 px-3 py-2 text-sm">
                      <span className="text-slate-700">{row.name}</span>
                      <span className="font-semibold text-slate-900">{row.clicks}</span>
                    </li>
                  ))}
                </ul>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <p className="text-sm font-semibold text-slate-900">Top affiliate links</p>
                <ul className="mt-3 space-y-2">
                  {topLinks.map((row) => (
                    <li key={row.name} className="flex items-center justify-between rounded-lg border border-slate-100 px-3 py-2 text-sm">
                      <span className="text-slate-700">{row.name}</span>
                      <span className="font-semibold text-slate-900">{row.clicks}</span>
                    </li>
                  ))}
                </ul>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <p className="text-sm font-semibold text-slate-900">Top categories</p>
                <ul className="mt-3 space-y-2">
                  {topCategories.map((row) => (
                    <li key={row.name} className="flex items-center justify-between rounded-lg border border-slate-100 px-3 py-2 text-sm">
                      <span className="text-slate-700">{row.name}</span>
                      <span className="font-semibold text-slate-900">{row.clicks}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="mt-4 overflow-x-auto rounded-2xl border border-slate-200">
              <table className="w-full min-w-[920px] text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-600">
                    <th className="px-3 py-2 font-semibold">Date</th>
                    <th className="px-3 py-2 font-semibold">Platform</th>
                    <th className="px-3 py-2 font-semibold">Product</th>
                    <th className="px-3 py-2 font-semibold">Review</th>
                    <th className="px-3 py-2 font-semibold">Affiliate link</th>
                    <th className="px-3 py-2 font-semibold">Category</th>
                  </tr>
                </thead>
                <tbody>
                  {events.map((event) => (
                    <tr key={event.id} className="border-b border-slate-100">
                      <td className="px-3 py-2 text-slate-700">{event.date}</td>
                      <td className="px-3 py-2 text-slate-700">{event.platform}</td>
                      <td className="px-3 py-2 text-slate-700">{event.product}</td>
                      <td className="px-3 py-2 text-slate-700">{event.review}</td>
                      <td className="px-3 py-2 text-slate-700">{event.affiliateLabel}</td>
                      <td className="px-3 py-2 text-slate-700">{event.category}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>
    </PageContainer>
  );
}
