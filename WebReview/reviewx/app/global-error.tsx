"use client";

export default function GlobalError() {
  return (
    <html lang="vi">
      <body className="bg-slate-50">
        <main className="mx-auto max-w-3xl px-4 py-16">
          <section className="rounded-2xl border border-slate-200 bg-white p-6 text-center shadow-sm">
            <h1 className="text-xl font-semibold text-slate-900">Đã có lỗi hệ thống</h1>
            <p className="mt-2 text-sm text-slate-600">Vui lòng tải lại trang hoặc thử lại sau.</p>
          </section>
        </main>
      </body>
    </html>
  );
}
