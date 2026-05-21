type ProductFaq = {
  question: string;
  answer: string;
};

type FAQSectionProps = {
  faqs: ProductFaq[];
};

export function FAQSection({ faqs }: FAQSectionProps) {
  return (
    <section className="mt-8 rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
      <h2 className="text-2xl font-bold tracking-tight text-slate-900">Câu hỏi thường gặp</h2>

      <div className="mt-4 space-y-3">
        {faqs.slice(0, 5).map((faq, index) => (
          <details key={`${faq.question}-${index}`} className="group rounded-2xl border border-slate-200 bg-white p-4">
            <summary className="cursor-pointer list-none pr-6 text-sm font-semibold text-slate-900">
              {faq.question}
              <span className="float-right text-slate-500 transition group-open:rotate-45">+</span>
            </summary>
            <p className="mt-3 text-sm leading-6 text-slate-600">{faq.answer}</p>
          </details>
        ))}
      </div>
    </section>
  );
}
