import Link from "next/link";
import { DealRow } from "./deal-row";
import { EmptyState, LoadingSkeleton } from "./ui";

type DealItem = {
  id: string;
  name: string;
  discount: string;
  currentPrice: string;
  oldPrice: string;
  thumb: string;
  active: boolean;
  platform: string;
  hasAffiliate: boolean;
  hasReview: boolean;
  reviewHref?: string | null;
};

type HotDealPanelProps = {
  deals?: DealItem[];
  isLoading?: boolean;
  error?: string;
};



export function HotDealPanel({ deals: propDeals, isLoading, error }: HotDealPanelProps) {
  const deals = propDeals ?? [];

  return (
    <section className="mt-10 rounded-3xl border border-slate-200/70 bg-gradient-to-r from-orange-50 via-pink-50 to-blue-50 p-5 shadow-sm sm:p-6">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-2xl font-bold tracking-tight text-slate-900">Deal hot</h2>
        <span className="text-xs text-slate-500">Cập nhật: {new Date().toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" })}</span>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, idx) => (
            <div key={`dl-${idx}`} className="flex gap-3 rounded-2xl border border-white/80 bg-white/90 p-3">
              <LoadingSkeleton className="h-14 w-14 rounded-xl" />
              <div className="flex-1 space-y-2">
                <LoadingSkeleton className="h-4 w-32" />
                <LoadingSkeleton className="h-3 w-24" />
              </div>
            </div>
          ))}
        </div>
      ) : error ? (
        <EmptyState title="Không tải được deals" message={error} />
      ) : deals.length === 0 ? (
        <EmptyState title="Chưa có deal active" message="Quay lại sau để xem các deal hot mới nhất." />
      ) : (
        <div className="space-y-3">
          {deals.map((deal) => (
            <DealRow
              key={deal.id}
              id={deal.id}
              name={deal.name}
              discount={deal.discount}
              currentPrice={deal.currentPrice}
              oldPrice={deal.oldPrice}
              thumb={deal.thumb}
              active={deal.active}
              platform={deal.platform}
              hasAffiliate={deal.hasAffiliate}
              hasReview={deal.hasReview}
              reviewHref={deal.reviewHref ?? undefined}
            />
          ))}
        </div>
      )}

      <div className="mt-4 flex justify-end">
        <Link
          href="/deals"
          className="inline-flex items-center justify-center rounded-2xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
        >
          Xem tất cả
        </Link>
      </div>
    </section>
  );
}