type SpecsTableProps = {
  specs: Array<{ label: string; value?: string }>;
};

export function SpecsTable({ specs }: SpecsTableProps) {
  const normalizedSpecs = specs
    .map((spec) => ({
      label: spec.label.trim(),
      value: typeof spec.value === "string" ? spec.value.trim() : "",
    }))
    .filter((spec) => spec.label.length > 0 && spec.value.length > 0);

  if (normalizedSpecs.length === 0) {
    return null;
  }

  return (
    <section className="mt-8 rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
      <h2 className="text-2xl font-bold tracking-tight text-slate-900">ThÃ´ng sá»‘ ká»¹ thuáº­t</h2>

      <div className="mt-4 overflow-x-auto">
        <table className="w-full min-w-[520px] border-separate border-spacing-0 overflow-hidden rounded-2xl border border-slate-200">
          <tbody>
            {normalizedSpecs.map((spec) => (
              <tr key={spec.label} className="odd:bg-white even:bg-slate-50">
                <th className="w-1/3 border-b border-slate-200 px-4 py-3 text-left text-sm font-semibold text-slate-800">{spec.label}</th>
                <td className="border-b border-slate-200 px-4 py-3 text-sm text-slate-700 break-words">{spec.value}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
