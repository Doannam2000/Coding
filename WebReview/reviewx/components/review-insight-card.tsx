type ReviewInsightCardProps = {
  insightPositive: string[];
  insightNegative: string[];
  insightKeywords: string[];
  sentimentScore: string;
  insightVerified: boolean;
};

export function ReviewInsightCard({ insightPositive, insightNegative, insightKeywords, sentimentScore, insightVerified }: ReviewInsightCardProps) {
  return (
    <section className="mt-8 rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
      <h2 className="text-2xl font-bold tracking-tight text-slate-900">Người dùng nói gì?</h2>

      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <article className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
          <h3 className="text-sm font-semibold text-emerald-800">Điểm được khen nhiều</h3>
          <ul className="mt-3 space-y-2 text-sm leading-6 text-emerald-900">
            {insightPositive.map((item) => (
              <li key={item} className="flex gap-2">
                <span>+</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </article>

        <article className="rounded-2xl border border-amber-200 bg-amber-50 p-4">
          <h3 className="text-sm font-semibold text-amber-800">Điểm bị phàn nàn</h3>
          <ul className="mt-3 space-y-2 text-sm leading-6 text-amber-900">
            {insightNegative.map((item) => (
              <li key={item} className="flex gap-2">
                <span>-</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </article>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-2">
        {insightKeywords.map((keyword) => (
          <span key={keyword} className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-700">
            {keyword}
          </span>
        ))}
        <span className="rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">Sentiment: {sentimentScore}</span>
      </div>

      <p className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-xs leading-5 text-slate-600">
        Dữ liệu được tổng hợp từ nguồn công khai và kiểm duyệt biên tập. Không hiển thị nguyên văn đánh giá người dùng.
      </p>

      {!insightVerified ? <p className="mt-3 text-xs text-amber-700">Insight hiện ở mức tham khảo và chưa đại diện cho kiểm thử hands-on.</p> : null}
    </section>
  );
}
