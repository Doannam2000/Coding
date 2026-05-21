import { redirect } from "next/navigation";
import { prisma } from "@/lib/prisma";

type GoProductPageProps = {
  params: Promise<{ productId: string }>;
  searchParams: Promise<{ platform?: string }>;
};

export default async function GoProductPage({ params, searchParams }: GoProductPageProps) {
  const { productId } = await params;
  const { platform } = await searchParams;

  const product = await prisma.product.findFirst({
    where: {
      status: "PUBLISHED",
      OR: [{ id: productId }, { slug: productId }],
    },
    select: {
      id: true,
      affiliateLinks: {
        where: { status: "ACTIVE" },
        select: { id: true, platform: true, affiliateUrl: true, isPrimary: true },
        orderBy: [{ isPrimary: "desc" }, { updatedAt: "desc" }],
      },
    },
  });

  const activeLinks = (product?.affiliateLinks ?? []).filter((link) => Boolean(link.affiliateUrl));
  if (activeLinks.length === 0) {
    redirect(`/link-error?reason=missing-product-affiliate&productId=${encodeURIComponent(productId)}`);
  }

  const platformFilter = platform?.trim().toLowerCase();
  const platformMatch = platformFilter
    ? activeLinks.find((link) => link.platform.trim().toLowerCase() === platformFilter)
    : undefined;
  const primaryLink = activeLinks.find((link) => link.isPrimary) ?? activeLinks[0];
  const selectedLink = platformMatch ?? primaryLink;

  redirect(`/recommends/${encodeURIComponent(selectedLink.id)}`);
}
