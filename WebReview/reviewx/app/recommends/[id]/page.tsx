import { headers } from "next/headers";
import { redirect } from "next/navigation";
import { prisma } from "@/lib/prisma";

type DeviceType = "mobile" | "tablet" | "desktop" | "unknown";

function detectDeviceType(userAgent: string): DeviceType {
  const ua = userAgent.toLowerCase();
  if (!ua || ua === "unknown") return "unknown";
  if (/ipad|tablet|playbook|silk/.test(ua)) return "tablet";
  if (/mobi|android|iphone|ipod|blackberry|phone/.test(ua)) return "mobile";
  return "desktop";
}

const allowedAffiliateHosts = ["shopee.vn", "s.shopee.vn", "shopee.ee", "shp.ee", "lazada.vn", "tiki.vn"];

function isAllowedAffiliateHost(hostname: string) {
  return allowedAffiliateHosts.some((host) => hostname === host || hostname.endsWith(`.${host}`));
}

function inferLinkType(id: string) {
  if (id.startsWith("deal-")) return "deal";
  if (id.includes("review")) return "review";
  return "product";
}

function makeLinkErrorUrl(id: string) {
  return `/link-error?id=${encodeURIComponent(id)}&type=${inferLinkType(id)}`;
}

function parseSafeAffiliateUrl(raw: string) {
  try {
    const url = new URL(raw);
    if (url.protocol !== "http:" && url.protocol !== "https:") return null;
    if (!isAllowedAffiliateHost(url.hostname.toLowerCase())) return null;
    return url;
  } catch {
    return null;
  }
}

type RecommendPageProps = {
  params: Promise<{ id: string }>;
};

function toIpHash(value: string) {
  let hash = 0;
  for (let i = 0; i < value.length; i += 1) {
    hash = (hash << 5) - hash + value.charCodeAt(i);
    hash |= 0;
  }
  return `ip_${Math.abs(hash)}`;
}

export default async function RecommendRedirectPage({ params }: RecommendPageProps) {
  const { id } = await params;

  const link = await prisma.affiliateLink.findFirst({
    where: { OR: [{ id }, { internalUrl: `/recommends/${id}` }] },
    select: { id: true, affiliateUrl: true, status: true, productId: true, reviewId: true, platform: true },
  });

  if (!link || link.status !== "ACTIVE" || !link.affiliateUrl) {
    redirect(makeLinkErrorUrl(id));
  }

  const parsedTarget = parseSafeAffiliateUrl(link.affiliateUrl);

  if (!parsedTarget) {
    redirect(makeLinkErrorUrl(id));
  }

  const headerList = await headers();
  const userAgent = headerList.get("user-agent") ?? "unknown";
  const referrer = headerList.get("referer") ?? "direct";
  const forwardedFor = headerList.get("x-forwarded-for") ?? "unknown";
  const sourcePage = headerList.get("x-source-page") ?? referrer;
  const utmSource = parsedTarget.searchParams.get("utm_source");
  const utmMedium = parsedTarget.searchParams.get("utm_medium");
  const utmCampaign = parsedTarget.searchParams.get("utm_campaign");
  const utmTerm = parsedTarget.searchParams.get("utm_term");
  const utmContent = parsedTarget.searchParams.get("utm_content");

  try {
    await prisma.$transaction([
      prisma.clickEvent.create({
        data: {
          affiliateLinkId: link.id,
          productId: link.productId,
          reviewId: link.reviewId,
          platform: link.platform,
          sourcePage,
          deviceType: detectDeviceType(userAgent),
          utmSource,
          utmMedium,
          utmCampaign,
          utmTerm,
          utmContent,
          referrer,
          userAgent,
          ipHash: toIpHash(forwardedFor),
        },
      }),
      prisma.affiliateLink.update({ where: { id: link.id }, data: { clickCount: { increment: 1 } } }),
    ]);
  } catch {
  }

  redirect(parsedTarget.toString());
}
