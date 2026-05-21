# CHECKLIST-QA - ReviewX

## 1) Gate ky thuat
- [ ] `npm run lint` pass
- [ ] `npm run build` pass
- [ ] Khong co console error nghiem trong tren route public chinh
- [ ] Khong co hydration mismatch de nhan thay

## 2) Smoke route public
- [ ] `/`
- [ ] `/danh-muc`
- [ ] `/danh-muc/[slug]` (it nhat 1 slug that)
- [ ] `/san-pham/[slug]` (it nhat 1 product publish)
- [ ] `/review/[slug]` (it nhat 1 review publish)
- [ ] `/deals`
- [ ] `/so-sanh`
- [ ] `/cong-cu/chon-san-pham`
- [ ] `/search?q=...`

## 3) Smoke route admin
- [ ] `/admin` (co auth)
- [ ] `/admin/products`
- [ ] `/admin/products/new`
- [ ] `/admin/reviews`
- [ ] `/admin/reviews/new`
- [ ] `/admin/deals`
- [ ] `/admin/categories`
- [ ] `/admin/brands`
- [ ] `/admin/affiliate-links`
- [ ] Logout xong vao lai `/admin/products` bi redirect ve login

## 4) E2E luong admin dang tin

### Product
- [ ] Tao Product draft voi day du field co ban
- [ ] Publish Product thanh cong
- [ ] Mo public `/san-pham/[slug]` thay noi dung dung

### Review
- [ ] Tao Review gan dung Product
- [ ] Publish Review thanh cong
- [ ] Mo public `/review/[slug]` thay noi dung dung

### Deal
- [ ] Tao Affiliate Link hop le
- [ ] Tao Deal gan Product + Affiliate Link
- [ ] Deal xuat hien tai `/deals`
- [ ] CTA deal redirect dung URL dich qua route `/go/*`

## 5) Affiliate/Redirect integrity
- [ ] CTA mua hang tu product detail hoat dong
- [ ] CTA tu review detail hoat dong
- [ ] CTA tu deals list hoat dong
- [ ] Link loi thi redirect ve route thong bao loi ro rang (khong trang trang)

## 6) UX/UI glassmorphism review
- [ ] Admin shell (sidebar/header/content) dung style glass nhat quan
- [ ] Public cards/list/detail dung style glass nhat quan
- [ ] Input/button/table co state hover/focus/disabled ro rang
- [ ] Contrast van de doc duoc tren nen trong suot
- [ ] Mobile (<=768px) khong vo layout va khong che text
- [ ] Desktop (>=1280px) can bang spacing/typography

## 7) SEO/Metadata
- [ ] Moi page public chinh co title rieng
- [ ] Moi page public chinh co description rieng
- [ ] Canonical hop le
- [ ] OG/Twitter image va text hop le
- [ ] Structured data khong bi loi schema co ban

## 8) Link crawl co ban
- [ ] Co script crawl internal links
- [ ] Crawl khong bao 404 cho link noi bo critical
- [ ] Khong co route critical render trang trang

## 9) Bang chung ban giao
- [ ] File list thay doi
- [ ] Ket qua lint/build
- [ ] Anh chup desktop/mobile cho page chinh
- [ ] Log test E2E Product/Review/Deal
- [ ] Danh sach known issues (neu con)

