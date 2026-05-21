import { PageContainer } from "@/components/ui";

export default function PrivacyPolicyPage() {
  return (
    <PageContainer>
      <article className="mx-auto max-w-4xl rounded-3xl border border-slate-200/70 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="text-3xl font-bold text-slate-900">Chính sách bảo mật</h1>
        <p className="mt-2 text-sm text-slate-500">Cập nhật lần cuối: 11/05/2026</p>

        <section className="mt-6 space-y-4 text-slate-700">
          <h2 className="text-xl font-semibold text-slate-900">1. Thông tin chúng tôi thu thập</h2>
          <p>ReviewX thu thập các thông tin sau khi bạn sử dụng website:</p>
          <ul className="list-disc space-y-2 pl-6">
            <li>Thông tin truy cập: địa chỉ IP, trình duyệt, thiết bị, thời gian truy cập</li>
            <li>Thông tin tương tác: sản phẩm bạn xem, review bạn đọc, deal bạn click</li>
            <li>Cookie và công nghệ tương tự để cải thiện trải nghiệm người dùng</li>
          </ul>

          <h2 className="text-xl font-semibold text-slate-900">2. Mục đích sử dụng thông tin</h2>
          <p>Chúng tôi sử dụng thông tin để:</p>
          <ul className="list-disc space-y-2 pl-6">
            <li>Cải thiện nội dung và trải nghiệm người dùng</li>
            <li>Phân tích hành vi người dùng để đề xuất sản phẩm phù hợp</li>
            <li>Theo dõi hiệu quả của các liên kết affiliate</li>
            <li>Tuân thủ quy định pháp luật</li>
          </ul>

          <h2 className="text-xl font-semibold text-slate-900">3. Chia sẻ thông tin</h2>
          <p>ReviewX không bán thông tin cá nhân của bạn cho bên thứ ba. Chúng tôi chỉ chia sẻ thông tin với:</p>
          <ul className="list-disc space-y-2 pl-6">
            <li>Các nền tảng affiliate (Shopee, Lazada, Tiki) khi bạn click vào liên kết mua hàng</li>
            <li>Nhà cung cấp dịch vụ phân tích (Google Analytics) để cải thiện website</li>
            <li>Cơ quan chức năng khi có yêu cầu hợp pháp</li>
          </ul>

          <h2 className="text-xl font-semibold text-slate-900">4. Bảo mật thông tin</h2>
          <p>Chúng tôi áp dụng các biện pháp bảo mật kỹ thuật và tổ chức để bảo vệ thông tin của bạn khỏi truy cập trái phép, mất mát, hoặc tiết lộ.</p>

          <h2 className="text-xl font-semibold text-slate-900">5. Quyền của bạn</h2>
          <p>Bạn có quyền:</p>
          <ul className="list-disc space-y-2 pl-6">
            <li>Yêu cầu truy cập, chỉnh sửa, hoặc xóa thông tin cá nhân</li>
            <li>Từ chối cookie thông qua cài đặt trình duyệt</li>
            <li>Phản đối việc xử lý dữ liệu của bạn</li>
          </ul>

          <h2 className="text-xl font-semibold text-slate-900">6. Liên hệ</h2>
          <p>Nếu bạn có câu hỏi về chính sách bảo mật, vui lòng liên hệ qua trang <a href="/contact" className="text-blue-600 hover:underline">Liên hệ</a>.</p>
        </section>
      </article>
    </PageContainer>
  );
}
