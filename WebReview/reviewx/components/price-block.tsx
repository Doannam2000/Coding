import { Badge } from "./ui";

type PriceBlockProps = {
  currentPrice: string;
  oldPrice?: string;
  discountLabel?: string;
  discountPercent?: number;
  currency?: string;
  className?: string;
};

export function PriceBlock({
  currentPrice,
  oldPrice,
  discountLabel,
  discountPercent,
  className = "",
}: PriceBlockProps) {
  const badgeLabel = discountLabel ?? (discountPercent ? `-${discountPercent}%` : null);

  return (
    <div
      className={`flex flex-wrap items-baseline gap-x-2 gap-y-1 ${className}`}
      aria-label={`Giá: ${currentPrice}${oldPrice ? `, giá cũ ${oldPrice}` : ""}${badgeLabel ? `, giảm ${badgeLabel}` : ""}`}
    >
      <span className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
        {currentPrice}
      </span>

      {oldPrice ? (
        <span className="text-sm font-normal text-slate-400 line-through sm:text-base" aria-label={`Giá gốc: ${oldPrice}`}>
          {oldPrice}
        </span>
      ) : null}

      {badgeLabel ? (
        <Badge tone="danger" className="transition hover:brightness-105" aria-label={`Giảm giá: ${badgeLabel}`}>
          {badgeLabel}
        </Badge>
      ) : null}
    </div>
  );
}