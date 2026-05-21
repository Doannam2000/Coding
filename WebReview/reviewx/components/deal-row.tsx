import Image from "next/image";
import Link from "next/link";

type DealRowProps = {
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
  reviewHref?: string;
};

export function DealRow({ id, name, discount, currentPrice, oldPrice, thumb, active, platform, hasAffiliate, hasReview, reviewHref }: DealRowProps) {
  return (
    <div className="flex flex-col gap-3 rounded-2xl border border-white/80 bg-white/90 p-3 shadow-sm sm:flex-row sm:items-center sm:justify-between">
      <div className="flex min-w-0 items-center gap-3">
        <div className="relative h-14 w-14 overflow-hidden rounded-xl border border-slate-200 bg-slate-100">
          <Image src={thumb} alt={name} fill className="object-cover" />
        </div>
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-slate-900">{name}</p>
          <div className="mt-1 flex flex-wrap items-center gap-2 text-xs">
            <span className="rounded-full bg-blue-50 px-2 py-0.5 font-semibold text-blue-700">{platform}</span>
            <span className="rounded-full bg-red-500 px-2 py-0.5 font-semibold text-white">{discount}</span>
            <span className="font-semibold text-slate-900">{currentPrice}</span>
            <span className="text-slate-400 line-through">{oldPrice}</span>
          </div>
        </div>
      </div>

      <div className="mt-2 flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
        {hasAffiliate && active ? (
          <Link
            href={`/go/deal/${id}`}
            className="inline-flex items-center justify-center rounded-xl bg-orange-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-orange-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-orange-500"
          >
            Xem deal
          </Link>
        ) : !hasAffiliate ? (
          <span className="inline-flex items-center justify-center rounded-xl border border-amber-300 bg-amber-50 px-4 py-2 text-sm font-semibold text-amber-700">
            Thiếu link affiliate
          </span>
        ) : !active ? (
          <span className="inline-flex items-center justify-center rounded-xl border border-amber-300 bg-amber-50 px-4 py-2 text-sm font-semibold text-amber-700">
            Deal hết hạn
          </span>
        ) : null}

        {hasReview && reviewHref ? (
          <Link
            href={reviewHref}
            className="inline-flex items-center justify-center rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
          >
            Đọc review
          </Link>
        ) : null}
      </div>
    </div>
  );
}
