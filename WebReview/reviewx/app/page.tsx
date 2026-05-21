"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { CategoryStrip } from "@/components/category-strip";
import { FeaturedWorthProductCard } from "@/components/featured-worth-product-card";
import { HeroSearch } from "@/components/hero-search";
import { HotDealPanel } from "@/components/hot-deal-panel";
import { LatestReviewsSection } from "@/components/latest-reviews";
import { TrustFeatureStrip } from "@/components/trust-feature-strip";
import { PageContainer, SectionHeader, LoadingSkeleton, EmptyState } from "@/components/ui";

type MiniProduct = {
  name: string;
  price: string;
  href: string;
};

type FeaturedProductData = {
  id: string;
  slug: string;
  name: string;
  score: number;
  imageSrc: string;
  imageAlt: string;
  discountLabel: string;
  productName: string;
  productHref: string;
  category: string;
  badge: "Đáng mua" | "Giá tốt" | "Best budget" | "Hot deal";
  pros: string[];
  cons: string[];
  currentPrice: string;
  oldPrice: string;
  shopeeHref: string;
  reviewHref: string;
  miniProducts: MiniProduct[];
};

type ReviewItem = {
  slug: string;
  title: string;
  excerpt: string;
  category: string;
  score: number;
  author: string;
  publishedDate: string;
  updatedDate: string;
  readingTime: string;
  coverImage: string;
};

type DealItem = {
  id: string;
  name: string;
  discount: string;
  currentPrice: string;
  oldPrice: string;
  thumb: string;
  active: boolean;
  platform: string;
  hasAffiliate: boolean;
  hasReview: boolean;
  reviewHref: string | null;
};

export default function Home() {
  const [featured, setFeatured] = useState<FeaturedProductData | null>(null);
  const [reviews, setReviews] = useState<ReviewItem[]>([]);
  const [deals, setDeals] = useState<DealItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    async function loadData() {
      try {
        const [featuredRes, reviewsRes, dealsRes] = await Promise.all([
          fetch("/api/public/featured-product"),
          fetch("/api/public/reviews"),
          fetch("/api/public/deals?status=active&limit=10"),
        ]);

        if (!cancelled) {
          const featuredJson = await featuredRes.json();
          const reviewsJson = await reviewsRes.json();
          const dealsJson = await dealsRes.json();

          if (featuredJson.success && featuredJson.data) {
            const f = featuredJson.data;
            const discount =
              f.oldPrice && f.currentPrice
                ? `-${Math.round((1 - parseFloat(f.currentPrice.replace(/\D/g, "")) / parseFloat(f.oldPrice.replace(/\D/g, ""))) * 100)}%`
                : "";
            setFeatured({
              id: f.id,
              slug: f.slug,
              name: f.name,
              score: f.score,
              imageSrc: f.coverImage || "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?q=80&w=1200&auto=format&fit=crop",
              imageAlt: f.name,
              discountLabel: discount,
              productName: f.name,
              productHref: `/san-pham/${f.slug}`,
              category: f.category,
              badge: "Đáng mua",
              pros: f.pros || [],
              cons: f.cons || [],
              currentPrice: f.currentPrice || "—",
              oldPrice: f.oldPrice || "",
              shopeeHref: `/go/product/${f.slug}`,
              reviewHref: f.reviews?.[0]?.slug ? `/review/${f.reviews[0].slug}` : "",
              miniProducts: (f.deals || []).slice(0, 3).map((d: any) => ({
                name: d.title || d.name || "",
                price: d.currentPrice || "—",
                href: `/go/deal/${d.id}`,
              })),
            });
          }

          if (reviewsJson.success && reviewsJson.data?.items) {
            setReviews(reviewsJson.data.items);
          }

          if (dealsJson.success && dealsJson.data?.items) {
            setDeals(dealsJson.data.items);
          }

          setIsLoading(false);
        }
      } catch (e: any) {
        if (!cancelled) {
          setError(e.message || "Không tải được dữ liệu");
          setIsLoading(false);
        }
      }
    }

    loadData();
    return () => {
      cancelled = true;
    };
  }, []);


  return (
    <PageContainer>
      <section className="glass-strong overflow-hidden rounded-3xl border border-slate-200/50 p-5 shadow-lg sm:p-8">
        <div className="grid gap-6 lg:grid-cols-2 lg:items-start">
          <div className="min-w-0">
            <HeroSearch />
          </div>

          <div className="min-w-0">
            {isLoading ? (
              <div className="glass space-y-4 rounded-2xl border border-slate-200/50 p-4 shadow-sm">
                <LoadingSkeleton className="h-4 w-32" />
                <LoadingSkeleton className="mt-3 h-7 w-20" />
              </div>
            ) : error ? (
              <EmptyState title="Lỗi tải dữ liệu" message={error} />
            ) : featured ? (
              <FeaturedWorthProductCard
                title="Sản phẩm đáng mua hôm nay"
                score={featured.score}
                imageSrc={featured.imageSrc}
                imageAlt={featured.imageAlt}
                discountLabel={featured.discountLabel}
                productName={featured.productName}
                productHref={featured.productHref}
                category={featured.category}
                badge={featured.badge}
                pros={featured.pros}
                cons={featured.cons}
                currentPrice={featured.currentPrice}
                oldPrice={featured.oldPrice}
                shopeeHref={featured.shopeeHref}
                reviewHref={featured.reviewHref}
                miniProducts={featured.miniProducts}
              />
            ) : null}
          </div>
        </div>
      </section>

      <CategoryStrip />

      <LatestReviewsSection
        reviews={reviews}
        isLoading={isLoading && !error}
        error={error}
      />

      <HotDealPanel
        deals={deals}
        isLoading={isLoading && !error}
        error={error}
      />

      <TrustFeatureStrip />
    </PageContainer>
  );
}
