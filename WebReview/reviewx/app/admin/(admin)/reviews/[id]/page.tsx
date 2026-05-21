"use client";

import { useParams } from "next/navigation";
import { PageContainer } from "@/components/ui";
import { AdminReviewForm } from "@/components/admin-review-form";

export default function AdminReviewEditPage() {
  const params = useParams<{ id: string }>();

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6">
        <div className="mb-4">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Chỉnh sửa review</h1>
          <p className="mt-1 text-sm text-slate-600">Cập nhật nội dung review và trạng thái xuất bản.</p>
        </div>
        <AdminReviewForm mode="edit" reviewId={params.id} />
      </section>
    </PageContainer>
  );
}
