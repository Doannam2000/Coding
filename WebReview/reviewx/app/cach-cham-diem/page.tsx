import { PageContainer } from "@/components/ui";

const scoringFactors = [
  { name: "Value for money", weight: "30%" },
  { name: "Core performance", weight: "25%" },
  { name: "Build and durability", weight: "15%" },
  { name: "Features and ecosystem", weight: "15%" },
  { name: "User feedback and reliability", weight: "15%" },
];

export default function ScoringMethodPage() {
  return (
    <PageContainer>
      <article className="mx-auto max-w-4xl rounded-3xl border border-slate-200/70 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="text-3xl font-bold text-slate-900">Cach cham diem san pham</h1>
        <p className="mt-2 text-sm text-slate-500">Last updated: 2026-05-13</p>

        <section className="mt-6 space-y-4 text-slate-700">
          <h2 className="text-xl font-semibold text-slate-900">Scoring framework</h2>
          <p>
            ReviewX uses a 10-point scale. Final score is a weighted combination of price-value,
            real-world performance, quality, feature set, and reliability signals from users.
          </p>
        </section>

        <section className="mt-6 space-y-4 text-slate-700">
          <h2 className="text-xl font-semibold text-slate-900">Weight by factor</h2>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[360px] text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-600">
                  <th className="py-2 font-semibold">Factor</th>
                  <th className="py-2 font-semibold">Weight</th>
                </tr>
              </thead>
              <tbody>
                {scoringFactors.map((factor) => (
                  <tr key={factor.name} className="border-b border-slate-100">
                    <td className="py-2 text-slate-800">{factor.name}</td>
                    <td className="py-2 text-slate-700">{factor.weight}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="mt-6 space-y-4 text-slate-700">
          <h2 className="text-xl font-semibold text-slate-900">How to read the score</h2>
          <ul className="list-disc space-y-2 pl-6">
            <li>8.5 - 10: highly recommended in its price band</li>
            <li>7.0 - 8.4: good option with trade-offs</li>
            <li>Below 7.0: buy only for specific needs</li>
          </ul>
        </section>
      </article>
    </PageContainer>
  );
}
