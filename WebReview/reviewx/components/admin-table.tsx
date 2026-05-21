"use client";

type Column = {
  key: string;
  label: string;
  render?: (row: Record<string, unknown>) => React.ReactNode;
  className?: string;
};

type AdminTableProps = {
  columns: Column[];
  rows: Record<string, unknown>[];
  loading?: boolean;
  emptyMessage?: string;
  errorMessage?: string;
  onRetry?: () => void;
  minWidth?: string;
};

export function AdminTable({
  columns,
  rows,
  loading = false,
  emptyMessage = "Không có dữ liệu.",
  errorMessage,
  onRetry,
  minWidth = "900px",
}: AdminTableProps) {
  if (errorMessage && onRetry) {
    return (
      <div className="mt-4">
        <div className="rounded-2xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          <p>{errorMessage}</p>
          <button
            type="button"
            onClick={onRetry}
            className="mt-1 rounded-full border border-red-200 bg-white px-3 py-1 text-xs font-semibold text-red-700"
          >
            Thử lại
          </button>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="mt-4 grid gap-2">
        <div className="h-12 w-full rounded-xl bg-slate-100" />
        <div className="h-12 w-full rounded-xl bg-slate-100" />
      </div>
    );
  }

  if (rows.length === 0) {
    return (
      <div className="mt-4">
        <p className="text-sm text-slate-500">{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div className="mt-4 overflow-x-auto">
      <table className={`w-full min-w-[${minWidth}] text-left text-sm`}>
        <thead>
          <tr className="border-b border-slate-200 text-slate-600">
            {columns.map((col) => (
              <th key={col.key} className="px-2 py-2 font-semibold">
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIdx) => (
            <tr key={rowIdx} className="border-b border-slate-100 transition hover:bg-slate-50">
              {columns.map((col) => (
                <td key={col.key} className={`px-2 py-2 text-slate-700 ${col.className || ""}`}>
                  {col.render ? col.render(row) : (row[col.key] as React.ReactNode)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function StatusBadge({ status }: { status: string }) {
  const cls =
    status === "PUBLISHED" || status === "Active"
      ? "border-emerald-200 bg-emerald-50 text-emerald-700"
      : status === "ARCHIVED" || status === "Expired"
      ? "border-slate-300 bg-slate-100 text-slate-700"
      : "border-amber-200 bg-amber-50 text-amber-700";
  return <span className={`rounded-full border px-2 py-0.5 text-xs font-semibold ${cls}`}>{status}</span>;
}