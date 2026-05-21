import { redirect } from "next/navigation";

type GoReviewPageProps = {
  params: Promise<{ reviewId: string }>;
};

export default async function GoReviewPage({ params }: GoReviewPageProps) {
  const { reviewId } = await params;
  redirect(`/review/${encodeURIComponent(reviewId)}`);
}
