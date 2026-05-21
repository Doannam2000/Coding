import { Badge } from "./ui";

type ProsConsGridProps = {
  pros: string[];
  cons: string[];
};

export function ProsConsGrid({ pros, cons }: ProsConsGridProps) {
  return (
    <section className="mt-8 rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8" aria-label="Ưu và nhược điểm">
      <div className="grid gap-6 md:grid-cols-2">
        <article className="rounded-2xl border border-emerald-200 bg-emerald-50/80 p-4">
          <Badge tone="success" className="mb-3 text-base">Ưu điểm</Badge>
          {pros.length > 0 ? (
            <ul className="mt-3 space-y-2 text-sm leading-relaxed text-emerald-900">
              {pros.map((item) => (
                <li key={item} className="flex gap-2">
                  <span className="text-emerald-600" aria-hidden="true">▸</span>
                  <span className="leading-relaxed">{item}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-3 text-sm text-emerald-700">Chưa có dữ liệu ưu điểm cho sản phẩm này.</p>
          )}
        </article>

        <article className="rounded-2xl border border-amber-200 bg-amber-50/80 p-4">
          <Badge tone="warning" className="mb-3 text-base">Nhược điểm</Badge>
          {cons.length > 0 ? (
            <ul className="mt-3 space-y-2 text-sm leading-relaxed text-amber-900">
              {cons.map((item) => (
                <li key={item} className="flex gap-2">
                  <span className="text-amber-600" aria-hidden="true">▸</span>
                  <span className="leading-relaxed">{item}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-3 text-sm text-amber-700">Chưa có dữ liệu nhược điểm cho sản phẩm này.</p>
          )}
        </article>
      </div>
    </section>
  );
}
