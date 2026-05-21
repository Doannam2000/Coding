import { PageContainer } from "@/components/ui";

export default function TermsPage() {
  return (
    <PageContainer>
      <article className="mx-auto max-w-4xl rounded-3xl border border-slate-200/70 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="text-3xl font-bold text-slate-900">Điều khoản sử dụng</h1>
        <p className="mt-2 text-sm text-slate-500">Cập nhật lần cuối: 11/05/2026</p>

        <section className="mt-6 space-y-4 text-slate-700">
          <h2 className="text-xl font-semibold text-slate-900">1. Chấp nhận điều khoản</h2>
          <p>Bằng việc truy cập và sử dụng ReviewX, bạn đồng ý tuân thủ các điều khoản sử dụng này. Nếu bạn không đồng ý, vui lòng không sử dụng website.</p>

          <h2 className="text-xl font-semibold text-slate-900">2. Nội dung review</h2>
          <p>Tất cả các bài review trên ReviewX được viết dựa trên:</p>
          <ul className="list-disc space-y-2 pl-6">
            <li>Nghiên cứu thông tin sản phẩm từ nhiều nguồn đáng tin cậy</li>
            <li>Trải nghiệm thực tế với sản phẩm khi có thể</li>
            <li>Phân tích đánh giá từ người dùng thực tế</li>
            <li>So sánh với các sản phẩm cùng phân khúc</li>
          </ul>
          <p className="mt-2">ReviewX cố gắng cung cấp thông tin chính xác và khách quan, nhưng không đảm bảo tính đầy đủ hoặc phù hợp cho mọi trường hợp sử dụng.</p>

          <h2 className="text-xl font-semibold text-slate-900">3. Liên kết affiliate</h2>
          <p>ReviewX tham gia các chương trình affiliate với Shopee, Lazada, Tiki và các nền tảng khác. Khi bạn mua hàng qua liên kết của chúng tôi, ReviewX có thể nhận hoa hồng mà không làm tăng giá sản phẩm bạn mua.</p>
          <p className="mt-2">Việc nhận hoa hồng không ảnh hưởng đến tính khách quan của các bài review. Chúng tôi chỉ giới thiệu sản phẩm mà chúng tôi tin là có giá trị cho người dùng.</p>

          <h2 className="text-xl font-semibold text-slate-900">4. Trách nhiệm của người dùng</h2>
          <p>Khi sử dụng ReviewX, bạn đồng ý:</p>
          <ul className="list-disc space-y-2 pl-6">
            <li>Không sử dụng website cho mục đích bất hợp pháp</li>
            <li>Không can thiệp vào hoạt động của website</li>
            <li>Không sao chép nội dung mà không có sự cho phép</li>
            <li>Tự chịu trách nhiệm về quyết định mua hàng của mình</li>
          </ul>

          <h2 className="text-xl font-semibold text-slate-900">5. Giới hạn trách nhiệm</h2>
          <p>ReviewX không chịu trách nhiệm về:</p>
          <ul className="list-disc space-y-2 pl-6">
            <li>Chất lượng, giá cả, hoặc tình trạng sản phẩm thực tế từ người bán</li>
            <li>Tranh chấp giữa bạn và người bán</li>
            <li>Thiệt hại phát sinh từ việc sử dụng thông tin trên website</li>
            <li>Lỗi kỹ thuật, gián đoạn dịch vụ, hoặc mất dữ liệu</li>
          </ul>

          <h2 className="text-xl font-semibold text-slate-900">6. Thay đổi điều khoản</h2>
          <p>ReviewX có quyền thay đổi điều khoản sử dụng bất kỳ lúc nào. Các thay đổi có hiệu lực ngay khi được đăng tải trên website.</p>

          <h2 className="text-xl font-semibold text-slate-900">7. Liên hệ</h2>
          <p>Nếu bạn có câu hỏi về điều khoản sử dụng, vui lòng liên hệ qua trang <a href="/contact" className="text-blue-600 hover:underline">Liên hệ</a>.</p>
        </section>
      </article>
    </PageContainer>
  );
}
