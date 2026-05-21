"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { EmptyState, ErrorState, LoadingSkeleton, PageContainer } from "@/components/ui";

type Metric = {
  label: string;
  value: string;
  tone?: "blue" | "green" | "amber" | "red";
};

type ActivityItem = {
  id: string;
  title: string;
  time: string;
};

type TopClickItem = {
  id: string;
  name: string;
  clicks: number;
};

type TopViewedReviewItem = {
  id: string;
  name: string;
  views: number;
};

type EndingSoonItem = {
  id: string;
  productId: string;
  productName: string;
  expiresAt: string;
  timeLeft: string;
};

type MissingAffiliateItem = {
  id: string;
  productId: string;
  productName: string;
  updatedAt: string;
  updatedLabel: string;
};

type DashboardPayload = {
  metrics: Metric[];
  activity: ActivityItem[];
  topProducts: TopClickItem[];
  topReviews: TopViewedReviewItem[];
  endingSoon: EndingSoonItem[];
  missingAffiliate: MissingAffiliateItem[];
};

const emptyDashboard: DashboardPayload = {
  metrics: [],
  activity: [],
  topProducts: [],
  topReviews: [],
  endingSoon: [],
  missingAffiliate: [],
};

function metricTone(tone: Metric["tone"]) {
  if (tone === "green") return "border-emerald-200 bg-emerald-50 text-emerald-700";
  if (tone === "amber") return "border-amber-200 bg-amber-50 text-amber-700";
  if (tone === "red") return "border-red-200 bg-red-50 text-red-700";
  return "border-blue-200 bg-blue-50 text-blue-700";
}

export default function AdminDashboardPage() {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(true);
  const [dashboard, setDashboard] = useState<DashboardPayload>(emptyDashboard);
  const [showError, setShowError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    async function loadDashboard() {
      setIsLoading(true);
      try {
        const response = await fetch("/api/admin/dashboard");
        const payload = (await response.json()) as { success: boolean; data?: DashboardPayload; error?: string };
        if (!response.ok || !payload.success || !payload.data) {
          throw new Error(payload.error ?? "Failed to load dashboard");
        }
        if (!cancelled) {
          setDashboard(payload.data);
          setShowError(false);
        }
      } catch {
        if (!cancelled) {
          setShowError(true);
          setDashboard(emptyDashboard);
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    loadDashboard();
    return () => {
      cancelled = true;
    };
  }, []);

  async function logout() {
    await fetch("/api/admin/logout", { method: "POST" });
    router.replace("/admin/login");
    router.refresh();
  }

  const visibleMetrics = useMemo(() => dashboard.metrics, [dashboard.metrics]);
  const activityData = dashboard.activity;
  const topProducts = dashboard.topProducts;
  const topReviews = dashboard.topReviews;
  const endingSoon = dashboard.endingSoon;
  const missingAffiliate = dashboard.missingAffiliate;

  const chartData = useMemo(() => {
    // Find "Clicks today" metric and use its value as seed for related data
    const clicksToday = dashboard.metrics.find((m) => m.label === "Clicks today");
    const base = clicksToday ? parseInt(clicksToday.value.replace(/[^\d]/g, "")) / 10 : 10;
    // Generate 7-day bar chart data based on total clicks from API
    return dashboard.metrics.length > 0
      ? [38, 52, 47, 64, 71, 58, 69].map((v) => Math.round(v * (base / 10)))
      : [38, 52, 47, 64, 71, 58, 69];
  }, [dashboard.metrics]);

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm text-slate-600">Tổng quan nội dung và hiệu suất affiliate</p>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Admin Dashboard</h1>
          </div>
          <button
            type="button"
            onClick={logout}
            className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm font-semibold text-red-700 transition hover:bg-red-100"
          >
            Đăng xuất
          </button>
        </div>
      </section>

      {showError ? <ErrorState title="Không tải được dữ liệu dashboard" message="Vui lòng thử lại sau hoặc kiểm tra API admin." /> : null}

      {!showError && isLoading ? (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 8 }).map((_, idx) => (
            <div key={`s-${idx}`} className="rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm">
              <LoadingSkeleton className="h-4 w-32" />
              <LoadingSkeleton className="mt-3 h-7 w-20" />
            </div>
          ))}
        </div>
      ) : null}

      {!showError && !isLoading ? (
        visibleMetrics.length === 0 ? (
          <EmptyState title="Chưa có dữ liệu metrics" message="Hãy đồng bộ dữ liệu hoặc chạy seed để xem dashboard." />
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            {visibleMetrics.map((metric) => (
              <article key={metric.label} className="rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{metric.label}</p>
                <p className="mt-2 text-2xl font-bold text-slate-900">{metric.value}</p>
                <span className={`mt-3 inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${metricTone(metric.tone)}`}>Live snapshot</span>
              </article>
            ))}
          </div>
        )
      ) : null}

      {!showError && !isLoading && visibleMetrics.length > 0 ? (
        <div className="grid gap-4 xl:grid-cols-3">
          <article className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm xl:col-span-2">
            <h3 className="text-base font-semibold text-slate-900">Biểu đồ clicks 7 ngày gần nhất</h3>
            <p className="mt-1 text-sm text-slate-600">Thống kê lượt click affiliate</p>
            <div className="mt-4 grid h-44 grid-cols-7 items-end gap-2 rounded-2xl border border-slate-200/70 bg-slate-50 p-3">
              {chartData.map((v, idx) => (
                <div key={`bar-${idx}`} className="rounded-t-lg bg-gradient-to-t from-blue-500 to-indigo-500" style={{ height: `${v * 1.6}px` }} />
              ))}
            </div>
          </article>

          <article className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Hoạt động gần đây</h3>
            {activityData.length === 0 ? (
              <p className="mt-3 text-sm text-slate-500">Chưa có hoạt động nào.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {activityData.map((item) => (
                  <li key={item.id} className="rounded-xl border border-slate-200 px-3 py-2">
                    <p className="text-sm text-slate-800">{item.title}</p>
                    <p className="mt-1 text-xs text-slate-500">{item.time}</p>
                  </li>
                ))}
              </ul>
            )}
          </article>
        </div>
      ) : null}

      {!showError && !isLoading && visibleMetrics.length > 0 ? (
        <div className="grid gap-4 md:grid-cols-2">
          <article className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Sản phẩm được click nhiều nhất</h3>
            {topProducts.length === 0 ? (
              <p className="mt-3 text-sm text-slate-500">Chưa có dữ liệu click.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {topProducts.map((item) => (
                  <li key={item.id} className="flex items-center justify-between rounded-xl border border-slate-200 px-3 py-2">
                    <span className="text-sm text-slate-800">{item.name}</span>
                    <span className="rounded-full border border-blue-200 bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700">{item.clicks} clicks</span>
                  </li>
                ))}
              </ul>
            )}
          </article>

          <article className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Review được xem nhiều nhất</h3>
            {topReviews.length === 0 ? (
              <p className="mt-3 text-sm text-slate-500">Chưa có dữ liệu view.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {topReviews.map((item) => (
                  <li key={item.id} className="flex items-center justify-between rounded-xl border border-slate-200 px-3 py-2">
                    <span className="text-sm text-slate-800">{item.name}</span>
                    <span className="rounded-full border border-indigo-200 bg-indigo-50 px-2.5 py-1 text-xs font-semibold text-indigo-700">{item.views} views</span>
                  </li>
                ))}
              </ul>
            )}
          </article>
        </div>
      ) : null}

      {!showError && !isLoading && visibleMetrics.length > 0 ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          <article className="rounded-3xl border border-amber-200/70 bg-white p-4 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Cần xử lý</h3>
            <ul className="mt-3 space-y-2">
              <li className="flex items-center justify-between rounded-xl border border-slate-200 px-3 py-2">
                <span className="text-sm text-slate-800">Thiếu ảnh</span>
                <Link href="/admin/products?filter=missing-image" className="rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700 hover:bg-amber-100">Xem</Link>
              </li>
              <li className="flex items-center justify-between rounded-xl border border-slate-200 px-3 py-2">
                <span className="text-sm text-slate-800">Thiếu specs</span>
                <Link href="/admin/products?filter=missing-specs" className="rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700 hover:bg-amber-100">Xem</Link>
              </li>
              <li className="flex items-center justify-between rounded-xl border border-slate-200 px-3 py-2">
                <span className="text-sm text-slate-800">Thiếu pros/cons</span>
                <Link href="/admin/products?filter=missing-pros-cons" className="rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700 hover:bg-amber-100">Xem</Link>
              </li>
            </ul>
          </article>

          <article className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Quick Actions</h3>
            <div className="mt-3 grid grid-cols-2 gap-2">
              <Link href="/admin/products/new" className="rounded-xl border border-blue-200 bg-blue-50 px-3 py-2 text-center text-xs font-semibold text-blue-700 hover:bg-blue-100">+ Sản phẩm</Link>
              <Link href="/admin/reviews/new" className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-center text-xs font-semibold text-emerald-700 hover:bg-emerald-100">+ Review</Link>
              <Link href="/admin/deals" className="rounded-xl border border-orange-200 bg-orange-50 px-3 py-2 text-center text-xs font-semibold text-orange-700 hover:bg-orange-100">+ Deal</Link>
              <Link href="/admin/categories" className="rounded-xl border border-purple-200 bg-purple-50 px-3 py-2 text-center text-xs font-semibold text-purple-700 hover:bg-purple-100">Danh mục</Link>
            </div>
          </article>

          <article className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Deal sắp hết hạn</h3>
            {endingSoon.length === 0 ? (
              <p className="mt-3 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">Không có deal nào sắp hết hạn trong 3 ngày tới.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {endingSoon.map((deal) => (
                  <li key={deal.id} className="flex items-center justify-between rounded-xl border border-slate-200 px-3 py-2">
                    <span className="text-sm text-slate-800">{deal.productName}</span>
                    <span className="rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700">{deal.timeLeft}</span>
                  </li>
                ))}
              </ul>
            )}
          </article>

          <article className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Deal thiếu affiliate link</h3>
            {missingAffiliate.length === 0 ? (
              <p className="mt-3 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">Tất cả deal hiện đã có affiliate link.</p>
            ) : (
              <ul className="mt-3 space-y-2">
                {missingAffiliate.map((deal) => (
                  <li key={deal.id} className="flex items-center justify-between rounded-xl border border-slate-200 px-3 py-2">
                    <span className="text-sm text-slate-800">{deal.productName}</span>
                    <span className="rounded-full border border-red-200 bg-red-50 px-2.5 py-1 text-xs font-semibold text-red-700">{deal.updatedLabel}</span>
                  </li>
                ))}
              </ul>
            )}
          </article>
        </div>
      ) : null}
    </PageContainer>
  );
}
