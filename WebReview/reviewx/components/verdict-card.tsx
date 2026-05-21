type VerdictCardProps = {
  verdictText: string;
  buyIf: string;
  considerIf: string;
  avoidIf: string;
  buyPriceHint: string;
  considerPriceHint: string;
};

export function VerdictCard({ verdictText, buyIf, considerIf, avoidIf, buyPriceHint, considerPriceHint }: VerdictCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200/70 bg-[rgba(255,255,255,.86)] p-4">
      <p className="text-sm font-semibold text-slate-900">Có đáng mua không?</p>
      <p className="mt-2 text-sm leading-6 text-slate-600">{verdictText}</p>

      <div className="mt-3 space-y-2 text-sm">
        <p className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-emerald-700">✅ Nên mua nếu: {buyIf}</p>
        <p className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-amber-700">⚠️ Cân nhắc nếu: {considerIf}</p>
        <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-red-700">⛔ Không phù hợp nếu: {avoidIf}</p>
      </div>

      <div className="mt-3 space-y-1 text-sm text-slate-700">
        <p>{buyPriceHint}</p>
        <p>{considerPriceHint}</p>
      </div>
    </div>
  );
}
