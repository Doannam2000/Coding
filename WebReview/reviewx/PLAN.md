# PLAN - ReviewX Fix Roadmap (UX/UI + Functionality)

## Muc tieu
- Hoan thien chuc nang admin dang tin end-to-end (Product, Review, Deal, Affiliate).
- Nang cap UX/UI dong bo theo dinh huong glassmorphism.
- Dat muc production-ready voi gate ky thuat va QA ro rang.

## Nguyen tac thuc thi
1. Fix stability truoc khi redesign UI.
2. Moi task phai co bang chung verify (lint/build/test/screenshot).
3. Khong merge UI lon neu luong chuc nang chinh chua pass.

## Sprint roadmap

### Sprint 1 - Technical Baseline (1-2 ngay)
**Muc tieu:** codebase sach, on dinh.

**Work items**
- Fix lint/runtime:
  - `app/deals/page.tsx`
  - `app/so-sanh/page.tsx`
  - `app/danh-muc/[slug]/page-client.tsx`
  - `app/page.tsx`
  - `app/review/[slug]/page.tsx`
- Dinh nghia lai lint scope cho script phu tro hoac chuyen sang TS:
  - `check-*.js`, `insert-sony-review.js`, `dist/seed.js`, `deals-backup.tsx`
- Chuan hoa text tieng Viet bi loi encoding:
  - `app/layout.tsx`
  - `app/admin/(admin)/*`
  - `components/admin-product-form.tsx`
  - `components/admin-review-form.tsx`

**Exit criteria**
- `npm run lint` pass.
- `npm run build` pass.
- Khong con text loi ma hoa o UI chinh.

### Sprint 2 - Admin Functional Reliability (1-2 ngay)
**Muc tieu:** admin publish du lieu that thanh cong.

**Work items**
- Product flow:
  - `components/admin-product-form.tsx`
  - `app/api/admin/products/route.ts`
  - `app/api/admin/products/[id]/route.ts`
- Review flow:
  - `components/admin-review-form.tsx`
  - `app/api/admin/reviews/route.ts`
  - `app/api/admin/reviews/[id]/route.ts`
- Deal + affiliate flow:
  - `app/admin/(admin)/deals/page.tsx`
  - `app/api/admin/deals/route.ts`
  - `app/api/admin/deals/[id]/route.ts`
  - `app/api/admin/affiliate-links/route.ts`
  - `app/api/admin/affiliate-links/[id]/route.ts`

**Exit criteria**
- Tao/publish duoc 1 Product, 1 Review, 1 Deal that.
- Public route hien thi dung:
  - `/san-pham/[slug]`
  - `/review/[slug]`
  - `/deals`
- CTA affiliate redirect dung qua `/go/*`.

### Sprint 3 - Glassmorphism UX/UI Rollout (2-3 ngay)
**Muc tieu:** dong bo giao dien admin + public.

**Work items**
- Tao design tokens glass:
  - `app/globals.css`
- Refactor admin shell + controls:
  - `app/admin/(admin)/layout.tsx`
  - `components/ui.tsx`
  - admin pages chinh
- Refactor public pages:
  - `app/page.tsx`
  - `app/deals/page.tsx`
  - `app/san-pham/[slug]/page.tsx`
  - `app/review/[slug]/page.tsx`
  - card/list components lien quan

**Glass standards**
- Surface: trong suot nhe + blur vua phai.
- Border: alpha border de tach lop.
- Elevation: shadow mem, khong qua dam.
- State: hover/focus/active/disabled nhat quan.
- Accessibility: contrast va focus ring ro rang.

**Exit criteria**
- Admin va public dung cung he visual glass.
- Khong vo layout mobile/desktop.
- Text de doc, khong bi mat contrast do nen trong.

### Sprint 4 - SEO + QA Gate (1 ngay)
**Muc tieu:** san sang release.

**Work items**
- Them metadata title/description rieng tung page public.
- Rasoat canonical/OG/Twitter/schema.
- Viet script crawl internal links de bat 404/trang trang.
- Smoke test route chinh + luong CTA.

**Exit criteria**
- Checklist QA pass.
- Khong con TODO critical mo.
- Co bao cao ket qua truoc release.

## Task board flow
- Backlog -> Ready -> In Progress -> Review -> QA -> Done

## Rule cho "Done"
Mot task chi duoc "Done" khi co du:
1. Mo ta thay doi.
2. Bang chung command/test hoac screenshot.
3. Khong pha gate lint/build.

## Muc uu tien
- P0: Stability + Admin publish flow + CTA affiliate.
- P1: UX admin + metadata per-page.
- P2: Glass polish + automation QA mo rong.

