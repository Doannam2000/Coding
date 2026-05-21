import { PageContainer } from "@/components/ui";

export default function ContactPage() {
  return (
    <PageContainer>
      <article className="mx-auto max-w-4xl rounded-3xl border border-slate-200/70 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="text-3xl font-bold text-slate-900">Liên hệ</h1>
        <p className="mt-2 text-slate-600">Chúng tôi luôn sẵn sàng lắng nghe ý kiến và phản hồi từ bạn.</p>

        <section className="mt-8 space-y-6">
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-6">
            <h2 className="text-lg font-semibold text-slate-900">Email</h2>
            <p className="mt-2 text-slate-700">
              <a href="mailto:contact@reviewx.vn" className="text-blue-600 hover:underline">contact@reviewx.vn</a>
            </p>
            <p className="mt-1 text-sm text-slate-500">Chúng tôi sẽ phản hồi trong vòng 24-48 giờ làm việc.</p>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-6">
            <h2 className="text-lg font-semibold text-slate-900">Bạn có thể liên hệ với chúng tôi về</h2>
            <ul className="mt-3 space-y-2 text-slate-700">
              <li className="flex items-start gap-2">
                <span className="text-blue-600">•</span>
                <span>Yêu cầu review sản phẩm cụ thể</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-blue-600">•</span>
                <span>Báo cáo lỗi hoặc thông tin không chính xác trong bài review</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-blue-600">•</span>
                <span>Góp ý về nội dung hoặc trải nghiệm website</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-blue-600">•</span>
                <span>Câu hỏi về chính sách affiliate hoặc bảo mật</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-blue-600">•</span>
                <span>Hợp tác kinh doanh</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-blue-600">•</span>
                <span>Báo cáo deal hết hạn hoặc liên kết lỗi</span>
              </li>
            </ul>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-6">
            <h2 className="text-lg font-semibold text-slate-900">Thời gian phản hồi</h2>
            <p className="mt-2 text-slate-700">Chúng tôi cố gắng phản hồi tất cả email trong vòng 24-48 giờ làm việc (Thứ Hai - Thứ Sáu).</p>
            <p className="mt-2 text-sm text-slate-500">Lưu ý: Email gửi vào cuối tuần hoặc ngày lễ có thể được phản hồi chậm hơn.</p>
          </div>

          <div className="rounded-2xl border border-blue-200 bg-blue-50 p-6">
            <h2 className="text-lg font-semibold text-blue-900">Lưu ý quan trọng</h2>
            <p className="mt-2 text-sm text-blue-800">ReviewX không cung cấp dịch vụ hỗ trợ khách hàng cho các giao dịch mua bán trên Shopee, Lazada, Tiki hoặc các nền tảng khác. Vui lòng liên hệ trực tiếp với người bán hoặc nền tảng đó để được hỗ trợ về đơn hàng, hoàn trả, hoặc bảo hành.</p>
          </div>
        </section>
      </article>
    </PageContainer>
  );
}
