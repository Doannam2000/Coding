import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { fail, ok } from "@/lib/api-response";
import { isSupportedShopeeHost, normalizePaginationParams, parseHttpUrl } from "@/lib/validators";

type ShortLinkStatus = "PENDING" | "RESOLVED" | "CONVERTED" | "FAILED" | "CAPTCHA_REQUIRED" | "DUPLICATE";
type ConvertPayload = { inputUrl?: string };

function extractIds(resolvedUrl: string) {
  const digits = resolvedUrl.replace(/\D/g, "");
  return {
    shopId: digits.slice(0, 6) || null,
    itemId: digits.slice(6, 12) || null,
  };
}

function deriveStatus(url: URL): ShortLinkStatus {
  if (url.searchParams.get("captcha") === "1") return "CAPTCHA_REQUIRED";
  if (url.searchParams.get("redirect_fail") === "1") return "FAILED";
  if (url.searchParams.get("expired") === "1") return "FAILED";
  return url.hostname === "shopee.vn" ? "CONVERTED" : "RESOLVED";
}

export async function GET(request: NextRequest) {
  const { limit, skip, page } = normalizePaginationParams(request.nextUrl.searchParams);
  const items = await prisma.shortLink.findMany({
    orderBy: { createdAt: "desc" },
    skip,
    take: limit,
    include: { affiliateLink: true },
  });
  return NextResponse.json(ok({ page, limit, items }));
}

export async function POST(request: NextRequest) {
  const body = (await request.json()) as ConvertPayload;
  const inputUrl = body.inputUrl?.trim() ?? "";

  if (!inputUrl) {
    return NextResponse.json(fail("Vui lòng nhập link Shopee cần chuyển đổi."), { status: 400 });
  }

  const parsed = parseHttpUrl(inputUrl);
  if (!parsed) {
    return NextResponse.json(fail("URL không hợp lệ."), { status: 400 });
  }

  if (!isSupportedShopeeHost(parsed.hostname)) {
    return NextResponse.json(fail("Chỉ hỗ trợ domain s.shopee.vn, shopee.ee, shp.ee hoặc shopee.vn."), { status: 400 });
  }

  const resolvedUrl = parsed.toString();
  const { shopId, itemId } = extractIds(resolvedUrl);

  const duplicated = await prisma.shortLink.findFirst({ where: { platform: parsed.hostname, shopId: shopId ?? undefined, itemId: itemId ?? undefined } });
  if (duplicated) {
    const created = await prisma.shortLink.create({
      data: {
        inputUrl,
        resolvedUrl,
        platform: parsed.hostname,
        shopId,
        itemId,
        status: "DUPLICATE",
        internalTrackingUrl: duplicated.internalTrackingUrl,
        errorMessage: "Link đã tồn tại trong lịch sử chuyển đổi.",
      },
    });
    return NextResponse.json(ok(created));
  }

  const matchedAffiliate =
    shopId && itemId
      ? await prisma.shortLink
          .findFirst({
            where: {
              shopId,
              itemId,
              affiliateLinkId: { not: null },
            },
            orderBy: { createdAt: "desc" },
            include: { affiliateLink: true },
          })
          .then((row: { affiliateLink: { id: string; internalUrl: string | null } | null } | null) => row?.affiliateLink ?? null)
      : null;

  const status = deriveStatus(parsed);
  const created = await prisma.shortLink.create({
    data: {
      inputUrl,
      resolvedUrl,
      platform: parsed.hostname,
      shopId,
      itemId,
      status,
      affiliateLinkId: matchedAffiliate?.id,
      internalTrackingUrl: matchedAffiliate?.internalUrl ?? null,
      errorMessage: status === "CAPTCHA_REQUIRED" ? "Phát hiện captcha/challenge. Vui lòng xử lý thủ công." : null,
    },
    include: { affiliateLink: true },
  });

  return NextResponse.json(ok(created));
}
