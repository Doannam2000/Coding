import { LoadingSkeleton, PageContainer } from "@/components/ui";

export default function ProductDetailLoading() {
  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
        <div className="grid gap-8 lg:grid-cols-2">
          <div className="space-y-4">
            <LoadingSkeleton className="h-72 w-full rounded-3xl sm:h-96" />
            <div className="grid grid-cols-4 gap-3">
              <LoadingSkeleton className="h-20 w-full rounded-2xl" />
              <LoadingSkeleton className="h-20 w-full rounded-2xl" />
              <LoadingSkeleton className="h-20 w-full rounded-2xl" />
              <LoadingSkeleton className="h-20 w-full rounded-2xl" />
            </div>
          </div>

          <div className="space-y-3">
            <LoadingSkeleton className="h-6 w-24" />
            <LoadingSkeleton className="h-10 w-5/6" />
            <LoadingSkeleton className="h-5 w-1/2" />
            <LoadingSkeleton className="h-8 w-2/3" />
            <LoadingSkeleton className="h-24 w-full rounded-2xl" />
            <LoadingSkeleton className="h-12 w-40 rounded-2xl" />
          </div>
        </div>
      </section>
    </PageContainer>
  );
}
