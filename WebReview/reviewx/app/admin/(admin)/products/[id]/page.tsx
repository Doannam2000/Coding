"use client";

import { useParams } from "next/navigation";
import { PageContainer } from "@/components/ui";
import { AdminProductForm } from "@/components/admin-product-form";

export default function AdminProductEditPage() {
  const params = useParams<{ id: string }>();

  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6">
        <div className="mb-4">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Chỉnh sửa sản phẩm</h1>
          <p className="mt-1 text-sm text-slate-600">Cập nhật nội dung, trạng thái và dữ liệu hiển thị public.</p>
        </div>
        <AdminProductForm mode="edit" productId={params.id} />
      </section>
    </PageContainer>
  );
}
