import { ShopeeCTAButton } from "@/components/ui";

type StickyMobileCTAProps = {
  priceRange?: string;
  worthScore: number;
  productId: string;
  affiliateActive: boolean;
  hasAffiliateLink: boolean;
};

export function StickyMobileCTA({ priceRange, worthScore, productId, affiliateActive, hasAffiliateLink }: StickyMobileCTAProps) {
  return (
    <div className="fixed inset-x-0 bottom-0 z-40 border-t border-slate-200/80 bg-white/95 p-3 backdrop-blur md:hidden" style={{ paddingBottom: "max(0.75rem, env(safe-area-inset-bottom))" }}>
      <div className="mx-auto flex w-full max-w-[1360px] items-center gap-3">
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold text-slate-900">{priceRange ?? "Chưa có giá"}</p>
          <p className="text-xs text-slate-600">Điểm: {worthScore.toFixed(1)} / 10</p>
        </div>

        {affiliateActive && hasAffiliateLink ? (
          <ShopeeCTAButton href={`/recommends/${productId}`}>Xem giá Shopee</ShopeeCTAButton>
        ) : (
          <span className="inline-flex items-center justify-center rounded-2xl border border-amber-300 bg-amber-50 px-4 py-2 text-xs font-semibold text-amber-700">
            Link tạm hết hạn
          </span>
        )}
      </div>
    </div>
  );
}
