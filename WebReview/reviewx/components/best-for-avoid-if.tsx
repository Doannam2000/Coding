type BestForAvoidIfProps = {
  bestFor: string[];
  avoidFor: string[];
};

export function BestForAvoidIf({ bestFor, avoidFor }: BestForAvoidIfProps) {
  return (
    <section className="mt-8 rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
      <div className="grid gap-4 md:grid-cols-2">
        <article className="rounded-2xl border border-blue-200 bg-blue-50 p-4">
          <h2 className="text-lg font-semibold text-blue-800">Phù hợp với ai</h2>
          {bestFor.length > 0 ? (
            <ul className="mt-3 space-y-2 text-sm leading-6 text-blue-900">
              {bestFor.map((item) => (
                <li key={item} className="flex gap-2">
                  <span>✅</span>
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-3 text-sm text-blue-700">Chưa có gợi ý đối tượng phù hợp cho sản phẩm này.</p>
          )}
        </article>

        <article className="rounded-2xl border border-rose-200 bg-rose-50 p-4">
          <h2 className="text-lg font-semibold text-rose-800">Không phù hợp với ai</h2>
          {avoidFor.length > 0 ? (
            <ul className="mt-3 space-y-2 text-sm leading-6 text-rose-900">
              {avoidFor.map((item) => (
                <li key={item} className="flex gap-2">
                  <span>⛔</span>
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-3 text-sm text-rose-700">Chưa có cảnh báo đối tượng cần tránh cho sản phẩm này.</p>
          )}
        </article>
      </div>
    </section>
  );
}
