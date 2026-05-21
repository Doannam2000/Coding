import Link from "next/link";
import { PageContainer } from "./ui";

export function AppFooter() {
  return (
    <footer className="mt-16 border-t border-slate-200/70 bg-white">
      <PageContainer>
        <div className="grid gap-10 py-12 sm:grid-cols-2 lg:grid-cols-4">
          <div className="space-y-3">
            <p className="text-lg font-bold text-slate-900">ReviewX</p>
            <p className="text-sm text-slate-600">Nen tang giup ban quyet dinh mua hang nhanh va ro rang.</p>
          </div>

          <div className="space-y-2 text-sm text-slate-600">
            <p className="font-semibold text-slate-900">Minh bach affiliate</p>
            <p>Chung toi co the nhan hoa hong khi ban mua hang qua lien ket gioi thieu.</p>
          </div>

          <div className="space-y-2 text-sm">
            <p className="font-semibold text-slate-900">Trust pages</p>
            <div className="flex flex-col gap-2 text-slate-600">
              <Link href="/about" className="text-slate-600 transition hover:text-slate-900">Ve ReviewX</Link>
              <Link href="/cach-cham-diem" className="text-slate-600 transition hover:text-slate-900">Cach cham diem</Link>
              <Link href="/affiliate-policy" className="text-slate-600 transition hover:text-slate-900">Chinh sach affiliate</Link>
              <Link href="/privacy-policy" className="text-slate-600 transition hover:text-slate-900">Chinh sach bao mat</Link>
              <Link href="/terms" className="text-slate-600 transition hover:text-slate-900">Dieu khoan su dung</Link>
              <Link href="/contact" className="text-slate-600 transition hover:text-slate-900">Lien he</Link>
            </div>
          </div>

          <div className="space-y-2 text-sm text-slate-600">
            <p className="font-semibold text-slate-900">Social</p>
            <p>Ket noi voi chung toi qua email</p>
            <p>contact@reviewx.vn</p>
          </div>
        </div>
      </PageContainer>
    </footer>
  );
}
