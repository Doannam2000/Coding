"use client";

import { PageContainer } from "@/components/ui";
import { AdminProductForm } from "@/components/admin-product-form";

export default function AdminProductCreatePage() {
  return (
    <PageContainer>
      <section className="rounded-3xl border border-slate-200/70 bg-white p-4 shadow-sm sm:p-6">
        <div className="mb-4">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Tạo sản phẩm mới</h1>
          <p className="mt-1 text-sm text-slate-600">Nhập đầy đủ thông tin để tạo sản phẩm draft hoặc publish.</p>
        </div>
        <AdminProductForm mode="create" />
      </section>
    </PageContainer>
  );
}
