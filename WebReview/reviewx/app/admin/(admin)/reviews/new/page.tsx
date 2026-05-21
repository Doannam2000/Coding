"use client";

import { PageContainer } from "@/components/ui";
import { AdminReviewForm } from "@/components/admin-review-form";

export default function AdminReviewCreatePage() {
  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6">
        <div className="mb-4">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Tạo review mới</h1>
          <p className="mt-1 text-sm text-slate-600">Nhập nội dung review ở dạng draft hoặc publish.</p>
        </div>
        <AdminReviewForm mode="create" />
      </section>
    </PageContainer>
  );
}
