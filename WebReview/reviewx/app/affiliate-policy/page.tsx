import { PageContainer } from "@/components/ui";

export default function AffiliatePolicyPage() {
  return (
    <PageContainer>
      <article className="mx-auto max-w-4xl rounded-3xl border border-slate-200/70 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="text-3xl font-bold text-slate-900">Chính sách Affiliate</h1>
        <p className="mt-2 text-sm text-slate-500">Cập nhật lần cuối: 11/05/2026</p>

        <section className="mt-6 space-y-4 text-slate-700">
          <h2 className="text-xl font-semibold text-slate-900">ReviewX kiếm tiền như thế nào?</h2>
          <p>ReviewX là một website review sản phẩm độc lập. Chúng tôi kiếm tiền thông qua các chương trình affiliate marketing với các nền tảng thương mại điện tử như Shopee, Lazada, Tiki và các đối tác khác.</p>
          <p className="mt-2">Khi bạn click vào liên kết sản phẩm trên ReviewX và mua hàng, chúng tôi có thể nhận một khoản hoa hồng nhỏ từ người bán. Điều này không làm tăng giá sản phẩm bạn mua.</p>

          <h2 className="text-xl font-semibold text-slate-900">Affiliate có ảnh hưởng đến review không?</h2>
          <p><strong>Không.</strong> Tính khách quan là ưu tiên hàng đầu của ReviewX. Chúng tôi cam kết:</p>
          <ul className="list-disc space-y-2 pl-6">
            <li>Review dựa trên nghiên cứu kỹ lưỡng và trải nghiệm thực tế</li>
            <li>Không bao giờ thay đổi đánh giá để tăng hoa hồng</li>
            <li>Chỉ giới thiệu sản phẩm mà chúng tôi tin là có giá trị thực sự</li>
            <li>Luôn chỉ ra cả ưu điểm và nhược điểm của sản phẩm</li>
            <li>Không nhận tiền từ nhà sản xuất để viết review tích cực</li>
          </ul>

          <h2 className="text-xl font-semibold text-slate-900">Chúng tôi làm việc với ai?</h2>
          <p>ReviewX tham gia các chương trình affiliate với:</p>
          <ul className="list-disc space-y-2 pl-6">
            <li><strong>Shopee Affiliate Program</strong> - Nền tảng thương mại điện tử hàng đầu Việt Nam</li>
            <li><strong>Lazada Affiliate Program</strong> - Nền tảng mua sắm trực tuyến</li>
            <li><strong>Tiki Affiliate Program</strong> - Nền tảng bán lẻ trực tuyến</li>
            <li>Các đối tác khác có thể được thêm vào trong tương lai</li>
          </ul>

          <h2 className="text-xl font-semibold text-slate-900">Cách chúng tôi chọn sản phẩm review</h2>
          <p>Chúng tôi chọn sản phẩm để review dựa trên:</p>
          <ul className="list-disc space-y-2 pl-6">
            <li>Nhu cầu và quan tâm của người dùng</li>
            <li>Sản phẩm phổ biến hoặc đang có deal tốt</li>
            <li>Sản phẩm mới ra mắt đáng chú ý</li>
            <li>Yêu cầu từ cộng đồng người đọc</li>
          </ul>
          <p className="mt-2">Việc có chương trình affiliate không ảnh hưởng đến quyết định review sản phẩm nào.</p>

          <h2 className="text-xl font-semibold text-slate-900">Minh bạch về liên kết</h2>
          <p>Tất cả các liên kết affiliate trên ReviewX đều được đánh dấu rõ ràng. Khi bạn thấy các nút như &quot;Xem giá&quot;, &quot;Mua ngay&quot;, hoặc &quot;Xem deal&quot;, đó là liên kết affiliate.</p>
          <p className="mt-2">Bạn hoàn toàn có thể tự tìm kiếm sản phẩm trên các nền tảng thương mại điện tử mà không cần qua liên kết của chúng tôi. Tuy nhiên, việc sử dụng liên kết của ReviewX giúp chúng tôi duy trì và phát triển nội dung chất lượng miễn phí cho bạn.</p>

          <h2 className="text-xl font-semibold text-slate-900">Cam kết của chúng tôi</h2>
          <p>ReviewX cam kết:</p>
          <ul className="list-disc space-y-2 pl-6">
            <li>Luôn minh bạch về mối quan hệ affiliate</li>
            <li>Đặt lợi ích người đọc lên hàng đầu</li>
            <li>Cung cấp thông tin chính xác và khách quan</li>
            <li>Cập nhật review khi có thông tin mới quan trọng</li>
            <li>Lắng nghe và phản hồi ý kiến từ cộng đồng</li>
          </ul>

          <h2 className="text-xl font-semibold text-slate-900">Câu hỏi hoặc phản hồi?</h2>
          <p>Nếu bạn có bất kỳ câu hỏi nào về chính sách affiliate của chúng tôi, hoặc muốn báo cáo vấn đề về tính khách quan của một bài review, vui lòng liên hệ qua trang <a href="/contact" className="text-blue-600 hover:underline">Liên hệ</a>.</p>
        </section>
      </article>
    </PageContainer>
  );
}
