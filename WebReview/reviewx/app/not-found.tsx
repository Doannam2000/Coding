import Link from "next/link";
import { PageContainer } from "@/components/ui";

const POPULAR_CATEGORIES = [
  { name: "Công nghệ", slug: "cong-nghe" },
  { name: "Gia dụng", slug: "gia-dung" },
  { name: "Làm đẹp", slug: "lam-dep" },
  { name: "Mẹ & bé", slug: "me-be" },
];

export default function NotFound() {
  return (
    <PageContainer>
      <div className="mx-auto max-w-3xl rounded-3xl border border-slate-200/70 bg-white p-8 text-center shadow-sm sm:p-12">
        <div className="text-6xl font-bold text-slate-300">404</div>
        <h1 className="mt-4 text-3xl font-bold text-slate-900">Không tìm thấy trang</h1>
        <p className="mt-3 text-slate-600">Trang bạn đang tìm kiếm có thể đã bị xóa, đổi tên hoặc tạm thời không khả dụng.</p>

        <form action="/tim-kiem" method="get" className="mt-8">
          <div className="flex gap-2">
            <input
              name="q"
              type="text"
              placeholder="Tìm kiếm sản phẩm, review, deal..."
              className="flex-1 rounded-xl border border-slate-300 px-4 py-3 text-sm focus:border-blue-500 focus:outline-none"
            />
            <button type="submit" className="rounded-xl bg-blue-600 px-6 py-3 text-sm font-semibold text-white hover:bg-blue-700">
              Tìm kiếm
            </button>
          </div>
        </form>

        <div className="mt-8">
          <Link href="/" className="inline-flex rounded-xl bg-slate-900 px-6 py-3 text-sm font-semibold text-white hover:bg-slate-800">
            Về trang chủ
          </Link>
        </div>

        <div className="mt-10 border-t border-slate-200 pt-8">
          <h2 className="text-lg font-semibold text-slate-900">Danh mục phổ biến</h2>
          <div className="mt-4 flex flex-wrap justify-center gap-3">
            {POPULAR_CATEGORIES.map((cat) => (
              <Link
                key={cat.slug}
                href={`/danh-muc/${cat.slug}`}
                className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-2 text-sm font-medium text-slate-700 hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700"
              >
                {cat.name}
              </Link>
            ))}
          </div>
        </div>

        <div className="mt-8 flex flex-wrap justify-center gap-4 text-sm">
          <Link href="/deals" className="text-blue-600 hover:underline">
            Xem deal hot
          </Link>
          <Link href="/so-sanh" className="text-blue-600 hover:underline">
            So sánh sản phẩm
          </Link>
          <Link href="/cong-cu/chon-san-pham" className="text-blue-600 hover:underline">
            Công cụ chọn sản phẩm
          </Link>
        </div>
      </div>
    </PageContainer>
  );
}
