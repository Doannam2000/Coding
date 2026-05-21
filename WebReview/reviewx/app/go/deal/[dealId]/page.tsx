import { redirect } from "next/navigation";
import { prisma } from "@/lib/prisma";

type GoDealPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function GoDealPage({ params }: GoDealPageProps) {
  const { slug } = await params;

  // Try to find deal by id or by product slug
  const deal = await prisma.deal.findFirst({
    where: {
      OR: [
        { id: slug },
        { product: { slug } }
      ]
    },
    select: {
      id: true,
      affiliateLinkId: true,
      affiliateLink: {
        select: {
          id: true,
          status: true,
          affiliateUrl: true,
        },
      },
    },
  });

  const affiliateLink = deal?.affiliateLink;
  if (!affiliateLink || affiliateLink.status !== "ACTIVE" || !affiliateLink.affiliateUrl) {
    redirect(`/link-error?reason=missing-deal-affiliate&dealId=${encodeURIComponent(slug)}`);
  }

  // Log click (fire and forget)
  try {
    await fetch(`${process.env.NEXT_PUBLIC_SITE_URL || ""}/api/admin/analytics/events`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        type: "deal_click",
        dealId: deal.id,
        affiliateLinkId: affiliateLink.id,
      }),
    });
  } catch {}

  redirect(affiliateLink.affiliateUrl);
}
