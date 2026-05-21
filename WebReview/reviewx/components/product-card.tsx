import Image from "next/image";
import Link from "next/link";
import { ProductScoreBadge, ShopeeCTAButton } from "./ui";
import { cardStyles, buttonStyles } from "@/lib/design-system";

type ProductCardProps = {
  id: string;
  name: string;
  image: string;
  brand: string;
  useCase: string;
  priceLabel: string;
  score: number;
  shopeeMall: boolean;
  reviewHref?: string;
};

export function ProductCard({ id, name, image, brand, useCase, priceLabel, score, shopeeMall, reviewHref }: ProductCardProps) {
  return (
    <article className={`group flex h-full flex-col ${cardStyles.interactive} p-4`}>
      <Link href={`/san-pham/${id}`} className="block overflow-hidden rounded-xl border border-slate-200/50 bg-slate-50">
        <Image
          src={image}
          alt={name}
          width={800}
          height={600}
          className="h-44 w-full object-cover transition-transform duration-300 group-hover:scale-105"
          loading="lazy"
          placeholder="blur"
          blurDataURL="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAwIiBoZWlnaHQ9IjYwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjBmMGYwIi8+PC9zdmc+"
        />
      </Link>

      <div className="mt-3 flex items-center justify-between gap-2">
        <span className="rounded-full border border-slate-200/70 bg-slate-50/80 px-2.5 py-1 text-xs font-semibold text-slate-700">{brand}</span>
        <ProductScoreBadge score={score} />
      </div>

      <Link href={`/san-pham/${id}`} className="mt-3 line-clamp-2 block text-sm font-semibold leading-5 text-slate-900 transition-colors hover:text-blue-600">
        {name}
      </Link>

      <p className="mt-2 text-xs text-slate-500">Nhu cầu: {useCase}</p>
      <p className="mt-1 text-lg font-bold text-slate-900">{priceLabel}</p>

      <div className="mt-3 flex flex-wrap items-center gap-2">
        {shopeeMall ? (
          <span className="rounded-full border border-blue-200/70 bg-blue-50/80 px-2.5 py-1 text-xs font-semibold text-blue-700">Shopee Mall</span>
        ) : (
          <span className="rounded-full border border-slate-200/70 bg-slate-50/80 px-2.5 py-1 text-xs font-semibold text-slate-600">Shop thường</span>
        )}
      </div>

      <div className="mt-auto flex gap-2 pt-4">
        <ShopeeCTAButton href={`/recommends/${id}`} className="flex-1">Xem giá</ShopeeCTAButton>
        <Link
          href={reviewHref ?? `/san-pham/${id}`}
          className={`${buttonStyles.secondary} flex-1 px-3 py-2 text-xs`}
          aria-label={`Đọc review ${name}`}
        >
          Review
        </Link>
      </div>
    </article>
  );
}
