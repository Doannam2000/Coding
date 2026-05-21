import { LoadingSkeleton, PageContainer } from "@/components/ui";

export default function Loading() {
  return (
    <PageContainer>
      <div className="rounded-3xl border border-slate-200/70 bg-white p-8 shadow-sm">
        <LoadingSkeleton className="mb-4 h-8 w-1/2" />
        <LoadingSkeleton className="h-4 w-full" />
      </div>
    </PageContainer>
  );
}
