const trustFeatures = [
  {
    title: "Dữ liệu thật từ Shopee",
    description: "Dữ liệu cập nhật theo thị trường giúp bạn ra quyết định nhanh hơn.",
    icon: "📦",
  },
  {
    title: "Phân tích khách quan",
    description: "Tổng hợp ưu nhược điểm rõ ràng, không thiên vị quảng cáo.",
    icon: "⚖️",
  },
  {
    title: "Gợi ý đáng tin cậy",
    description: "Đề xuất theo nhu cầu thực tế với tiêu chí dễ kiểm chứng.",
    icon: "✅",
  },
  {
    title: "Tiết kiệm thời gian",
    description: "Giảm thời gian tìm hiểu bằng verdict và so sánh nhanh.",
    icon: "⏱️",
  },
  {
    title: "Affiliate minh bạch",
    description: "Liên kết affiliate luôn được công khai để đảm bảo minh bạch.",
    icon: "🔎",
  },
];

export function TrustFeatureStrip() {
  return (
    <section className="mt-10 rounded-3xl border border-slate-200/70 bg-white p-5 shadow-sm sm:p-6">
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        {trustFeatures.map((item) => (
          <article key={item.title} className="rounded-2xl border border-slate-200/70 bg-[rgba(255,255,255,.86)] p-4 shadow-sm">
            <div className="mb-2 inline-flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-base">{item.icon}</div>
            <h3 className="text-sm font-semibold text-slate-900">{item.title}</h3>
            <p className="mt-1 text-xs leading-5 text-slate-600">{item.description}</p>
          </article>
        ))}
      </div>
    </section>
  );
}
