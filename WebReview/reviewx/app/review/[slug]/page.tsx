import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { prisma } from "@/lib/prisma";
import { BestForAvoidIf } from "@/components/best-for-avoid-if";
import { FAQSection } from "@/components/faq-section";
import { StickyMobileCTA } from "@/components/sticky-mobile-cta";
import { ProsConsGrid } from "@/components/pros-cons-grid";
import { ReviewInsightCard } from "@/components/review-insight-card";
import { SpecsTable } from "@/components/specs-table";
import { VerdictCard } from "@/components/verdict-card";
import { PageContainer, ProductScoreBadge, ShopeeCTAButton } from "@/components/ui";

type AffiliateLinkItem = {
  platform: string;
  url?: string;
  available: boolean;
  expired?: boolean;
};

type FaqItem = {
  question: string;
  answer: string;
};

type ScoreBreakdown = {
  design: number;
  performance: number;
  features: number;
  priceValue: number;
  durability: number;
  userExperience: number;
};

type RelatedReview = {
  id: string;
  slug: string;
  title: string;
  score: number | null;
  coverImage: string | null;
};

type ReviewPageData = {
  id: string;
  slug: string;
  title: string;
  summary: string;
  content: string;
  coverImage: string | null;
  score: number;
  shortVerdict: string;
  productId: string;
  productName: string;
  productSlug: string;
  category: string;
  categorySlug: string;
  publishedDate: string;
  updatedDate: string;
  readingTime: string;
  affiliateLinks: AffiliateLinkItem[];
  scoreBreakdown: ScoreBreakdown;
  specs: Array<{ label: string; value?: string }>;
  pros: string[];
  cons: string[];
  bestFor: string[];
  avoidFor: string[];
  faqs: FaqItem[];
  relatedReviews: RelatedReview[];
  insightPositive: string[];
  insightNegative: string[];
  insightKeywords: string[];
  sentimentScore: string;
  insightVerified: boolean;
};

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

function parseFaqText(faqText: string | null): FaqItem[] {
  if (!faqText) return [];
  const lines = faqText.split("\n").filter(Boolean);
  const faqs: FaqItem[] = [];
  let currentQuestion = "";
  let currentAnswer = "";
  for (const line of lines) {
    if (line.startsWith("Q:") || line.startsWith("Câu hỏi:")) {
      if (currentQuestion && currentAnswer) {
        faqs.push({ question: currentQuestion, answer: currentAnswer });
      }
      currentQuestion = line.replace(/^(Q:|Câu hỏi:)\s*/, "").trim();
      currentAnswer = "";
    } else if (line.startsWith("A:") || line.startsWith("Câu trả lời:")) {
      currentAnswer = line.replace(/^(A:|Câu trả lời:)\s*/, "").trim();
    } else {
      if (!currentQuestion) {
        currentQuestion = line.trim();
      } else if (!currentAnswer) {
        currentAnswer = line.trim();
      } else {
        currentAnswer += "\n" + line.trim();
      }
    }
  }
  if (currentQuestion && currentAnswer) {
    faqs.push({ question: currentQuestion, answer: currentAnswer });
  }
  return faqs;
}

function parseScoreBreakdown(raw: string | null): ScoreBreakdown {
  const defaultBreakdown: ScoreBreakdown = { design: 0, performance: 0, features: 0, priceValue: 0, durability: 0, userExperience: 0 };
  if (!raw) return defaultBreakdown;
  try {
    const parsed = JSON.parse(raw);
    if (typeof parsed === "object" && parsed !== null) {
      return { ...defaultBreakdown, ...parsed };
    }
  } catch {}
  return defaultBreakdown;
}

function scoreToStatus(score: number): "dang-mua" | "can-nhac" | "khong-khuyen-nghi" {
  if (score >= 8) return "dang-mua";
  if (score >= 6) return "can-nhac";
  return "khong-khuyen-nghi";
}

async function getReview(slug: string): Promise<ReviewPageData | null> {
  const review = await prisma.review.findUnique({
    where: { slug },
    include: {
      product: {
        select: {
          id: true,
          name: true,
          slug: true,
          specs: true,
          category: { select: { name: true, slug: true } },
        },
      },
      category: { select: { name: true, slug: true } },
      affiliateLinks: { where: { status: "ACTIVE" } },
      insights: { orderBy: { createdAt: "desc" }, take: 1 },
      faqs: { orderBy: { sortOrder: "asc" } },
    },
  });

  if (!review || review.status !== "PUBLISHED") return null;

  const scoreBreakdown = parseScoreBreakdown(review.scoreBreakdown);
  const faqs =
    review.faqs.length > 0
      ? review.faqs.map((f) => ({ question: f.question, answer: f.answer }))
      : parseFaqText(review.faqText);

  const relatedReviews = await prisma.review.findMany({
    where: {
      id: { not: review.id },
      status: "PUBLISHED",
      ...(review.categoryId
        ? { categoryId: review.categoryId }
        : review.productId
          ? { productId: review.productId }
          : {}),
    },
    take: 3,
    select: { id: true, slug: true, title: true, score: true, coverImage: true },
    orderBy: { updatedAt: "desc" },
  });

  const specs = parseItems(review.product?.specs)
    .map((s) => {
      const parts = s.split(":").map((p) => p.trim());
      return { label: parts[0] ?? s, value: parts[1] ?? "" };
    })
    .filter((s) => s.label && s.label !== "Chưa có thông số");

  const readingMinutes = Math.max(1, Math.ceil((review.content?.length ?? 0) / 500));

  return {
    id: review.id,
    slug: review.slug,
    title: review.title,
    summary: review.summary ?? "",
    content: review.content ?? "",
    coverImage: review.coverImage ?? null,
    score: review.score ?? 0,
    shortVerdict: review.verdict ?? "",
    productId: review.productId,
    productName: review.product?.name ?? "",
    productSlug: review.product?.slug ?? "",
    category: review.category?.name ?? review.product?.category?.name ?? "",
    categorySlug: review.category?.slug ?? review.product?.category?.slug ?? "",
    publishedDate: review.publishedAt?.toISOString().split("T")[0] ?? "",
    updatedDate: review.updatedAt.toISOString().split("T")[0],
    readingTime: `${readingMinutes} phút đọc`,
    affiliateLinks: review.affiliateLinks.map((l) => ({
      platform: l.platform,
      url: l.affiliateUrl ?? undefined,
      available: l.status === "ACTIVE",
    })),
    scoreBreakdown,
    specs,
    pros: parseItems(review.pros),
    cons: parseItems(review.cons),
    bestFor: parseItems(review.bestFor),
    avoidFor: parseItems(review.avoidIf),
    faqs,
    relatedReviews,
    insightPositive: review.insights?.[0]?.positive ? [review.insights[0].positive] : [],
    insightNegative: review.insights?.[0]?.negative ? [review.insights[0].negative] : [],
    insightKeywords: review.insights?.[0]?.keywords ? [review.insights[0].keywords] : [],
    sentimentScore: "—",
    insightVerified: false,
  };
}

function ReviewCTA({ review, label }: { review: ReviewPageData; label: string }) {
  const availableLinks = review.affiliateLinks.filter((link) => link.available && !link.expired);

  return (
    <div className="rounded-2xl border border-orange-200/70 bg-orange-50 p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm font-semibold text-slate-900">{label}</p>
        <ProductScoreBadge score={review.score} />
      </div>
      <p className="mt-2 text-sm text-slate-700">
        Điểm đánh giá: <span className="font-semibold text-slate-900">{review.score.toFixed(1)} / 10</span>
      </p>
      {availableLinks.length > 0 ? (
        <div className="mt-3 grid gap-2 sm:grid-cols-2">
          {availableLinks.map((link) => (
            <Link
              key={link.platform}
              href={`/go/product/${review.productSlug}`}
              className="inline-flex items-center justify-center rounded-xl border border-orange-200 bg-white px-4 py-2 text-sm font-semibold text-orange-700 transition hover:bg-orange-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-orange-500"
            >
              Xem giá {link.platform}
            </Link>
          ))}
        </div>
      ) : (
        <p className="mt-3 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">
          Hiện chưa có affiliate link khả dụng cho sản phẩm này.
        </p>
      )}
    </div>
  );
}

type ReviewPageProps = {
  params: Promise<{ slug: string }>;
};

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const { slug } = await params;
  const item = await prisma.review.findUnique({
    where: { slug },
    select: { title: true, verdict: true, slug: true, coverImage: true, status: true },
  });
  if (!item || item.status !== "PUBLISHED") {
    return {
      title: "Không tìm thấy review | ReviewX",
      robots: { index: false, follow: false },
    };
  }

  return {
    title: `${item.title} | ReviewX`,
    description: item.verdict ?? undefined,
    alternates: { canonical: `/review/${item.slug}` },
    openGraph: {
      title: `${item.title} | ReviewX`,
      description: item.verdict ?? undefined,
      url: `/review/${item.slug}`,
      images: [{ url: item.coverImage ?? "https://images.unsplash.com/photo-1519389950473-47ba0277781c?q=80&w=1200&auto=format&fit=crop", width: 1200, height: 630 }],
    },
  };
}

export default async function ReviewPage({ params }: ReviewPageProps) {
  const { slug } = await params;
  const review = await getReview(slug);

  if (!review) {
    notFound();
  }

  const statusBadge = scoreToStatus(review.score);
  const statusMeta = {
    "dang-mua": { label: "Đáng mua", className: "border-emerald-200 bg-emerald-50 text-emerald-700" },
    "can-nhac": { label: "Cân nhắc", className: "border-amber-200 bg-amber-50 text-amber-700" },
    "khong-khuyen-nghi": { label: "Không khuyến nghị", className: "border-red-200 bg-red-50 text-red-700" },
  }[statusBadge];

  const activeAffiliateLinks = review.affiliateLinks.filter((link) => link.available);
  const hasAffiliateLink = activeAffiliateLinks.length > 0;

 const reviewSchema = {
   "@context": "https://schema.org",
   "@type": "Review",
   itemReviewed: {
     "@type": "Product",
     name: review.productName,
   },
   author: { "@type": "Organization", name: "ReviewX" },
   reviewRating: { "@type": "Rating", ratingValue: review.score, bestRating: 10 },
   reviewBody: review.shortVerdict,
 };

 const articleSchema = {
   "@context": "https://schema.org",
   "@type": "Article",
   headline: review.title,
   author: { "@type": "Organization", name: "ReviewX" },
   datePublished: review.publishedDate,
   dateModified: review.updatedDate,
   description: review.summary,
 };

 const faqSchema = {
   "@context": "https://schema.org",
   "@type": "FAQPage",
   mainEntity: review.faqs.map((faq) => ({
     "@type": "Question" as const,
     name: faq.question,
     acceptedAnswer: { "@type": "Answer" as const, text: faq.answer },
   })),
 };

 const breadcrumbSchema = {
   "@context": "https://schema.org",
   "@type": "BreadcrumbList",
   itemListElement: [
     { "@type": "ListItem", position: 1, name: "Trang chủ", item: "https://reviewx.vn/" },
     { "@type": "ListItem", position: 2, name: "Review", item: "https://reviewx.vn/tim-kiem" },
     { "@type": "ListItem", position: 3, name: review.productName, item: `https://reviewx.vn/review/${review.slug}` },
   ],
 };

 return (
   <PageContainer>
     <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(reviewSchema) }} />
     <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(articleSchema) }} />
     {review.faqs.length > 0 && <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(faqSchema) }} />}
     <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbSchema) }} />
      <div className="space-y-8">
        <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-sm sm:p-8">
          <nav className="mb-4 flex flex-wrap items-center gap-2 text-sm text-slate-500">
            <Link href="/" className="hover:text-slate-700">Trang chủ</Link>
            <span>/</span>
            <Link href="/tim-kiem" className="hover:text-slate-700">Review</Link>
            <span>/</span>
            <span className="font-medium text-slate-700">{review.productName}</span>
          </nav>

          <div className="flex flex-wrap items-center gap-3">
            <span className="inline-flex items-center rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-semibold text-slate-700">
              {review.category}
            </span>
            <span className="text-xs text-slate-500">Đăng: {review.publishedDate}</span>
            <span className="text-xs text-slate-500">Cập nhật: {review.updatedDate}</span>
            <span className="text-xs text-slate-500">{review.readingTime}</span>
          </div>

          <h1 className="mt-4 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">{review.title}</h1>
          <p className="mt-3 text-sm leading-6 text-slate-600">{review.summary}</p>

          <div className="mt-4 overflow-hidden rounded-2xl border border-slate-200 bg-slate-100">
            {review.coverImage ? (
              <Image src={review.coverImage} alt={review.title} width={800} height={450} className="h-56 w-full object-cover sm:h-72" loading="lazy" />
            ) : (
              <div className="h-56 w-full bg-slate-200 sm:h-72" />
            )}
          </div>

          <p className="mt-4 rounded-2xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm leading-6 text-slate-700">
            Bài viết có thể chứa liên kết tiếp thị liên kết. Khi bạn mua hàng qua liên kết này, chúng tôi có thể nhận hoa hồng mà không làm thay đổi giá bạn phải trả.
          </p>

          <article className="mt-6 rounded-2xl border border-slate-200/70 bg-white p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-lg font-semibold text-slate-900">{review.productName}</h2>
              <ProductScoreBadge score={review.score} />
            </div>
            <p className="mt-3 text-sm leading-6 text-slate-600">{review.summary}</p>
            <p className="mt-3 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{review.shortVerdict}</p>

            <div className="mt-4 flex flex-wrap gap-3">
              <Link
                href={`/san-pham/${review.productSlug}`}
                className="inline-flex items-center justify-center rounded-2xl border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
              >
                Xem sản phẩm
              </Link>
              {hasAffiliateLink && (
                <Link
                  href={`/go/product/${review.productSlug}`}
                  className="inline-flex items-center justify-center rounded-2xl bg-orange-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-orange-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-orange-500"
                >
                  Xem giá tốt nhất
                </Link>
              )}
            </div>

            <div className="mt-4">
              <ReviewCTA review={review} label="Ưu đãi sau phần tóm tắt" />
            </div>
          </article>
        </section>

        {review.content ? (
          <article
            className="rounded-2xl border border-slate-200/70 bg-white/90 p-5 shadow-sm sm:p-8 prose prose-slate max-w-none"
            dangerouslySetInnerHTML={{ __html: review.content }}
          />
        ) : null}

        <section className="grid gap-8 lg:grid-cols-[260px_minmax(0,1fr)]">
          <aside className="hidden lg:block">
            <div className="sticky top-24 rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm">
              <p className="text-sm font-semibold text-slate-900">Mục lục bài viết</p>
              <p className="mt-3 text-xs text-slate-500">Nội dung được tải từ cơ sở dữ liệu.</p>
            </div>
          </aside>

          <div className="space-y-4">
            <article className="rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm">
              <h3 className="text-xl font-semibold text-slate-900">Bảng điểm chi tiết</h3>
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                {[
                  { label: "Design", value: review.scoreBreakdown.design },
                  { label: "Performance", value: review.scoreBreakdown.performance },
                  { label: "Features", value: review.scoreBreakdown.features },
                  { label: "Price/value", value: review.scoreBreakdown.priceValue },
                  { label: "Durability", value: review.scoreBreakdown.durability },
                  { label: "User experience", value: review.scoreBreakdown.userExperience },
                  { label: "Overall score", value: review.score },
                ].map((item) => (
                  <div key={item.label} className="rounded-xl border border-slate-200 bg-slate-50 p-3">
                    <p className="text-sm text-slate-600">{item.label}</p>
                    <p className="mt-1 text-lg font-semibold text-slate-900">{item.value.toFixed(1)} / 10</p>
                  </div>
                ))}
              </div>
            </article>

            <VerdictCard
              verdictText={review.shortVerdict}
              buyIf={review.bestFor.length > 0 ? review.bestFor[0] : review.summary}
              considerIf=""
              avoidIf={review.avoidFor.length > 0 ? review.avoidFor[0] : ""}
              buyPriceHint=""
              considerPriceHint=""
            />

            {(review.pros.length > 0 || review.cons.length > 0) && (
              <ProsConsGrid pros={review.pros} cons={review.cons} />
            )}

            {review.specs.length > 0 && (
              <article className="rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm">
                <h3 className="text-xl font-semibold text-slate-900">Thông số kỹ thuật</h3>
                <ul className="mt-3 space-y-2 text-sm text-slate-600">
                  {review.specs.map((item) => (
                    <li key={item.label} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
                      <span className="font-semibold text-slate-700">{item.label}:</span> {item.value || "—"}
                    </li>
                  ))}
                </ul>
              </article>
            )}

            {review.faqs.length > 0 && <FAQSection faqs={review.faqs} />}

            {review.relatedReviews.length > 0 && (
              <article className="rounded-2xl border border-slate-200/70 bg-white p-5 shadow-sm">
                <h3 className="text-xl font-semibold text-slate-900">Bài review liên quan</h3>
                <div className="mt-3 grid gap-3 sm:grid-cols-2">
                  {review.relatedReviews.map((item) => (
                    <Link key={item.slug} href={`/review/${item.slug}`} className="rounded-xl border border-slate-200 bg-slate-50 p-3 transition hover:bg-slate-100">
                      <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                      <p className="mt-1 text-xs text-slate-600">Điểm: {item.score?.toFixed(1) ?? "—"} / 10</p>
                    </Link>
                  ))}
                </div>
              </article>
            )}

            <ReviewInsightCard
              insightPositive={review.insightPositive}
              insightNegative={review.insightNegative}
              insightKeywords={review.insightKeywords}
              sentimentScore={review.sentimentScore}
              insightVerified={review.insightVerified}
            />
          </div>
        </section>

        {hasAffiliateLink && (
          <StickyMobileCTA
            priceRange=""
            worthScore={review.score}
            productId={review.productId}
            affiliateActive={hasAffiliateLink}
            hasAffiliateLink={hasAffiliateLink}
          />
        )}
      </div>
    </PageContainer>
  );
}