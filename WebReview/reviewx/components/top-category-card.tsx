import Link from "next/link";

type TopCategoryCardProps = {
  label: string;
  slug: string;
  icon: string;
  active?: boolean;
};

export function TopCategoryCard({ label, slug, icon, active = false }: TopCategoryCardProps) {
  const href = slug === "xem-them" ? "/danh-muc" : `/danh-muc/${slug}`;

  return (
    <Link
      href={href}
      className={`shrink-0 rounded-2xl border border-slate-200/70 bg-white px-4 py-3 text-center transition hover:-translate-y-0.5 hover:shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${active ? "bg-slate-50" : ""}`}
    >
      <span className="mx-auto mb-1 flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-base">{icon}</span>
      <p className="text-xs font-medium text-slate-700">{label}</p>
    </Link>
  );
}
