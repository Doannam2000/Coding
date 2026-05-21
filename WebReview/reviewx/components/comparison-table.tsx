"use client";

import Image from "next/image";
import Link from "next/link";

type CompareProduct = {
  id: string;
  name: string;
  image: string;
  priceLabel: string;
  oldPriceLabel?: string;
  discountLabel?: string;
  worthScore: number;
  category: string;
  rating?: number;
  soldCount?: number;
  brand?: string;
  specifications?: string[];
  pros?: string[];
  cons?: string[];
  bestFor?: string[];
};

type ComparisonTableProps = {
  products: CompareProduct[];
  onRemove: (id: string) => void;
  onSpecsToggle?: () => void;
  onDiffToggle?: () => void;
  onlyDifferences?: boolean;
  showAllSpecs?: boolean;
  isLoading?: boolean;
  hasError?: boolean;
  onRetry?: () => void;
  cellHighlightClass?: (active: boolean) => string;
  isLowestPrice?: (product: CompareProduct) => boolean;
  isHighestScore?: (product: CompareProduct) => boolean;
  isHighestDiscount?: (product: CompareProduct) => boolean;
  textOrUpdating?: (value?: string) => string;
  listOrUpdating?: (value?: string[]) => string;
  showSpecsToggle?: boolean;
  showDiffToggle?: boolean;
  differenceBadgeText?: string;
  tableLegend?: string;
};

export function ComparisonTable({
  products,
  onRemove,
  onSpecsToggle,
  onDiffToggle,
  onlyDifferences = false,
  showAllSpecs = false,
  isLoading = false,
  hasError = false,
  onRetry,
  cellHighlightClass = () => "",
  isLowestPrice = () => false,
  isHighestScore = () => false,
  isHighestDiscount = () => false,
  textOrUpdating = (v) => (v ?? "Đang cập nhật"),
  listOrUpdating = (v) => (v && v.length > 0 ? v.join(", ") : "Đang cập nhật"),
  showSpecsToggle = false,
  showDiffToggle = false,
  differenceBadgeText = "",
  tableLegend = "",
}: ComparisonTableProps) {
  const renderError = hasError;
  const renderLoading = isLoading;

  function detailHref(product: CompareProduct) {
    return `/san-pham/${product.id}`;
  }

  function reviewHref(product: CompareProduct) {
    return `/review/${product.id}`;
  }

  function affiliateHref(product: CompareProduct) {
    return `/go/product/${product.id}`;
  }

  return (
    <div className="space-y-4">
      {/* Controls */}
      {(showSpecsToggle || showDiffToggle || differenceBadgeText) && (
        <div className="flex flex-wrap items-center gap-2">
          {showDiffToggle && (
            <button
              type="button"
              onClick={onDiffToggle}
              className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-100"
            >
              {onlyDifferences ? "Hiện tất cả" : "Chỉ khác nhau"}
            </button>
          )}
          {showSpecsToggle && (
            <button
              type="button"
              onClick={onSpecsToggle}
              className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-100"
            >
              {showAllSpecs ? "Thu gọn specs" : "Xem toàn bộ specs"}
            </button>
          )}
          {differenceBadgeText && (
            <span className="text-xs text-blue-600">{differenceBadgeText}</span>
          )}
        </div>
      )}

      {/* Status messages */}
      {renderError && onRetry && (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          <p>Bảng so sánh đang lỗi.</p>
          <button
            type="button"
            onClick={onRetry}
            className="mt-1 rounded-full border border-red-200 bg-white px-3 py-1 text-xs font-semibold text-red-700"
          >
            Thử lại
          </button>
        </div>
      )}

      {renderLoading && (
        <div className="rounded-2xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600">
          Đang chuẩn bị bảng so sánh...
        </div>
      )}

      {/* Legend */}
      {tableLegend && <p className="text-xs text-emerald-700">{tableLegend}</p>}

      {/* Desktop Table */}
      <div className="hidden rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm lg:block">
        <div className="overflow-x-auto">
          <table className="min-w-[1200px] w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200">
                <th className="sticky left-0 z-10 bg-white px-3 py-3 text-left font-semibold text-slate-700">Tiêu chí</th>
                {products.map((product) => (
                  <th key={`head-${product.id}`} className="px-3 py-3 text-left font-semibold text-slate-900">
                    <div className="flex items-center gap-3">
                      <Image src={product.image} alt={product.name} width={44} height={44} className="size-11 rounded-lg object-cover" loading="lazy" unoptimized />
                      <span className="max-w-[150px] truncate">{product.name}</span>
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {/* Category */}
              <tr className="border-b border-slate-100">
                <td className="sticky left-0 bg-white px-3 py-3 font-medium text-slate-700">Danh mục</td>
                {products.map((product) => (
                  <td key={`category-${product.id}`} className="px-3 py-3 text-slate-700">{textOrUpdating(product.category)}</td>
                ))}
              </tr>
              {/* Brand */}
              <tr className="border-b border-slate-100">
                <td className="sticky left-0 bg-white px-3 py-3 font-medium text-slate-700">Thương hiệu</td>
                {products.map((product) => (
                  <td key={`brand-${product.id}`} className="px-3 py-3 text-slate-700">{textOrUpdating(product.brand)}</td>
                ))}
              </tr>
              {/* Price */}
              <tr className="border-b border-slate-100">
                <td className="sticky left-0 bg-white px-3 py-3 font-medium text-slate-700">Giá hiện tại</td>
                {products.map((product) => (
                  <td key={`price-${product.id}`} className={`px-3 py-3 text-slate-700 ${cellHighlightClass(isLowestPrice(product))}`}>
                    {product.priceLabel}
                  </td>
                ))}
              </tr>
              {/* Old Price */}
              <tr className="border-b border-slate-100">
                <td className="sticky left-0 bg-white px-3 py-3 font-medium text-slate-700">Giá gốc</td>
                {products.map((product) => (
                  <td key={`old-price-${product.id}`} className="px-3 py-3 text-slate-700">{textOrUpdating(product.oldPriceLabel)}</td>
                ))}
              </tr>
              {/* Discount */}
              <tr className="border-b border-slate-100">
                <td className="sticky left-0 bg-white px-3 py-3 font-medium text-slate-700">Giảm giá</td>
                {products.map((product) => (
                  <td key={`discount-${product.id}`} className={`px-3 py-3 text-slate-700 ${cellHighlightClass(isHighestDiscount(product))}`}>
                    {textOrUpdating(product.discountLabel)}
                  </td>
                ))}
              </tr>
              {/* Score */}
              <tr className="border-b border-slate-100">
                <td className="sticky left-0 bg-white px-3 py-3 font-medium text-slate-700">Điểm tổng quan</td>
                {products.map((product) => (
                  <td key={`worth-${product.id}`} className={`px-3 py-3 text-slate-700 ${cellHighlightClass(isHighestScore(product))}`}>
                    {product.worthScore}
                  </td>
                ))}
              </tr>
              {/* Specs */}
              <tr className="border-b border-slate-100">
                <td className="sticky left-0 bg-white px-3 py-3 font-medium text-slate-700">Thông số</td>
                {products.map((product) => (
                  <td key={`spec-${product.id}`} className="px-3 py-3 text-slate-700">{listOrUpdating(product.specifications)}</td>
                ))}
              </tr>
              {/* Pros */}
              <tr className="border-b border-slate-100">
                <td className="sticky left-0 bg-white px-3 py-3 font-medium text-slate-700">Ưu điểm</td>
                {products.map((product) => (
                  <td key={`pros-${product.id}`} className="px-3 py-3 text-slate-700">{listOrUpdating(product.pros)}</td>
                ))}
              </tr>
              {/* Cons */}
              <tr className="border-b border-slate-100">
                <td className="sticky left-0 bg-white px-3 py-3 font-medium text-slate-700">Nhược điểm</td>
                {products.map((product) => (
                  <td key={`cons-${product.id}`} className="px-3 py-3 text-slate-700">{listOrUpdating(product.cons)}</td>
                ))}
              </tr>
              {/* Best For */}
              <tr className="border-b border-slate-100">
                <td className="sticky left-0 bg-white px-3 py-3 font-medium text-slate-700">Phù hợp với</td>
                {products.map((product) => (
                  <td key={`bestfor-${product.id}`} className="px-3 py-3 text-slate-700">{listOrUpdating(product.bestFor)}</td>
                ))}
              </tr>
              {/* Actions */}
              <tr>
                <td className="sticky left-0 bg-white px-3 py-3 font-medium text-slate-700">Liên kết</td>
                {products.map((product) => (
                  <td key={`cta-${product.id}`} className="px-3 py-3">
                    <div className="flex flex-wrap gap-2">
                      <Link href={affiliateHref(product)} className="inline-flex items-center rounded-xl border border-orange-200 bg-orange-500 px-3 py-2 text-xs font-semibold text-white">Xem giá Shopee</Link>
                      <Link href={reviewHref(product)} className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700">Đọc review</Link>
                      <Link href={detailHref(product)} className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700">Chi tiết</Link>
                    </div>
                  </td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Mobile Cards */}
      <div className="grid gap-4 lg:hidden">
        {products.map((product) => (
          <article key={product.id} className="w-full rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md">
            <div className="flex items-start gap-3">
              <Image src={product.image} alt={product.name} width={56} height={56} className="size-14 rounded-lg object-cover" loading="lazy" unoptimized />
              <div className="min-w-0">
                <p className="text-xs font-semibold text-slate-500">{textOrUpdating(product.category)}</p>
                <h3 className="mt-1 break-words text-sm font-semibold leading-5 text-slate-900">{product.name}</h3>
              </div>
            </div>
            <div className="mt-3 space-y-2 text-sm text-slate-700">
              <p><span className="font-semibold text-slate-900">Thương hiệu:</span> {textOrUpdating(product.brand)}</p>
              <p className={cellHighlightClass(isLowestPrice(product))}><span className="font-semibold text-slate-900">Giá hiện tại:</span> {product.priceLabel}</p>
              <p><span className="font-semibold text-slate-900">Giá gốc:</span> {textOrUpdating(product.oldPriceLabel)}</p>
              <p className={cellHighlightClass(isHighestDiscount(product))}><span className="font-semibold text-slate-900">Giảm giá:</span> {textOrUpdating(product.discountLabel)}</p>
              <p className={cellHighlightClass(isHighestScore(product))}><span className="font-semibold text-slate-900">Điểm tổng quan:</span> {product.worthScore}</p>
              <p><span className="font-semibold text-slate-900">Thông số:</span> {listOrUpdating(product.specifications)}</p>
              <p><span className="font-semibold text-slate-900">Ưu điểm:</span> {listOrUpdating(product.pros)}</p>
              <p><span className="font-semibold text-slate-900">Nhược điểm:</span> {listOrUpdating(product.cons)}</p>
              <p><span className="font-semibold text-slate-900">Phù hợp với:</span> {listOrUpdating(product.bestFor)}</p>
            </div>
            <div className="mt-3 flex flex-wrap gap-2">
              <Link href={detailHref(product)} className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700">Chi tiết</Link>
              <Link href={reviewHref(product)} className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700">Đọc review</Link>
              <Link href={affiliateHref(product)} className="inline-flex items-center rounded-xl border border-orange-200 bg-orange-500 px-3 py-2 text-xs font-semibold text-white">Xem giá Shopee</Link>
            </div>
            <button type="button" onClick={() => onRemove(product.id)} className="mt-3 inline-flex items-center rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-xs font-semibold text-red-700 transition hover:bg-red-100">Bỏ sản phẩm</button>
          </article>
        ))}
      </div>
    </div>
  );
}