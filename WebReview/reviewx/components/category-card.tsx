import Link from "next/link";
import Image from "next/image";

type CategoryCardProps = {
  id: string;
  slug: string;
  name: string;
  icon?: string | null;
  image?: string | null;
  description?: string | null;
  productCount: number;
  reviewCount: number;
  dealCount: number;
  isFeatured?: boolean;
};

const FALLBACK_ICONS = ["🧩", "📦", "🔧", "💡", "🎯", "🏅", "🛡️", "🚀", "🎨", "📊"];

export function CategoryCard({
  id,
  slug,
  name,
  icon,
  image,
  description,
  productCount,
  reviewCount,
  dealCount,
  isFeatured,
}: CategoryCardProps) {
  const displayIcon = icon || FALLBACK_ICONS[parseInt(id, 36) % FALLBACK_ICONS.length];

  return (
    <Link
      href={`/danh-muc/${slug}`}
      className={`group flex h-full rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-md ${
        isFeatured ? "border-blue-300 bg-blue-50/50" : ""
      }`}
    >
      <div className="flex w-full flex-col items-center gap-3 text-center">
        <span className="relative flex h-16 w-16 items-center justify-center overflow-hidden rounded-2xl border border-slate-200 bg-slate-50 text-3xl transition group-hover:scale-110">
          {image ? (
            <Image
              src={image}
              alt={name}
              width={64}
              height={64}
              className="h-full w-full object-cover"
              loading="lazy"
              unoptimized
            />
          ) : (
            displayIcon
          )}
        </span>
        <h3 className="line-clamp-2 text-base font-semibold text-slate-900 group-hover:text-blue-600">{name}</h3>
        {description ? <p className="line-clamp-2 text-xs text-slate-500">{description}</p> : null}

        <div className="flex flex-wrap items-center justify-center gap-3 pt-1 text-xs">
          <span className="rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-slate-600">{productCount} san pham</span>
          <span className="rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-slate-600">{reviewCount} review</span>
          <span className="rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-slate-600">{dealCount} deal</span>
        </div>

        <span className="mt-1 inline-flex items-center text-xs font-medium text-blue-600 opacity-0 transition group-hover:opacity-100">
          Xem danh muc →
        </span>
      </div>
    </Link>
  );
}
