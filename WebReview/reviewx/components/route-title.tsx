"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";

const DEFAULT_TITLE = "ReviewX";
const DEFAULT_DESCRIPTION = "Check trÆ°á»›c khi mua. Mua gÃ¬ cÅ©ng Ä‘Ã¡ng.";
const SITE_ORIGIN = "https://reviewx.vn";
const DEFAULT_OG_IMAGE = "https://images.unsplash.com/photo-1519389950473-47ba0277781c?q=80&w=1200&auto=format&fit=crop";
const DEFAULT_TWITTER_CARD = "summary_large_image";

const STATIC_TITLES: Record<string, string> = {
  "/": "Trang chủ | ReviewX",
  "/affiliate-policy": "Chính sách Affiliate | ReviewX",
  "/privacy-policy": "Chính sách quyền riêng tư | ReviewX",
  "/terms": "Điều khoản sử dụng | ReviewX",
  "/contact": "Liên hệ | ReviewX",
  "/danh-muc": "Danh mục sản phẩm | ReviewX",
  "/deals": "Deal tốt hôm nay | ReviewX",
  "/search": "Tìm kiếm | ReviewX",
  "/tim-kiem": "Tìm kiếm nâng cao | ReviewX",
  "/so-sanh": "So sánh sản phẩm | ReviewX",
  "/cong-cu/chon-san-pham": "Công cụ chọn sản phẩm | ReviewX",
  "/link-error": "Liên kết không hợp lệ | ReviewX",
  "/admin": "Admin Dashboard | ReviewX",
  "/admin/login": "Admin Login | ReviewX",
  "/admin/products": "Admin Products | ReviewX",
  "/admin/products/new": "Admin Create Product | ReviewX",
  "/admin/reviews": "Admin Reviews | ReviewX",
  "/admin/reviews/new": "Admin Create Review | ReviewX",
  "/admin/deals": "Admin Deals | ReviewX",
  "/admin/categories": "Admin Categories | ReviewX",
  "/admin/brands": "Admin Brands | ReviewX",
  "/admin/affiliate-links": "Admin Affiliate Links | ReviewX",
  "/admin/analytics": "Admin Analytics | ReviewX",
  "/admin/ai-drafts": "Admin AI Drafts | ReviewX",
  "/admin/crawler": "Admin Crawler Jobs | ReviewX",
  "/admin/tools/short-link-converter": "Admin Short Link Converter | ReviewX",
};

const STATIC_DESCRIPTIONS: Record<string, string> = {
  "/": "ReviewX giÃºp báº¡n check sáº£n pháº©m, so sÃ¡nh giÃ¡, review vÃ  deal trÆ°á»›c khi mua.",
  "/affiliate-policy": "ChÃ­nh sÃ¡ch affiliate vÃ  cam káº¿t minh báº¡ch cá»§a ReviewX.",
  "/privacy-policy": "ChÃ­nh sÃ¡ch quyá»n riÃªng tÆ° vÃ  cÃ¡ch ReviewX xá»­ lÃ½ dá»¯ liá»‡u ngÆ°á»i dÃ¹ng.",
  "/terms": "Äiá»u khoáº£n sá»­ dá»¥ng website ReviewX.",
  "/contact": "LiÃªn há»‡ ReviewX Ä‘á»ƒ há»— trá»£, pháº£n há»“i hoáº·c gá»­i Ä‘á» xuáº¥t ná»™i dung.",
  "/danh-muc": "KhÃ¡m phÃ¡ cÃ¡c danh má»¥c sáº£n pháº©m Ä‘Æ°á»£c ReviewX Ä‘Ã¡nh giÃ¡.",
  "/deals": "Tá»•ng há»£p deal giá»›i háº¡n, mÃ£ giáº£m giÃ¡ vÃ  liÃªn káº¿t mua nhanh.",
  "/search": "TÃ¬m nhanh sáº£n pháº©m vÃ  bÃ i review trÃªn ReviewX.",
  "/tim-kiem": "TÃ¬m kiáº¿m nÃ¢ng cao theo tá»« khÃ³a, danh má»¥c vÃ  Ä‘iá»u kiá»‡n.",
  "/so-sanh": "So sÃ¡nh sáº£n pháº©m theo tiÃªu chÃ­, giáº£i phÃ¡p vÃ  má»©c giÃ¡.",
  "/cong-cu/chon-san-pham": "CÃ´ng cá»¥ gá»£i Ã½ chá»n sáº£n pháº©m phÃ¹ há»£p nhu cáº§u.",
  "/link-error": "LiÃªn káº¿t khÃ´ng há»£p lá»‡ hoáº·c Ä‘Ã£ háº¿t hiá»‡u lá»±c.",
  "/admin": "Trang tá»•ng quan há»‡ thá»‘ng quáº£n trá»‹ ReviewX.",
  "/admin/login": "ÄÄƒng nháº­p há»‡ thá»‘ng quáº£n trá»‹ ReviewX.",
  "/admin/products": "Quáº£n lÃ½ danh sÃ¡ch sáº£n pháº©m trÃªn ReviewX.",
  "/admin/products/new": "Táº¡o sáº£n pháº©m má»›i trÃªn ReviewX.",
  "/admin/reviews": "Quáº£n lÃ½ cÃ¡c bÃ i review trÃªn ReviewX.",
  "/admin/reviews/new": "Táº¡o bÃ i review má»›i trÃªn ReviewX.",
  "/admin/deals": "Quáº£n lÃ½ deal, mÃ£ giáº£m giÃ¡ vÃ  thá»i gian hiá»‡u lá»±c.",
  "/admin/categories": "Quáº£n lÃ½ danh má»¥c sáº£n pháº©m trÃªn ReviewX.",
  "/admin/brands": "Quáº£n lÃ½ thÆ°Æ¡ng hiá»‡u sáº£n pháº©m trÃªn ReviewX.",
  "/admin/affiliate-links": "Quáº£n lÃ½ liÃªn káº¿t affiliate vÃ  tracking.",
  "/admin/analytics": "Theo dÃµi phÃ¢n tÃ­ch click, chuyá»ƒn Ä‘á»•i vÃ  hiá»‡u suáº¥t.",
  "/admin/ai-drafts": "Quáº£n lÃ½ cÃ¡c bÃ i nhÃ¡p táº¡o bá»Ÿi AI.",
  "/admin/crawler": "Theo dÃµi vÃ  quáº£n lÃ½ cÃ¡c crawler job.",
  "/admin/tools/short-link-converter": "CÃ´ng cá»¥ chuyá»ƒn Ä‘á»•i short link trong admin.",
};

function toTitleCase(value: string): string {
  return value
    .replace(/[-_]/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/\b\w/g, (ch) => ch.toUpperCase());
}

function resolveDynamicTitle(pathname: string): string | null {
  if (pathname.startsWith("/admin/products/")) return "Admin Edit Product | ReviewX";
  if (pathname.startsWith("/admin/reviews/")) return "Admin Edit Review | ReviewX";
  if (pathname.startsWith("/go/deal/")) return "Chuyển hướng deal | ReviewX";
  if (pathname.startsWith("/go/product/")) return "Chuyển hướng sản phẩm | ReviewX";
  if (pathname.startsWith("/go/review/")) return "Chuyển hướng review | ReviewX";
  if (pathname.startsWith("/recommends/")) return "Chuyển hướng affiliate | ReviewX";
  if (pathname.startsWith("/danh-muc/")) return null;
  if (pathname.startsWith("/san-pham/")) return null;
  if (pathname.startsWith("/review/")) return null;

  const segments = pathname.split("/").filter(Boolean);
  if (segments.length === 0) return STATIC_TITLES["/"];
  const formatted = segments.map(toTitleCase).join(" / ");
  return `${formatted} | ReviewX`;
}

function resolveDynamicDescription(pathname: string): string | null {
  if (pathname.startsWith("/admin/products/")) return "Chá»‰nh sá»­a thÃ´ng tin sáº£n pháº©m trong admin ReviewX.";
  if (pathname.startsWith("/admin/reviews/")) return "Chá»‰nh sá»­a ná»™i dung review trong admin ReviewX.";
  if (pathname.startsWith("/go/deal/")) return "Trang chuyá»ƒn hÆ°á»›ng tá»›i deal chi tiáº¿t.";
  if (pathname.startsWith("/go/product/")) return "Trang chuyá»ƒn hÆ°á»›ng tá»›i liÃªn káº¿t mua sáº£n pháº©m.";
  if (pathname.startsWith("/go/review/")) return "Trang chuyá»ƒn hÆ°á»›ng tá»›i liÃªn káº¿t review.";
  if (pathname.startsWith("/recommends/")) return "Trang chuyá»ƒn hÆ°á»›ng affiliate an toÃ n cá»§a ReviewX.";
  if (pathname.startsWith("/danh-muc/")) return null;
  if (pathname.startsWith("/san-pham/")) return null;
  if (pathname.startsWith("/review/")) return null;
  return "Ná»™i dung trang trÃªn ReviewX.";
}

function upsertMetaDescription(content: string) {
  const selector = 'meta[name="description"]';
  let node = document.querySelector(selector) as HTMLMetaElement | null;
  if (!node) {
    node = document.createElement("meta");
    node.setAttribute("name", "description");
    document.head.appendChild(node);
  }
  node.setAttribute("content", content);
}

function upsertOpenGraphTitle(content: string) {
  const selector = 'meta[property="og:title"]';
  let node = document.querySelector(selector) as HTMLMetaElement | null;
  if (!node) {
    node = document.createElement("meta");
    node.setAttribute("property", "og:title");
    document.head.appendChild(node);
  }
  node.setAttribute("content", content);
}

function upsertOpenGraphDescription(content: string) {
  const selector = 'meta[property="og:description"]';
  let node = document.querySelector(selector) as HTMLMetaElement | null;
  if (!node) {
    node = document.createElement("meta");
    node.setAttribute("property", "og:description");
    document.head.appendChild(node);
  }
  node.setAttribute("content", content);
}

function upsertOpenGraphImage(content: string) {
  const selector = 'meta[property="og:image"]';
  let node = document.querySelector(selector) as HTMLMetaElement | null;
  if (!node) {
    node = document.createElement("meta");
    node.setAttribute("property", "og:image");
    document.head.appendChild(node);
  }
  node.setAttribute("content", content);
}

function upsertTwitterCard(content: string) {
  const selector = 'meta[name="twitter:card"]';
  let node = document.querySelector(selector) as HTMLMetaElement | null;
  if (!node) {
    node = document.createElement("meta");
    node.setAttribute("name", "twitter:card");
    document.head.appendChild(node);
  }
  node.setAttribute("content", content);
}

function upsertTwitterTitle(content: string) {
  const selector = 'meta[name="twitter:title"]';
  let node = document.querySelector(selector) as HTMLMetaElement | null;
  if (!node) {
    node = document.createElement("meta");
    node.setAttribute("name", "twitter:title");
    document.head.appendChild(node);
  }
  node.setAttribute("content", content);
}

function upsertTwitterDescription(content: string) {
  const selector = 'meta[name="twitter:description"]';
  let node = document.querySelector(selector) as HTMLMetaElement | null;
  if (!node) {
    node = document.createElement("meta");
    node.setAttribute("name", "twitter:description");
    document.head.appendChild(node);
  }
  node.setAttribute("content", content);
}

function upsertTwitterImage(content: string) {
  const selector = 'meta[name="twitter:image"]';
  let node = document.querySelector(selector) as HTMLMetaElement | null;
  if (!node) {
    node = document.createElement("meta");
    node.setAttribute("name", "twitter:image");
    document.head.appendChild(node);
  }
  node.setAttribute("content", content);
}

function upsertCanonical(pathname: string) {
  const href = `${SITE_ORIGIN}${pathname === "/" ? "" : pathname}`;
  let node = document.querySelector('link[rel="canonical"]') as HTMLLinkElement | null;
  if (!node) {
    node = document.createElement("link");
    node.setAttribute("rel", "canonical");
    document.head.appendChild(node);
  }
  node.setAttribute("href", href);
}

export function RouteTitle() {
  const pathname = usePathname();

  useEffect(() => {
    if (!pathname) return;
    if (document.title && document.title !== DEFAULT_TITLE) return;
    const staticTitle = STATIC_TITLES[pathname];
    const nextTitle = staticTitle ?? resolveDynamicTitle(pathname);
    if (nextTitle) document.title = nextTitle;
  }, [pathname]);

  useEffect(() => {
    if (!pathname) return;
    const currentOgTitle =
      (document.querySelector('meta[property="og:title"]') as HTMLMetaElement | null)?.content ?? "";
    if (currentOgTitle && currentOgTitle !== DEFAULT_TITLE) return;
    const staticTitle = STATIC_TITLES[pathname];
    const nextTitle = staticTitle ?? resolveDynamicTitle(pathname);
    if (nextTitle) upsertOpenGraphTitle(nextTitle);
  }, [pathname]);

  useEffect(() => {
    if (!pathname) return;
    const currentDescription =
      (document.querySelector('meta[name="description"]') as HTMLMetaElement | null)?.content ?? "";
    if (currentDescription && currentDescription !== DEFAULT_DESCRIPTION) return;
    const staticDescription = STATIC_DESCRIPTIONS[pathname];
    const nextDescription = staticDescription ?? resolveDynamicDescription(pathname);
    if (nextDescription) upsertMetaDescription(nextDescription);
  }, [pathname]);

  useEffect(() => {
    if (!pathname) return;
    const currentOgDescription =
      (document.querySelector('meta[property="og:description"]') as HTMLMetaElement | null)?.content ?? "";
    if (currentOgDescription && currentOgDescription !== DEFAULT_DESCRIPTION) return;
    const staticDescription = STATIC_DESCRIPTIONS[pathname];
    const nextDescription = staticDescription ?? resolveDynamicDescription(pathname);
    if (nextDescription) upsertOpenGraphDescription(nextDescription);
  }, [pathname]);

  useEffect(() => {
    if (!pathname) return;
    const currentOgImage =
      (document.querySelector('meta[property="og:image"]') as HTMLMetaElement | null)?.content ?? "";
    if (currentOgImage && currentOgImage !== DEFAULT_OG_IMAGE) return;
    upsertOpenGraphImage(DEFAULT_OG_IMAGE);
  }, [pathname]);

  useEffect(() => {
    if (!pathname) return;
    const currentTwitterCard =
      (document.querySelector('meta[name="twitter:card"]') as HTMLMetaElement | null)?.content ?? "";
    if (currentTwitterCard && currentTwitterCard !== DEFAULT_TWITTER_CARD) return;
    upsertTwitterCard(DEFAULT_TWITTER_CARD);
  }, [pathname]);

  useEffect(() => {
    if (!pathname) return;
    const currentTwitterTitle =
      (document.querySelector('meta[name="twitter:title"]') as HTMLMetaElement | null)?.content ?? "";
    if (currentTwitterTitle && currentTwitterTitle !== DEFAULT_TITLE) return;
    const staticTitle = STATIC_TITLES[pathname];
    const nextTitle = staticTitle ?? resolveDynamicTitle(pathname);
    if (nextTitle) upsertTwitterTitle(nextTitle);
  }, [pathname]);

  useEffect(() => {
    if (!pathname) return;
    const currentTwitterDescription =
      (document.querySelector('meta[name="twitter:description"]') as HTMLMetaElement | null)?.content ?? "";
    if (currentTwitterDescription && currentTwitterDescription !== DEFAULT_DESCRIPTION) return;
    const staticDescription = STATIC_DESCRIPTIONS[pathname];
    const nextDescription = staticDescription ?? resolveDynamicDescription(pathname);
    if (nextDescription) upsertTwitterDescription(nextDescription);
  }, [pathname]);

  useEffect(() => {
    if (!pathname) return;
    const currentTwitterImage =
      (document.querySelector('meta[name="twitter:image"]') as HTMLMetaElement | null)?.content ?? "";
    if (currentTwitterImage && currentTwitterImage !== DEFAULT_OG_IMAGE) return;
    upsertTwitterImage(DEFAULT_OG_IMAGE);
  }, [pathname]);

  useEffect(() => {
    if (!pathname) return;
    const currentCanonical =
      (document.querySelector('link[rel="canonical"]') as HTMLLinkElement | null)?.href ?? "";
    if (currentCanonical && currentCanonical !== `${SITE_ORIGIN}/`) return;
    upsertCanonical(pathname);
  }, [pathname]);

  return null;
}
