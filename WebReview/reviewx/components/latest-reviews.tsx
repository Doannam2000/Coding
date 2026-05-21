import { ReviewCard } from "./review-card";
import { EmptyState, LoadingSkeleton } from "./ui";

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

type LatestReviewsSectionProps = {
  reviews?: ReviewItem[];
  isLoading?: boolean;
  error?: string;
};

export function LatestReviewsSection({ reviews: propReviews, isLoading, error }: LatestReviewsSectionProps) {
  const latestReviews = propReviews ?? [];

  if (isLoading) {
    return (
      <section className="mt-10 space-y-4">
        <h2 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">Bài viết review mới nhất</h2>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 3 }).map((_, idx) => (
            <div key={`ls-${idx}`} className="rounded-2xl border border-slate-200/70 bg-white p-4 shadow-sm">
              <LoadingSkeleton className="h-44 w-full rounded-t-2xl" />
              <div className="mt-3 space-y-2">
                <LoadingSkeleton className="h-4 w-20" />
                <LoadingSkeleton className="h-5 w-full" />
                <LoadingSkeleton className="h-4 w-3/4" />
              </div>
            </div>
          ))}
        </div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="mt-10 space-y-4">
        <h2 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">Bài viết review mới nhất</h2>
        <EmptyState title="Không tải được review" message={error} />
      </section>
    );
  }

  if (latestReviews.length === 0) {
    return (
      <section className="mt-10 space-y-4">
        <h2 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">Bài viết review mới nhất</h2>
        <div className="flex flex-col items-center justify-center rounded-3xl border border-slate-200/70 bg-white p-12 text-center">
          <p className="text-lg font-medium text-slate-400">Chưa có bài review nào.</p>
          <p className="mt-1 text-sm text-slate-500">Quay lại sau để cập nhật bài viết mới nhất.</p>
        </div>
      </section>
    );
  }

  return (
    <section className="mt-10 space-y-4">
      <h2 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">Bài viết review mới nhất</h2>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {latestReviews.map((item) => (
          <ReviewCard
            key={item.slug}
            slug={item.slug}
            title={item.title}
            excerpt={item.excerpt}
            category={item.category}
            score={item.score}
            author={item.author}
            publishedDate={item.publishedDate}
            updatedDate={item.updatedDate}
            readingTime={item.readingTime}
            coverImage={item.coverImage}
          />
        ))}
      </div>
    </section>
  );
}
