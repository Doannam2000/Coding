import { PageContainer } from "@/components/ui";

export default function AboutPage() {
  return (
    <PageContainer>
      <article className="mx-auto max-w-4xl rounded-3xl border border-slate-200/70 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="text-3xl font-bold text-slate-900">About ReviewX</h1>
        <p className="mt-2 text-sm text-slate-500">Last updated: 2026-05-13</p>

        <section className="mt-6 space-y-4 text-slate-700">
          <h2 className="text-xl font-semibold text-slate-900">Our mission</h2>
          <p>
            ReviewX helps users compare products, understand trade-offs, and buy with confidence.
            We focus on practical guidance, clear scoring, and transparent affiliate disclosures.
          </p>
        </section>

        <section className="mt-6 space-y-4 text-slate-700">
          <h2 className="text-xl font-semibold text-slate-900">How reviews are written</h2>
          <p>
            Every review follows a repeatable editorial flow: gather product specs, compare with
            alternatives in the same budget, evaluate real-world use cases, and publish pros/cons
            with a final verdict.
          </p>
        </section>

        <section className="mt-6 space-y-4 text-slate-700">
          <h2 className="text-xl font-semibold text-slate-900">Update policy</h2>
          <p>
            We update content when pricing, product availability, or major product revisions change.
            Significant updates are reflected in the article metadata and page update date.
          </p>
        </section>

        <section className="mt-6 space-y-4 text-slate-700">
          <h2 className="text-xl font-semibold text-slate-900">Correction policy</h2>
          <p>
            If you find a factual error, contact us. Verified issues are corrected promptly with
            the relevant section revised so readers can rely on the latest accurate information.
          </p>
        </section>
      </article>
    </PageContainer>
  );
}
