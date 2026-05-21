import { TopCategoryCard } from "./top-category-card";

const categories = [
  { label: "Công nghệ", slug: "cong-nghe", icon: "💻" },
  { label: "Gia dụng", slug: "gia-dung", icon: "🏠" },
  { label: "Làm đẹp", slug: "lam-dep", icon: "✨" },
  { label: "Mẹ & bé", slug: "me-va-be", icon: "🍼" },
  { label: "Nhà cửa", slug: "nha-cua", icon: "🧰" },
  { label: "Đồ bếp", slug: "do-bep", icon: "🍳" },
  { label: "Gaming", slug: "gaming", icon: "🎮" },
  { label: "Thể thao", slug: "the-thao", icon: "🏃" },
  { label: "Sách", slug: "sach", icon: "📚" },
  { label: "Xem thêm", slug: "xem-them", icon: "➕" },
];

export function CategoryStrip() {
  return (
    <section className="mt-10 rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6">
      <div className="flex gap-3 overflow-x-auto pb-1">
        {categories.map((item, idx) => (
          <TopCategoryCard key={item.slug} label={item.label} slug={item.slug} icon={item.icon} active={idx === 0} />
        ))}
      </div>
    </section>
  );
}
