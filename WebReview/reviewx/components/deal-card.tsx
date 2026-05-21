import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { ShopeeCTAButton } from "./ui";
import { cardStyles, buttonStyles, badgeStyles } from "@/lib/design-system";

type DealCardProps = {
  id: string;
  productName: string;
  image: string;
  category: string;
  platform: string;
  discountPercent: number;
  currentPriceLabel: string;
  oldPriceLabel: string;
  expiryLabel: string;
  endTime: string;
  isExpired: boolean;
  hasAffiliate: boolean;
  hasCoupon?: boolean;
  couponCode?: string;
};

export function DealCard({
  id,
  productName,
  image,
  category,
  platform,
  discountPercent,
  currentPriceLabel,
  oldPriceLabel,
  expiryLabel,
  endTime,
  isExpired,
  hasAffiliate,
  hasCoupon,
  couponCode,
}: DealCardProps) {
  const [copied, setCopied] = useState(false);
  const [imageError, setImageError] = useState(false);
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (isExpired) return;
    const interval = setInterval(() => {
      setNow(Date.now());
    }, 1000);
    return () => clearInterval(interval);
  }, [isExpired]);

  const countdownLabel = useMemo(() => {
    if (isExpired) return "Deal đã hết hạn";
    const target = new Date(endTime).getTime();
    const diff = target - now;
    if (!Number.isFinite(target) || diff <= 0) return "Deal đã hết hạn";

    const totalSeconds = Math.floor(diff / 1000);
    const days = Math.floor(totalSeconds / 86400);
    const hours = Math.floor((totalSeconds % 86400) / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    if (days > 0) {
      return `Còn ${days}d ${hours}h ${minutes}m`;
    }

    return `Còn ${hours}h ${minutes}m ${seconds}s`;
  }, [endTime, isExpired, now]);

  const expiryText = isExpired ? expiryLabel : countdownLabel;
  const imageSrc = imageError
    ? "https://images.unsplash.com/photo-1484704849700-f032a568e944?q=80&w=1200&auto=format&fit=crop"
    : image;
  const hasValidAffiliate = hasAffiliate && id.trim().length > 0;

  async function handleCopyCoupon() {
    if (!couponCode) return;
    try {
      await navigator.clipboard.writeText(couponCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 1200);
    } catch {
      setCopied(false);
    }
  }

  return (
    <article className={`group ${cardStyles.interactive} p-4`}>
      <div className="relative overflow-hidden rounded-xl border border-slate-200/50 bg-slate-50">
        <Image
          src={imageSrc}
          alt={productName}
          width={800}
          height={600}
          className="h-40 w-full object-cover transition-transform duration-300 group-hover:scale-105"
          onError={() => setImageError(true)}
          loading="lazy"
          placeholder="blur"
          blurDataURL="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjBmMGYwIi8+PC9zdmc+"
        />
        {!isExpired ? (
          <span className="absolute left-2 top-2 rounded-lg bg-red-500 px-2.5 py-1 text-xs font-bold text-white shadow-sm">
            -{discountPercent}%
          </span>
        ) : (
          <span className="absolute left-2 top-2 rounded-lg bg-slate-500 px-2.5 py-1 text-xs font-bold text-white shadow-sm">
            Đã hết hạn
          </span>
        )}
        <span className="absolute right-2 top-2 rounded-lg border border-emerald-200/70 bg-emerald-50/90 backdrop-blur-sm px-2.5 py-1 text-xs font-semibold text-emerald-700 shadow-sm">
          Đã xác minh
        </span>
      </div>

      <div className="mt-3 space-y-3">
        <div className="flex items-center justify-between gap-2">
          <span className={`${badgeStyles.neutral} text-xs`}>{category}</span>
          <span className="rounded-full border border-orange-200/70 bg-orange-50/80 px-2.5 py-1 text-xs font-semibold text-orange-700">
            {platform}
          </span>
        </div>

        <h3 className="line-clamp-2 break-words text-sm font-semibold leading-5 text-slate-900">
          {productName}
        </h3>

        <div className="flex items-end gap-2">
          <p className="text-lg font-bold text-slate-900">{currentPriceLabel}</p>
          <p className="text-sm text-slate-500 line-through">{oldPriceLabel}</p>
        </div>

        {hasCoupon && couponCode ? (
          <div className="flex items-center gap-2 rounded-lg border border-amber-200/70 bg-amber-50/80 p-2">
            <span className="text-xs font-semibold text-amber-700">Mã:</span>
            <code className="flex-1 text-xs font-bold text-amber-900">{couponCode}</code>
            <button
              type="button"
              onClick={handleCopyCoupon}
              className={`${buttonStyles.secondary} px-2 py-1 text-xs`}
              aria-label={`Copy mã giảm giá ${couponCode}`}
            >
              {copied ? "✓ Đã copy" : "Copy"}
            </button>
          </div>
        ) : null}

        <p className={`text-xs font-semibold ${isExpired ? "text-slate-500" : "text-emerald-600"}`}>
          <time dateTime={endTime}>{expiryText}</time>
        </p>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {isExpired ? (
          <span className={`${badgeStyles.danger} flex-1 justify-center py-2.5`}>
            Deal đã hết hạn
          </span>
        ) : hasValidAffiliate ? (
          <ShopeeCTAButton href={`/go/deal/${id}`} className="flex-1">Xem deal</ShopeeCTAButton>
        ) : (
          <span className={`${badgeStyles.warning} flex-1 justify-center py-2.5`}>
            Link không hợp lệ
          </span>
        )}

        <Link
          href={hasValidAffiliate ? `/go/deal/${id}` : "/link-error"}
          className={`${buttonStyles.secondary} flex-1 px-3 py-2 text-xs`}
          aria-label={hasValidAffiliate ? `Xem chi tiết deal ${productName}` : "Báo lỗi link"}
        >
          {hasValidAffiliate ? "Chi tiết" : "Báo lỗi"}
        </Link>
      </div>
    </article>
  );
}
