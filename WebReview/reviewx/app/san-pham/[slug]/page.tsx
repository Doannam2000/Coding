import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { prisma } from "@/lib/prisma";
import { BestForAvoidIf } from "@/components/best-for-avoid-if";
import { FAQSection } from "@/components/faq-section";
import { ProductGallery } from "@/components/product-gallery";
import { StickyMobileCTA } from "@/components/sticky-mobile-cta";
import { ProsConsGrid } from "@/components/pros-cons-grid";
import { ReviewInsightCard } from "@/components/review-insight-card";
import { SpecsTable } from "@/components/specs-table";
import { VerdictCard } from "@/components/verdict-card";
import { PageContainer, ProductScoreBadge, ShopeeCTAButton } from "@/components/ui";

type ProductStatus = "dang-mua" | "can-nhac" | "khong-khuyen-nghi";

type AffiliateLinkItem = {
  id: string;
  platform: string;
  url?: string;
  available: boolean;
  isPrimary: boolean;
};

type SimilarProduct = {
  id: string;
  name: string;
  score: number;
  price: string;
  reason: "Rẻ hơn" | "Cao cấp hơn" | "Bán chạy hơn" | "Cùng phân khúc";
  image: string;
};

type ProductFaq = {
  question: string;
  answer: string;
};

type ProductData = {
  id: string;
  slug: string;
  name: string;
  brand: string;
  category: string;
  categorySlug: string;
  shopeeRating: number;
  soldCount: string;
  currentPrice?: string;
  oldPrice?: string;
  discountPercent?: number;
  priceRange?: string;
  priceUpdatedAt?: string;
  description?: string;
  tags?: string[];
  relatedDeals?: { id: string; title: string; platform: string; currentPrice: string; discount: string; href: string }[];
  relatedReviews?: { id: string; slug: string; title: string; score: number; href: string }[];
  worthScore: number;
  status: ProductStatus;
  verdict?: string;
  buyIf: string;
  considerIf: string;
  avoidIf: string;
  buyPriceHint: string;
  considerPriceHint: string;
  pros: string[];
  cons: string[];
  bestFor: string[];
  avoidFor: string[];
  insightPositive: string[];
  insightNegative: string[];
  insightKeywords: string[];
  sentimentScore: string;
  insightVerified: boolean;
  specs: Array<{ label: string; value?: string }>;
  similarProducts: SimilarProduct[];
  faqs: ProductFaq[];
  images: string[];
  isHot: boolean;
  affiliateActive: boolean;
  affiliateUrlMissing?: boolean;
  affiliateLinks?: AffiliateLinkItem[];
};

const fallbackImage = "https://images.unsplash.com/photo-1545127398-14699f92334b?q=80&w=1200&auto=format&fit=crop";

function parseItems(raw: string | null | undefined): string[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed.slice(0, 20);
    return raw.split("\n").filter(Boolean).slice(0, 20);
  } catch {
    return raw.split("\n").filter(Boolean).slice(0, 20);
  }
}

function buildVerdict(product: { verdict?: string | null; priceMin?: number | null; priceMax?: number | null }) {
  const v = product.verdict ?? "";
  const min = product.priceMin;
  const max = product.priceMax;
  if (v) return v;
  if (min != null || max != null) {
    const range = min === max
      ? `${min?.toLocaleString("vi-VN")}đ`
      : min != null && max != null
        ? `${min.toLocaleString("vi-VN")}đ – ${max.toLocaleString("vi-VN")}đ`
        : min != null ? `Từ ${min.toLocaleString("vi-VN")}đ` : `Đến ${max?.toLocaleString("vi-VN")}đ`;
    return `Điểm đáng mua tốt nhất trong khoảng giá ${range}.`;
  }
  return "Chưa có đánh giá tổng quan.";
}

function scoreToStatus(score: number): ProductStatus {
  if (score >= 8) return "dang-mua";
  if (score >= 6) return "can-nhac";
  return "khong-khuyen-nghi";
}

function normalizeSlugInput(slug: string): string {
  return slug.trim().toLowerCase().replace(/\s+/g, "-").replace(/-+/g, "-").replace(/^-|-$/g, "");
}

async function findPublishedProductBySlug(slug: string) {
  const normalizedSlug = normalizeSlugInput(slug);

  const exact = await prisma.product.findFirst({
    where: { slug: normalizedSlug, status: "PUBLISHED" },
    include: {
      brand: true,
      category: true,
      images: { orderBy: { sortOrder: "asc" } },
      affiliateLinks: { where: { status: "ACTIVE" }, orderBy: [{ isPrimary: "desc" }, { updatedAt: "desc" }] },
    },
  });

  if (exact) {
    return exact;
  }

  return prisma.product.findFirst({
    where: {
      status: "PUBLISHED",
      OR: [{ slug: { endsWith: `-${normalizedSlug}` } }, { slug: { contains: normalizedSlug } }],
    },
    include: {
      brand: true,
      category: true,
      images: { orderBy: { sortOrder: "asc" } },
      affiliateLinks: { where: { status: "ACTIVE" }, orderBy: [{ isPrimary: "desc" }, { updatedAt: "desc" }] },
    },
    orderBy: { updatedAt: "desc" },
  });
}

async function getProduct(slug: string): Promise<ProductData | null> {
  const product = await findPublishedProductBySlug(slug);

  if (!product) return null;

  const dbDeals = await prisma.deal.findMany({
    where: { productId: product.id, status: "Active" },
    take: 3,
    orderBy: { discount: "desc" },
    include: { affiliateLink: true },
  });

  const dbReviews = await prisma.review.findMany({
    where: { productId: product.id, status: "PUBLISHED" },
    take: 3,
    select: { id: true, slug: true, title: true, score: true, coverImage: true },
  });

  const images = product.images.length > 0 ? product.images.map((i) => i.url) : [product.thumbnail ?? fallbackImage];
  const score = product.worthScore ?? 0;
  const status = scoreToStatus(score);

  // Best deal for pricing
  const bestDeal = dbDeals[0];
  let currentPrice: string | undefined;
  let oldPrice: string | undefined;
  let discountPercent: number | undefined;
  if (bestDeal) {
    const cp = bestDeal.currentPrice;
    const op = bestDeal.oldPrice;
    if (cp) currentPrice = `${Number(cp).toLocaleString("vi-VN")}đ`;
    if (op) oldPrice = `${Number(op).toLocaleString("vi-VN")}đ`;
    const cpNum = Number(cp);
    const opNum = Number(op);
    if (cpNum && opNum && opNum > 0) discountPercent = Math.round(((opNum - cpNum) / opNum) * 100);
  }

  const priceRange = product.priceMin !== null && product.priceMax !== null
    ? `${product.priceMin.toLocaleString("vi-VN")}đ – ${product.priceMax.toLocaleString("vi-VN")}đ`
    : product.priceMin !== null ? `Từ ${product.priceMin.toLocaleString("vi-VN")}đ`
    : product.priceMax !== null ? `Đến ${product.priceMax.toLocaleString("vi-VN")}đ`
    : undefined;

  const verdict = buildVerdict(product);
  const priceHint = product.priceMax !== null ? Math.round(product.priceMax * 0.85) : null;
  const considerHint = product.priceMax !== null ? Math.round(product.priceMax * 1.1) : null;

  const relatedDeals = dbDeals.map((d) => ({
    id: d.id,
    title: d.discount,
    platform: d.affiliateLink?.platform ?? "Shopee",
    currentPrice: d.currentPrice ? `${Number(d.currentPrice).toLocaleString("vi-VN")}đ` : "—",
    discount: d.discount ?? "",
    href: `/go/deal/${d.id}`,
  }));

  const relatedReviews = dbReviews.map((r) => ({
    id: r.id,
    slug: r.slug ?? "",
    title: r.title ?? "",
    score: r.score ?? 0,
    href: `/review/${r.slug}`,
  }));

  const hasAffiliate = product.affiliateLinks.some((l) => l.status === "ACTIVE");

  return {
    id: product.id,
    slug: product.slug,
    name: product.name,
    brand: product.brand?.name ?? "",
    category: product.category?.name ?? "Công nghệ",
    categorySlug: product.category?.slug ?? "",
    shopeeRating: product.rating ?? 0,
    soldCount: product.soldCount ? `${Math.round(product.soldCount / 1000)}k` : "—",
    currentPrice,
    oldPrice,
    discountPercent,
    priceRange,
    description: product.description ?? "",
    worthScore: score,
    status,
    verdict,
    buyIf: product.verdict ?? "Sản phẩm đáng cân nhắc trong tầm giá.",
    considerIf: "",
    avoidIf: "",
    buyPriceHint: priceHint ? `Nên mua nếu dưới ${priceHint.toLocaleString("vi-VN")}đ` : "",
    considerPriceHint: considerHint ? `Cân nhắc nếu trên ${considerHint.toLocaleString("vi-VN")}đ` : "",
    pros: parseItems(product.pros),
    cons: parseItems(product.cons),
    bestFor: parseItems(product.bestFor),
    avoidFor: parseItems(product.avoidIf),
    insightPositive: [],
    insightNegative: [],
    insightKeywords: [],
    sentimentScore: "—",
    insightVerified: false,
    specs: parseItems(product.specs)
      .map((s) => {
        const parts = s.split(":").map((p) => p.trim());
        return { label: parts[0] ?? s, value: parts[1] ?? "" };
      })
      .filter((s) => s.label && s.label !== "Chưa có thông số"),
    similarProducts: [],
    faqs: [],
    images,
    isHot: score >= 8.5,
    affiliateActive: hasAffiliate,
    relatedDeals,
    relatedReviews,
    affiliateLinks: product.affiliateLinks.map((l) => ({
      id: l.id,
      platform: l.platform,
      url: l.affiliateUrl ?? undefined,
      available: l.status === "ACTIVE",
      isPrimary: l.isPrimary,
    })),
  };
}

const statusBadgeMap: Record<ProductStatus, { label: string; className: string }> = {
  "dang-mua": { label: "Đáng mua", className: "border-emerald-200 bg-emerald-50 text-emerald-700" },
  "can-nhac": { label: "Cân nhắc", className: "border-amber-200 bg-amber-50 text-amber-700" },
  "khong-khuyen-nghi": { label: "Không khuyến nghị", className: "border-red-200 bg-red-50 text-red-700" },
};


type ProductDetailPageProps = {
  params: Promise<{ slug: string }>;
  searchParams: Promise<{ image?: string }>;
};

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const { slug } = await params;
  const product = await getProduct(slug);

  if (!product) {
    return { title: "Không tìm thấy sản phẩm | ReviewX", robots: { index: false, follow: false } };
  }

  return {
    title: `${product.name} | ReviewX`,
    description: `${product.name} - ${product.brand}. Điểm đáng mua ${product.worthScore}/10.`,
    alternates: { canonical: `/san-pham/${product.slug}` },
    openGraph: {
      title: `${product.name} | ReviewX`,
      description: `${product.name} - ${product.brand}. Điểm đáng mua ${product.worthScore}/10.`,
      url: `/san-pham/${product.slug}`,
      images: [{ url: product.images[0] || fallbackImage, width: 1200, height: 630 }],
    },
  };
}

export default async function ProductDetailPage({ params, searchParams }: { params: Promise<{ slug: string }>; searchParams: Promise<{ image?: string }> }) {
  const { slug } = await params;
  const { image: imageParam } = await searchParams;

  const product = await getProduct(slug);
  if (!product) notFound();

  const images = product.images.length > 0 ? product.images : [fallbackImage];
  const imageIndex = imageParam ? Number.parseInt(imageParam, 10) : Number.NaN;
  const activeImage = Number.isInteger(imageIndex) && imageIndex >= 0 && imageIndex < images.length ? images[imageIndex] : images[0];
  const statusMeta = statusBadgeMap[product.status];
  const activeAffiliateLinks = (product.affiliateLinks ?? []).filter((link) => link.available);
  const hasAffiliateLink = activeAffiliateLinks.length > 0;
  const primaryAffiliateLink = activeAffiliateLinks.find((link) => link.isPrimary) ?? activeAffiliateLinks[0];
  const verdictText = product.verdict ?? "Chưa có verdict chi tiết. Vui lòng xem thêm thông tin sản phẩm trước khi quyết định.";

  const productSchema = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.name,
    image: images,
    description: verdictText,
    brand: { "@type": "Brand", name: product.brand },
    category: product.category,
    ...(product.shopeeRating > 0 ? {
      aggregateRating: {
        "@type": "AggregateRating",
        ratingValue: product.shopeeRating,
        reviewCount: Number(product.soldCount.replace(/[^0-9]/g, "") || "1")
      }
    } : {}),
    ...(product.affiliateActive && hasAffiliateLink ? {
      offers: {
        "@type": "Offer",
        priceCurrency: "VND",
        availability: "https://schema.org/InStock",
        url: `https://reviewx.vn/recommends/${product.id}`
      }
    } : {}),
  };

  const faqSchema = {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: product.faqs.map((faq) => ({ "@type": "Question", name: faq.question, acceptedAnswer: { "@type": "Answer", text: faq.answer } })),
  };

  const breadcrumbSchema = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "Trang chủ", item: "https://reviewx.vn/" },
      { "@type": "ListItem", position: 2, name: product.category, item: `https://reviewx.vn/danh-muc/${product.categorySlug}` },
      { "@type": "ListItem", position: 3, name: product.name, item: `https://reviewx.vn/san-pham/${product.slug}` },
    ],
  };

  return (
    <PageContainer>
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(productSchema) }} />
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(faqSchema) }} />
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbSchema) }} />
      <div className="pb-24 md:pb-0">
      <nav className="mb-4 flex flex-wrap items-center gap-2 text-sm text-slate-500">
        <Link href="/" className="hover:text-slate-700">Trang chủ</Link>
        <span>/</span>
        <Link href="/danh-muc" className="hover:text-slate-700">Danh mục</Link>
        <span>/</span>
        <Link href={`/danh-muc/${product.categorySlug}`} className="hover:text-slate-700">{product.category}</Link>
        <span>/</span>
        <span className="font-medium text-slate-700">{product.name}</span>
      </nav>
      <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
        <div className="grid gap-8 lg:grid-cols-2">
          <ProductGallery
            slug={product.slug}
            name={product.name}
            isHot={product.isHot}
            images={images}
            activeImage={activeImage}
          />

          <div className="space-y-4">
            <span className="inline-flex items-center rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-semibold text-slate-600">
              {product.category}
            </span>
            <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">{product.name}</h1>
            <p className="text-sm text-slate-600">
              Thương hiệu <span className="font-semibold text-slate-900">{product.brand}</span>
            </p>

            <div className="flex flex-wrap items-center gap-3 text-sm text-slate-600">
              <span className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-1">Shopee {product.shopeeRating}/5</span>
              <span className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-1">Đã bán {product.soldCount}</span>
            </div>

            {product.currentPrice ? (
              <div className="space-y-1">
                <div className="flex flex-wrap items-end gap-2">
                  <p className="text-2xl font-bold text-slate-900">{product.currentPrice}</p>
                  {product.oldPrice ? <p className="text-sm text-slate-500 line-through">{product.oldPrice}</p> : null}
                  {product.discountPercent ? (
                    <span className="rounded-full border border-red-200 bg-red-50 px-2.5 py-1 text-xs font-semibold text-red-700">-{product.discountPercent}%</span>
                  ) : null}
                </div>
                {product.priceRange ? <p className="text-xs text-slate-500">Khoảng giá tham khảo: {product.priceRange}</p> : null}
              </div>
            ) : (
              <p className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-medium text-amber-700">Chưa có giá cập nhật</p>
            )}

            <div className="flex flex-wrap items-center gap-3">
              <ProductScoreBadge score={product.worthScore} />
              <span className={`inline-flex items-center rounded-xl border px-3 py-1 text-sm font-semibold ${statusMeta.className}`}>
                {statusMeta.label}
              </span>
            </div>

            {product.priceUpdatedAt && (
              <p className="text-xs text-slate-500 mt-1">
                Giá cập nhật lần cuối: {new Date(product.priceUpdatedAt).toLocaleDateString('vi-VN', {
                  day: '2-digit',
                  month: '2-digit',
                  year: 'numeric'
                })}
              </p>
            )}

            <VerdictCard
              verdictText={verdictText}
              buyIf={product.buyIf}
              considerIf={product.considerIf}
              avoidIf={product.avoidIf}
              buyPriceHint={product.buyPriceHint}
              considerPriceHint={product.considerPriceHint}
            />

            <p className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
              Lưu ý: ReviewX có thể nhận hoa hồng khi bạn mua hàng qua liên kết affiliate. Giá và khuyến mãi có thể thay đổi theo thời điểm.
            </p>

            {!product.affiliateActive || !hasAffiliateLink ? (
              <p className="rounded-2xl border border-amber-300 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-700">
                Hiện chưa có liên kết mua hàng đáng tin cậy cho sản phẩm này.
              </p>
            ) : (
              <section className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <h3 className="text-sm font-semibold text-slate-800">Liên kết mua hàng</h3>
                <div className="mt-3 grid gap-2 sm:grid-cols-2">
                  {activeAffiliateLinks.map((link) => (
                    <Link
                      key={link.platform}
                      href={`/go/product/${product.id}?platform=${encodeURIComponent(link.platform)}`}
                      className="inline-flex items-center justify-center rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
                    >
                      {link.platform}
                    </Link>
                  ))}
                </div>
              </section>
            )}

            <div className="flex flex-wrap gap-3">
              {product.affiliateActive && primaryAffiliateLink ? (
                <ShopeeCTAButton href={`/go/product/${product.id}`}>Xem giá tốt nhất</ShopeeCTAButton>
              ) : (
                <span className="inline-flex items-center justify-center rounded-2xl border border-amber-300 bg-amber-50 px-5 py-3 text-sm font-semibold text-amber-700">
                  Link affiliate tạm hết hiệu lực
                </span>
              )}

              <Link
                href="/so-sanh"
                className="inline-flex items-center justify-center rounded-2xl border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
              >
                So sánh sản phẩm
              </Link>

              <button className="inline-flex items-center justify-center rounded-2xl border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">
                Lưu sản phẩm
              </button>
            </div>
          </div>
        </div>
      </section>

      {(product.pros.length > 0 || product.cons.length > 0) && (
        <ProsConsGrid pros={product.pros} cons={product.cons} />
      )}

      <BestForAvoidIf bestFor={product.bestFor} avoidFor={product.avoidFor} />

      <ReviewInsightCard
        insightPositive={product.insightPositive}
        insightNegative={product.insightNegative}
        insightKeywords={product.insightKeywords}
        sentimentScore={product.sentimentScore}
        insightVerified={product.insightVerified}
      />

      <SpecsTable specs={product.specs} />

      <section className="mt-8 rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
        <h2 className="text-2xl font-bold tracking-tight text-slate-900">Sản phẩm tương tự</h2>

        <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {product.similarProducts.map((item) => (
            <article key={item.id} className="rounded-2xl border border-slate-200/70 bg-white p-3 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md">
              <div className="relative h-36 overflow-hidden rounded-xl border border-slate-200 bg-slate-100">
                <Image src={item.image} alt={item.name} fill className="object-cover" />
              </div>

              <span className="mt-3 inline-flex rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-medium text-slate-700">
                {item.reason}
              </span>

              <h3 className="mt-2 text-sm font-semibold text-slate-900">{item.name}</h3>

              <div className="mt-2 flex items-center justify-between">
                <ProductScoreBadge score={item.score} />
                <p className="text-sm font-semibold text-slate-900">{item.price}</p>
              </div>

              <Link
                href={`/go/product/${item.id}`}
                className="mt-3 inline-flex w-full items-center justify-center rounded-xl border border-orange-200 bg-orange-500 px-3 py-2 text-sm font-semibold text-white transition hover:bg-orange-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-orange-500"
              >
                Xem giá Shopee
              </Link>
            </article>
          ))}
        </div>
      </section>

      <FAQSection faqs={product.faqs} />

      <StickyMobileCTA
        priceRange={product.priceRange}
        worthScore={product.worthScore}
        productId={product.id}
        affiliateActive={product.affiliateActive}
        hasAffiliateLink={hasAffiliateLink}
      />
      </div>
    </PageContainer>
  );
}
