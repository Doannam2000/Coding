"use client";

type FilterSidebarProps = {
  filters: {
    category: string;
    priceRange: string;
    minScore: string;
    platform: string;
    discount: string;
  };
  onChange: (key: string, value: string) => void;
  onReset: () => void;
  activeFilters: string[];
  showFilters: boolean;
  mobileOpen?: boolean;
  categories?: string[];
  platforms?: string[];
  discountOptions?: string[];
  sortBy?: string;
  onSortChange?: (value: string) => void;
  sortOptions?: Array<{ value: string; label: string }>;
};

export function FilterSidebar({
  filters,
  onChange,
  onReset,
  activeFilters,
  showFilters,
  mobileOpen = true,
  categories = ["Công nghệ", "Gia dụng", "Làm đẹp", "Gaming"],
  platforms = ["Shopee", "Lazada", "Tiki", "Other"],
  discountOptions = ["10%+", "20%+", "30%+", "50%+"],
  sortBy = "relevance",
  onSortChange,
  sortOptions = [
    { value: "relevance", label: "Sắp xếp: Liên quan" },
    { value: "newest", label: "Sắp xếp: Mới nhất" },
    { value: "price", label: "Sắp xếp: Giá" },
    { value: "score", label: "Sắp xếp: Điểm" },
  ],
}: FilterSidebarProps) {
  if (!showFilters) return null;

  return (
    <div className={`space-y-3 rounded-2xl border border-slate-200 bg-slate-50 p-3 ${mobileOpen ? "block" : "hidden sm:block"}`}>
      <div className="flex items-center justify-between">
        <p className="text-sm font-semibold text-slate-800">Bộ lọc & sắp xếp</p>
        <button
          type="button"
          onClick={onReset}
          className="rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-semibold text-slate-700"
        >
          Xóa lọc
        </button>
      </div>

      <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        <select
          value={filters.category}
          onChange={(e) => onChange("category", e.target.value)}
          className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
        >
          <option value="">Danh mục</option>
          {categories.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>

        <select
          value={filters.priceRange}
          onChange={(e) => onChange("priceRange", e.target.value)}
          className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
        >
          <option value="">Khoảng giá</option>
          <option value="under-500k">&lt; 500k</option>
          <option value="500k-2m">500k - 2m</option>
          <option value="over-2m">&gt; 2m</option>
        </select>

        <select
          value={filters.minScore}
          onChange={(e) => onChange("minScore", e.target.value)}
          className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
        >
          <option value="">Điểm tối thiểu</option>
          <option value="6">6+</option>
          <option value="7">7+</option>
          <option value="8">8+</option>
          <option value="9">9+</option>
        </select>

        <select
          value={filters.platform}
          onChange={(e) => onChange("platform", e.target.value)}
          className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
        >
          <option value="">Nền tảng</option>
          {platforms.map((p) => (
            <option key={p} value={p}>
              {p}
            </option>
          ))}
        </select>

        <select
          value={filters.discount}
          onChange={(e) => onChange("discount", e.target.value)}
          className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
        >
          <option value="">Mức giảm</option>
          {discountOptions.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </select>

        {sortOptions && onSortChange && (
          <select
            value={sortBy}
            onChange={(e) => onSortChange(e.target.value)}
            className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
          >
            {sortOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        )}
      </div>

      {activeFilters.length > 0 && (
        <div className="flex flex-wrap items-center gap-2">
          {activeFilters.map((f, i) => (
            <span key={`${f}-${i}`} className="rounded-full border border-blue-200 bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700">
              {f}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}