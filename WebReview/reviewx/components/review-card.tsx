import Image from "next/image";
import Link from "next/link";
import { ProductScoreBadge } from "./ui";
import { cardStyles, buttonStyles } from "@/lib/design-system";

type ReviewCardProps = {
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

export function ReviewCard({ slug, title, excerpt, category, score, author, publishedDate, updatedDate, readingTime, coverImage }: ReviewCardProps) {
  return (
    <article className={`group flex h-full flex-col ${cardStyles.interactive} overflow-hidden p-0`}>
      <div className="relative overflow-hidden border-b border-slate-200/50 bg-slate-50">
        <Image
          src={coverImage}
          alt={title}
          width={1200}
          height={800}
          className="h-44 w-full object-cover transition-transform duration-300 group-hover:scale-105"
          loading="lazy"
          placeholder="blur"
          blurDataURL="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTIwMCIgaGVpZ2h0PSI4MDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0iI2YwZjBmMCIvPjwvc3ZnPg=="
        />
      </div>

      <div className="flex flex-1 flex-col gap-3 p-4">
        <div className="flex items-center justify-between gap-2">
          <span className="rounded-full border border-blue-200/70 bg-blue-50/80 px-3 py-1 text-xs font-semibold text-blue-700">{category}</span>
          <ProductScoreBadge score={score} />
        </div>

        <Link href={`/review/${slug}`} className="line-clamp-2 text-base font-semibold text-slate-900 transition-colors hover:text-blue-600">
          {title}
        </Link>

        <p className="line-clamp-3 text-sm leading-relaxed text-slate-600">{excerpt}</p>

        <div className="mt-auto space-y-3 pt-2">
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
            <span className="font-medium text-slate-700">{author}</span>
            <span aria-hidden="true">•</span>
            <time dateTime={publishedDate}>{publishedDate}</time>
            <span aria-hidden="true">•</span>
            <span>Cập nhật {updatedDate}</span>
            <span aria-hidden="true">•</span>
            <span>{readingTime}</span>
          </div>

          <div className="flex items-center justify-end">
            <Link
              href={`/review/${slug}`}
              className={`${buttonStyles.secondary} px-4 py-2 text-xs`}
              aria-label={`Đọc bài review ${title}`}
            >
              Đọc review
            </Link>
          </div>
        </div>
      </div>
    </article>
  );
}
