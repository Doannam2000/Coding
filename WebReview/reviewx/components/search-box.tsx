"use client";

type SearchBoxProps = {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  placeholder?: string;
  isLoading?: boolean;
  suggestions?: string[];
  onSuggestionClick?: (suggestion: string) => void;
  recentSearches?: string[];
  onRecentClear?: () => void;
  popularSearches?: string[];
  onPopularClick?: (term: string) => void;
  showRecent?: boolean;
  showPopular?: boolean;
};

export function SearchBox({
  value,
  onChange,
  onSubmit,
  placeholder = "Bạn đang tìm gì?",
  isLoading = false,
  suggestions = [],
  onSuggestionClick,
  recentSearches = [],
  onRecentClear,
  popularSearches = [],
  onPopularClick,
  showRecent = false,
  showPopular = true,
}: SearchBoxProps) {
  return (
    <div className="space-y-3">
      <form
        onSubmit={(e) => { e.preventDefault(); onSubmit(); }}
        className="flex flex-col gap-3 sm:flex-row"
      >
        <input
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className="h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-base transition focus-visible:border-blue-400 focus-visible:ring-2 focus-visible:ring-blue-100"
        />
        <button
          type="submit"
          className="inline-flex h-12 items-center justify-center rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 px-5 text-sm font-semibold text-white"
        >
          {isLoading ? "Đang tìm..." : "Tìm kiếm"}
        </button>
      </form>

      {suggestions.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {suggestions.map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => onSuggestionClick?.(s)}
              className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:bg-slate-100 focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-1"
            >
              {s}
            </button>
          ))}
        </div>
      )}

      {showPopular && popularSearches.length > 0 && (
        <div className="flex flex-wrap gap-2 text-xs">
          <span className="font-semibold text-slate-600">Tìm kiếm phổ biến:</span>
          {popularSearches.map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => onPopularClick?.(s)}
              className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 font-medium text-slate-700 transition hover:bg-slate-200 focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-1"
            >
              {s}
            </button>
          ))}
        </div>
      )}

      {showRecent && recentSearches.length > 0 && (
        <div className="space-y-2 text-xs">
          <div className="flex items-center justify-between gap-2">
            <span className="font-semibold text-slate-600">Gần đây:</span>
            <button
              type="button"
              onClick={onRecentClear}
              className="rounded-lg border border-slate-200 bg-white px-2 py-1 font-medium text-slate-700 transition hover:bg-slate-100 focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-1"
            >
              Xóa gần đây
            </button>
          </div>
          <div className="flex flex-wrap gap-2">
            {recentSearches.map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => onSuggestionClick?.(s)}
                className="rounded-full border border-slate-200 bg-white px-3 py-1.5 font-medium text-slate-700 transition hover:bg-slate-100 focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-1"
              >
                {s}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
