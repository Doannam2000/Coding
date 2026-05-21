import Image from "next/image";
import Link from "next/link";
import { MiniProductCard } from "./mini-product-card";
import { ProductScoreBadge, ShopeeCTAButton } from "./ui";
import { cardStyles, buttonStyles } from "@/lib/design-system";

type MiniProduct = {
  name: string;
  price: string;
  href: string;
};

type FeaturedWorthProductCardProps = {
  title: string;
  score: number;
  imageSrc: string;
  imageAlt: string;
  discountLabel: string;
  productName: string;
  productHref: string;
  category: string;
  badge: "Đáng mua" | "Giá tốt" | "Best budget" | "Hot deal";
  pros: string[];
  cons: string[];
  currentPrice: string;
  oldPrice: string;
  shopeeHref: string;
  reviewHref: string;
  miniProducts: MiniProduct[];
};

export function FeaturedWorthProductCard({
  title,
  score,
  imageSrc,
  imageAlt,
  discountLabel,
  productName,
  productHref,
  category,
  badge,
  pros,
  cons,
  currentPrice,
  oldPrice,
  shopeeHref,
  reviewHref,
  miniProducts,
}: FeaturedWorthProductCardProps) {
  return (
    <div className={`${cardStyles.base} space-y-4 p-5 sm:p-6`}>
      <div className="flex items-center justify-between gap-2">
        <div className="min-w-0">
          <h2 className="truncate text-base font-semibold text-slate-900">{title}</h2>
          <p className="mt-1 text-xs font-medium text-slate-500">Danh mục: {category}</p>
        </div>
        <ProductScoreBadge score={score} />
      </div>

      <div className="relative overflow-hidden rounded-xl border border-slate-200/50 bg-slate-50">
        <Image
          src={imageSrc}
          alt={imageAlt}
          width={1200}
          height={800}
          className="h-44 w-full object-cover"
          priority
          placeholder="blur"
          blurDataURL="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTIwMCIgaGVpZ2h0PSI4MDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iI2YwZjBmMCIvPjwvc3ZnPg=="
        />
        <span className="absolute left-3 top-3 rounded-lg bg-emerald-500 px-3 py-1.5 text-xs font-bold text-white shadow-sm">
          {badge}
        </span>
        {discountLabel && (
          <span className="absolute right-3 top-3 rounded-lg bg-red-500 px-3 py-1.5 text-xs font-bold text-white shadow-sm">
            {discountLabel}
          </span>
        )}
      </div>

      <Link href={productHref} className="block text-lg font-semibold text-slate-900 transition-colors hover:text-blue-600">
        {productName}
      </Link>

      <div className="grid gap-3 sm:grid-cols-2">
        <div className="rounded-xl border border-emerald-200/70 bg-emerald-50/80 backdrop-blur-sm p-4">
          <p className="text-xs font-semibold text-emerald-800">Ưu điểm</p>
          <ul className="mt-2 space-y-1 text-sm text-emerald-900">
            {pros.slice(0, 3).map((item, idx) => (
              <li key={idx} className="leading-5">• {item}</li>
            ))}
          </ul>
        </div>
        <div className="rounded-xl border border-rose-200/70 bg-rose-50/80 backdrop-blur-sm p-4">
          <p className="text-xs font-semibold text-rose-800">Nhược điểm</p>
          <ul className="mt-2 space-y-1 text-sm text-rose-900">
            {cons.slice(0, 3).map((item, idx) => (
              <li key={idx} className="leading-5">• {item}</li>
            ))}
          </ul>
        </div>
      </div>

      <div className="flex flex-wrap items-end gap-2">
        <p className="text-xl font-bold text-slate-900 sm:text-2xl">{currentPrice}</p>
        {oldPrice && <p className="text-sm text-slate-400 line-through">{oldPrice}</p>}
      </div>

      <div className="flex flex-col gap-2 sm:flex-row">
        <ShopeeCTAButton href={shopeeHref} className="flex-1">Xem giá Shopee</ShopeeCTAButton>
        {reviewHref && (
          <Link
            href={reviewHref}
            className={`${buttonStyles.secondary} flex-1`}
            aria-label={`Đọc review ${productName}`}
          >
            Đọc review
          </Link>
        )}
      </div>

      {miniProducts.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-3">
          {miniProducts.map((item, idx) => (
            <MiniProductCard key={idx} name={item.name} price={item.price} href={item.href} />
          ))}
        </div>
      )}
    </div>
  );
}
