"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ErrorState, LoadingSkeleton } from "@/components/ui";

type ProductStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
type LinkStatus = "active" | "missing" | "expired" | "disabled";

type OptionItem = { id: string; name: string };
type AffiliateOption = { id: string; label: string; platform: string };

type ProductSpecRow = {
  key: string;
  value: string;
  group: string;
  sortOrder: number;
};

type GalleryRow = {
  url: string;
  alt: string;
  sortOrder: number;
};

type ProductFormState = {
  name: string;
  slug: string;
  brandId: string;
  categoryId: string;
  status: ProductStatus;
  badge: string;
  shortDescription: string;
  fullDescription: string;
  currentPrice: string;
  originalPrice: string;
  discountPercent: string;
  priceRangeMin: string;
  priceRangeMax: string;
  currency: string;
  priceUpdatedAt: string;
  thumbnail: string;
  thumbnailAlt: string;
  galleryImages: GalleryRow[];
  shopeeOriginalUrl: string;
  shopeeAffiliateUrl: string;
  shopeeTrackingNote: string;
  lazadaOriginalUrl: string;
  lazadaAffiliateUrl: string;
  tikiOriginalUrl: string;
  tikiAffiliateUrl: string;
  officialStoreUrl: string;
  primaryPlatform: string;
  ctaLabel: string;
  linkStatus: LinkStatus;
  verdictLabel: string;
  shouldBuyIf: string;
  considerIf: string;
  avoidIfText: string;
  buyUnderPrice: string;
  considerAbovePrice: string;
  finalVerdict: string;
  worthScore: string;
  prosList: string[];
  consList: string[];
  suitableForList: string[];
  notSuitableForList: string[];
  specsItems: ProductSpecRow[];
  praisedPoints: string[];
  complainedPoints: string[];
  sentimentScore: string;
  insightNote: string;
  dataSourceNote: string;
  seoTitle: string;
  seoDescription: string;
  seoOgImage: string;
  canonicalUrl: string;
  noindex: boolean;
  tagIds: string[];
  affiliateLinkIds: string[];
};

type ProductDetailApi = {
  id: string;
  name: string;
  slug: string;
  brandId: string;
  categoryId: string;
  status: ProductStatus;
  badge: string | null;
  shortDescription: string | null;
  fullDescription: string | null;
  currentPrice: number | null;
  originalPrice: number | null;
  discountPercent: number | null;
  priceRangeMin: number | null;
  priceRangeMax: number | null;
  currency: string;
  priceUpdatedAt: string | null;
  thumbnail: string | null;
  thumbnailAlt: string | null;
  shopeeOriginalUrl: string | null;
  shopeeAffiliateUrl: string | null;
  shopeeTrackingNote: string | null;
  lazadaOriginalUrl: string | null;
  lazadaAffiliateUrl: string | null;
  tikiOriginalUrl: string | null;
  tikiAffiliateUrl: string | null;
  officialStoreUrl: string | null;
  primaryPlatform: string | null;
  ctaLabel: string | null;
  linkStatus: string | null;
  verdictLabel: string | null;
  shouldBuyIf: string | null;
  considerIf: string | null;
  avoidIfText: string | null;
  buyUnderPrice: number | null;
  considerAbovePrice: number | null;
  finalVerdict: string | null;
  worthScore: number | null;
  prosJson: string | null;
  consJson: string | null;
  suitableForJson: string | null;
  notSuitableForJson: string | null;
  praisedPointsJson: string | null;
  complainedPointsJson: string | null;
  sentimentScore: number | null;
  insightNote: string | null;
  dataSourceNote: string | null;
  seoTitle: string | null;
  seoDescription: string | null;
  seoOgImage: string | null;
  canonicalUrl: string | null;
  noindex: boolean;
  images: Array<{ url: string; alt: string | null; sortOrder: number }>;
  productSpecs: Array<{ key: string; value: string; group: string | null; sortOrder: number }>;
  productTags: Array<{ tagId: string }>;
  affiliateLinks: Array<{ id: string }>;
};

type ApiResponse<T> = { success: boolean; data?: T; error?: string };

const tabs = [
  { id: "basic", label: "CÆ¡ báº£n" },
  { id: "price", label: "GiÃ¡" },
  { id: "media", label: "áº¢nh" },
  { id: "affiliate", label: "Affiliate" },
  { id: "verdict", label: "Verdict" },
  { id: "content", label: "Ná»™i dung" },
  { id: "insight", label: "Insight" },
  { id: "seo", label: "SEO" },
] as const;

const initialState: ProductFormState = {
  name: "",
  slug: "",
  brandId: "",
  categoryId: "",
  status: "DRAFT",
  badge: "",
  shortDescription: "",
  fullDescription: "",
  currentPrice: "",
  originalPrice: "",
  discountPercent: "",
  priceRangeMin: "",
  priceRangeMax: "",
  currency: "VND",
  priceUpdatedAt: "",
  thumbnail: "",
  thumbnailAlt: "",
  galleryImages: [],
  shopeeOriginalUrl: "",
  shopeeAffiliateUrl: "",
  shopeeTrackingNote: "",
  lazadaOriginalUrl: "",
  lazadaAffiliateUrl: "",
  tikiOriginalUrl: "",
  tikiAffiliateUrl: "",
  officialStoreUrl: "",
  primaryPlatform: "",
  ctaLabel: "",
  linkStatus: "missing",
  verdictLabel: "",
  shouldBuyIf: "",
  considerIf: "",
  avoidIfText: "",
  buyUnderPrice: "",
  considerAbovePrice: "",
  finalVerdict: "",
  worthScore: "",
  prosList: [],
  consList: [],
  suitableForList: [],
  notSuitableForList: [],
  specsItems: [],
  praisedPoints: [],
  complainedPoints: [],
  sentimentScore: "",
  insightNote: "",
  dataSourceNote: "",
  seoTitle: "",
  seoDescription: "",
  seoOgImage: "",
  canonicalUrl: "",
  noindex: false,
  tagIds: [],
  affiliateLinkIds: [],
};

function slugify(value: string): string {
  return value
    .toLowerCase()
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/Ä‘/g, "d")
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}

function parseJsonList(raw: string | null): string[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.map((item) => String(item).trim()).filter(Boolean);
  } catch {
    return [];
  }
}

function toNumberOrNull(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) return null;
  const num = Number(trimmed.replace(/,/g, ""));
  return Number.isFinite(num) ? num : null;
}

function formatDateInput(value: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function isValidHttpUrl(value: string): boolean {
  if (!value.trim()) return false;
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

type ProductFormProps = {
  mode: "create" | "edit";
  productId?: string;
};

export function AdminProductForm({ mode, productId }: ProductFormProps) {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<(typeof tabs)[number]["id"]>("basic");
  const [form, setForm] = useState<ProductFormState>(initialState);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [toast, setToast] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [loading, setLoading] = useState(mode === "edit");
  const [saving, setSaving] = useState(false);
  const [apiError, setApiError] = useState("");
  const [brands, setBrands] = useState<OptionItem[]>([]);
  const [categories, setCategories] = useState<OptionItem[]>([]);
  const [tags, setTags] = useState<OptionItem[]>([]);
  const [affiliateLinks, setAffiliateLinks] = useState<AffiliateOption[]>([]);
  const [slugTouched, setSlugTouched] = useState(false);
  const initialSnapshotRef = useRef("");

  const formSnapshot = useMemo(() => JSON.stringify(form), [form]);
  const isDirty = initialSnapshotRef.current !== "" && formSnapshot !== initialSnapshotRef.current;

  const discountAuto = useMemo(() => {
    const current = toNumberOrNull(form.currentPrice);
    const original = toNumberOrNull(form.originalPrice);
    if (current === null || original === null || original <= 0 || current >= original) return 0;
    return Math.round(((original - current) / original) * 100);
  }, [form.currentPrice, form.originalPrice]);

  useEffect(() => {
    if (mode === "create" && !slugTouched && form.name.trim()) {
      setForm((prev) => ({ ...prev, slug: slugify(prev.name) }));
    }
  }, [form.name, mode, slugTouched]);

  useEffect(() => {
    const handler = (event: BeforeUnloadEvent) => {
      if (!isDirty) return;
      event.preventDefault();
      event.returnValue = true;
    };
    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [isDirty]);

  const loadSelectData = useCallback(async () => {
    const [brandRes, categoryRes, tagRes, affiliateRes] = await Promise.all([
      fetch("/api/admin/brands?limit=200"),
      fetch("/api/admin/categories?limit=200"),
      fetch("/api/admin/tags?limit=200"),
      fetch("/api/admin/affiliate-links?limit=200"),
    ]);

    const brandPayload = (await brandRes.json()) as ApiResponse<{ items: OptionItem[] }>;
    const categoryPayload = (await categoryRes.json()) as ApiResponse<{ items: OptionItem[] }>;
    const tagPayload = (await tagRes.json()) as ApiResponse<{ items: OptionItem[] }>;
    const affiliatePayload = (await affiliateRes.json()) as ApiResponse<{ items: AffiliateOption[] }>;

    if (brandRes.ok && brandPayload.success && brandPayload.data) setBrands(brandPayload.data.items);
    if (categoryRes.ok && categoryPayload.success && categoryPayload.data) setCategories(categoryPayload.data.items);
    if (tagRes.ok && tagPayload.success && tagPayload.data) setTags(tagPayload.data.items);
    if (affiliateRes.ok && affiliatePayload.success && affiliatePayload.data) setAffiliateLinks(affiliatePayload.data.items);
  }, []);

  const loadProduct = useCallback(async () => {
    if (mode !== "edit" || !productId) return;
    setLoading(true);
    setApiError("");
    try {
      const response = await fetch(`/api/admin/products/${productId}`);
      const payload = (await response.json()) as ApiResponse<ProductDetailApi>;
      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.error ?? "KhÃ´ng táº£i Ä‘Æ°á»£c dá»¯ liá»‡u product.");
      }
      const item = payload.data;
      const mapped: ProductFormState = {
        name: item.name,
        slug: item.slug,
        brandId: item.brandId,
        categoryId: item.categoryId,
        status: item.status,
        badge: item.badge ?? "",
        shortDescription: item.shortDescription ?? "",
        fullDescription: item.fullDescription ?? "",
        currentPrice: item.currentPrice?.toString() ?? "",
        originalPrice: item.originalPrice?.toString() ?? "",
        discountPercent: item.discountPercent?.toString() ?? "",
        priceRangeMin: item.priceRangeMin?.toString() ?? "",
        priceRangeMax: item.priceRangeMax?.toString() ?? "",
        currency: item.currency ?? "VND",
        priceUpdatedAt: formatDateInput(item.priceUpdatedAt),
        thumbnail: item.thumbnail ?? "",
        thumbnailAlt: item.thumbnailAlt ?? "",
        galleryImages: item.images.map((image) => ({
          url: image.url,
          alt: image.alt ?? "",
          sortOrder: image.sortOrder,
        })),
        shopeeOriginalUrl: item.shopeeOriginalUrl ?? "",
        shopeeAffiliateUrl: item.shopeeAffiliateUrl ?? "",
        shopeeTrackingNote: item.shopeeTrackingNote ?? "",
        lazadaOriginalUrl: item.lazadaOriginalUrl ?? "",
        lazadaAffiliateUrl: item.lazadaAffiliateUrl ?? "",
        tikiOriginalUrl: item.tikiOriginalUrl ?? "",
        tikiAffiliateUrl: item.tikiAffiliateUrl ?? "",
        officialStoreUrl: item.officialStoreUrl ?? "",
        primaryPlatform: item.primaryPlatform ?? "",
        ctaLabel: item.ctaLabel ?? "",
        linkStatus: (item.linkStatus?.toLowerCase() as LinkStatus) || "missing",
        verdictLabel: item.verdictLabel ?? "",
        shouldBuyIf: item.shouldBuyIf ?? "",
        considerIf: item.considerIf ?? "",
        avoidIfText: item.avoidIfText ?? "",
        buyUnderPrice: item.buyUnderPrice?.toString() ?? "",
        considerAbovePrice: item.considerAbovePrice?.toString() ?? "",
        finalVerdict: item.finalVerdict ?? "",
        worthScore: item.worthScore?.toString() ?? "",
        prosList: parseJsonList(item.prosJson),
        consList: parseJsonList(item.consJson),
        suitableForList: parseJsonList(item.suitableForJson),
        notSuitableForList: parseJsonList(item.notSuitableForJson),
        specsItems: item.productSpecs.map((spec) => ({
          key: spec.key,
          value: spec.value,
          group: spec.group ?? "",
          sortOrder: spec.sortOrder,
        })),
        praisedPoints: parseJsonList(item.praisedPointsJson),
        complainedPoints: parseJsonList(item.complainedPointsJson),
        sentimentScore: item.sentimentScore?.toString() ?? "",
        insightNote: item.insightNote ?? "",
        dataSourceNote: item.dataSourceNote ?? "",
        seoTitle: item.seoTitle ?? "",
        seoDescription: item.seoDescription ?? "",
        seoOgImage: item.seoOgImage ?? "",
        canonicalUrl: item.canonicalUrl ?? "",
        noindex: item.noindex,
        tagIds: item.productTags.map((productTag) => productTag.tagId),
        affiliateLinkIds: item.affiliateLinks.map((link) => link.id),
      };
      setForm(mapped);
      initialSnapshotRef.current = JSON.stringify(mapped);
    } catch (error) {
      setApiError(error instanceof Error ? error.message : "KhÃ´ng táº£i Ä‘Æ°á»£c dá»¯ liá»‡u product.");
    } finally {
      setLoading(false);
    }
  }, [mode, productId]);

  useEffect(() => {
    void loadSelectData();
  }, [loadSelectData]);

  useEffect(() => {
    if (mode === "create") {
      initialSnapshotRef.current = JSON.stringify(initialState);
      setLoading(false);
      return;
    }
    void loadProduct();
  }, [loadProduct, mode]);

  function fieldClass(field: string) {
    return `h-11 w-full rounded-xl border px-3 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 ${
      errors[field] ? "border-red-300 bg-red-50" : "border-slate-200"
    }`;
  }

  function setListField<K extends keyof ProductFormState>(field: K, value: string[]) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  function validateForm(nextStatus: ProductStatus): Record<string, string> {
    const next: Record<string, string> = {};
    const urlFields: Array<{ key: keyof ProductFormState; label: string }> = [
      { key: "thumbnail", label: "Main image URL" },
      { key: "shopeeOriginalUrl", label: "Shopee original URL" },
      { key: "shopeeAffiliateUrl", label: "Shopee affiliate URL" },
      { key: "lazadaOriginalUrl", label: "Lazada original URL" },
      { key: "lazadaAffiliateUrl", label: "Lazada affiliate URL" },
      { key: "tikiOriginalUrl", label: "Tiki original URL" },
      { key: "tikiAffiliateUrl", label: "Tiki affiliate URL" },
      { key: "officialStoreUrl", label: "Official store URL" },
      { key: "seoOgImage", label: "SEO OG image" },
      { key: "canonicalUrl", label: "Canonical URL" },
    ];

    if (!form.name.trim()) next.name = "TÃªn sáº£n pháº©m lÃ  báº¯t buá»™c.";
    if (!form.slug.trim()) next.slug = "Slug lÃ  báº¯t buá»™c.";
    if (!form.brandId.trim()) next.brandId = "Brand lÃ  báº¯t buá»™c.";
    if (!form.categoryId.trim()) next.categoryId = "Category lÃ  báº¯t buá»™c.";

    if (form.worthScore.trim()) {
      const score = Number(form.worthScore);
      if (!Number.isFinite(score) || score < 0 || score > 10) {
        next.worthScore = "Score pháº£i náº±m trong khoáº£ng 0-10.";
      }
    }

    const currentPrice = toNumberOrNull(form.currentPrice);
    const originalPrice = toNumberOrNull(form.originalPrice);
    if (currentPrice !== null && currentPrice < 0) next.currentPrice = "Current price pháº£i >= 0.";
    if (originalPrice !== null && originalPrice < 0) next.originalPrice = "Original price pháº£i >= 0.";
    if (currentPrice !== null && originalPrice !== null && currentPrice > originalPrice) {
      next.currentPrice = "Current price khÃ´ng Ä‘Æ°á»£c lá»›n hÆ¡n original price.";
    }

    for (const field of urlFields) {
      const value = String(form[field.key] ?? "").trim();
      if (value && !isValidHttpUrl(value)) {
        next[field.key] = `${field.label} khÃ´ng há»£p lá»‡.`;
      }
    }

    form.galleryImages.forEach((image, index) => {
      if (image.url.trim() && !isValidHttpUrl(image.url)) {
        next[`gallery_${index}_url`] = `Gallery image #${index + 1} URL khÃ´ng há»£p lá»‡.`;
      }
    });

    if (nextStatus === "PUBLISHED") {
      if (!form.shortDescription.trim()) next.shortDescription = "Published yÃªu cáº§u short description.";
      if (!form.fullDescription.trim()) next.fullDescription = "Published yÃªu cáº§u full description.";
      if (!form.thumbnail.trim()) next.thumbnail = "Published yÃªu cáº§u main image URL.";
      if (!form.thumbnailAlt.trim()) next.thumbnailAlt = "Published yÃªu cáº§u alt text cho áº£nh chÃ­nh.";
      if (!form.verdictLabel.trim()) next.verdictLabel = "Published yÃªu cáº§u verdict label.";
      if (currentPrice === null) next.currentPrice = "Published yÃªu cáº§u current price.";
      if (originalPrice === null) next.originalPrice = "Published yÃªu cáº§u original price.";
      if (!form.worthScore.trim()) next.worthScore = "Published yÃªu cáº§u score.";
      if (form.prosList.length === 0) next.prosList = "Published yÃªu cáº§u Ã­t nháº¥t 1 Æ°u Ä‘iá»ƒm.";
      if (form.consList.length === 0) next.consList = "Published yÃªu cáº§u Ã­t nháº¥t 1 nhÆ°á»£c Ä‘iá»ƒm.";
      if (form.specsItems.length === 0) next.specsItems = "Published yÃªu cáº§u Ã­t nháº¥t 1 thÃ´ng sá»‘.";
    }

    return next;
  }

  function toPayload(nextStatus: ProductStatus) {
    const currentPrice = toNumberOrNull(form.currentPrice);
    const originalPrice = toNumberOrNull(form.originalPrice);
    return {
      name: form.name.trim(),
      slug: form.slug.trim(),
      brandId: form.brandId,
      categoryId: form.categoryId,
      status: nextStatus,
      badge: form.badge.trim() || null,
      shortDescription: form.shortDescription.trim() || null,
      fullDescription: form.fullDescription.trim() || null,
      currentPrice,
      originalPrice,
      priceRangeMin: toNumberOrNull(form.priceRangeMin),
      priceRangeMax: toNumberOrNull(form.priceRangeMax),
      currency: form.currency.trim() || "VND",
      priceUpdatedAt: form.priceUpdatedAt ? new Date(form.priceUpdatedAt).toISOString() : null,
      thumbnail: form.thumbnail.trim() || null,
      thumbnailAlt: form.thumbnailAlt.trim() || null,
      galleryImages: form.galleryImages.map((image, index) => ({
        url: image.url.trim(),
        alt: image.alt.trim() || null,
        sortOrder: index,
      })),
      shopeeOriginalUrl: form.shopeeOriginalUrl.trim() || null,
      shopeeAffiliateUrl: form.shopeeAffiliateUrl.trim() || null,
      shopeeTrackingNote: form.shopeeTrackingNote.trim() || null,
      lazadaOriginalUrl: form.lazadaOriginalUrl.trim() || null,
      lazadaAffiliateUrl: form.lazadaAffiliateUrl.trim() || null,
      tikiOriginalUrl: form.tikiOriginalUrl.trim() || null,
      tikiAffiliateUrl: form.tikiAffiliateUrl.trim() || null,
      officialStoreUrl: form.officialStoreUrl.trim() || null,
      primaryPlatform: form.primaryPlatform.trim() || null,
      ctaLabel: form.ctaLabel.trim() || null,
      linkStatus: form.linkStatus,
      worthScore: toNumberOrNull(form.worthScore),
      verdictLabel: form.verdictLabel.trim() || null,
      shouldBuyIf: form.shouldBuyIf.trim() || null,
      considerIf: form.considerIf.trim() || null,
      avoidIfText: form.avoidIfText.trim() || null,
      buyUnderPrice: toNumberOrNull(form.buyUnderPrice),
      considerAbovePrice: toNumberOrNull(form.considerAbovePrice),
      finalVerdict: form.finalVerdict.trim() || null,
      prosList: form.prosList,
      consList: form.consList,
      suitableForList: form.suitableForList,
      notSuitableForList: form.notSuitableForList,
      specsItems: form.specsItems.map((spec, index) => ({
        key: spec.key.trim(),
        value: spec.value.trim(),
        group: spec.group.trim() || null,
        sortOrder: index,
      })),
      praisedPoints: form.praisedPoints,
      complainedPoints: form.complainedPoints,
      sentimentScore: toNumberOrNull(form.sentimentScore),
      insightNote: form.insightNote.trim() || null,
      dataSourceNote: form.dataSourceNote.trim() || null,
      seoTitle: form.seoTitle.trim() || null,
      seoDescription: form.seoDescription.trim() || null,
      seoOgImage: form.seoOgImage.trim() || null,
      canonicalUrl: form.canonicalUrl.trim() || null,
      noindex: form.noindex,
      tagIds: form.tagIds,
      affiliateLinkIds: form.affiliateLinkIds,
      discountPercent: discountAuto,
    };
  }

  async function submit(nextStatus: ProductStatus) {
    const nextErrors = validateForm(nextStatus);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      setToast({ type: "error", message: "Form cÃ²n lá»—i. Vui lÃ²ng kiá»ƒm tra tá»«ng trÆ°á»ng." });
      return;
    }

    setSaving(true);
    setToast(null);
    setApiError("");

    try {
      const payload = toPayload(nextStatus);
      const endpoint = mode === "create" ? "/api/admin/products" : `/api/admin/products/${productId}`;
      const method = mode === "create" ? "POST" : "PATCH";
      const response = await fetch(endpoint, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const result = (await response.json()) as ApiResponse<{ id: string }>;
      if (!response.ok || !result.success) {
        throw new Error(result.error ?? "KhÃ´ng lÆ°u Ä‘Æ°á»£c sáº£n pháº©m.");
      }

      setToast({ type: "success", message: nextStatus === "PUBLISHED" ? "ÄÃ£ publish sáº£n pháº©m." : "ÄÃ£ lÆ°u sáº£n pháº©m." });
      initialSnapshotRef.current = JSON.stringify(form);
      if (mode === "create") {
        router.push("/admin/products");
      } else {
        await loadProduct();
      }
    } catch (error) {
      setToast({ type: "error", message: error instanceof Error ? error.message : "KhÃ´ng lÆ°u Ä‘Æ°á»£c sáº£n pháº©m." });
    } finally {
      setSaving(false);
    }
  }

  async function archiveProduct() {
    if (mode !== "edit" || !productId) return;
    const confirmed = window.confirm("Báº¡n cÃ³ cháº¯c muá»‘n archive sáº£n pháº©m nÃ y?");
    if (!confirmed) return;
    await submit("ARCHIVED");
  }

  async function deleteProduct() {
    if (mode !== "edit" || !productId) return;
    const confirmed = window.confirm("XÃ³a vÄ©nh viá»…n sáº£n pháº©m? Thao tÃ¡c khÃ´ng thá»ƒ hoÃ n tÃ¡c.");
    if (!confirmed) return;
    setSaving(true);
    setToast(null);
    try {
      const response = await fetch(`/api/admin/products/${productId}`, { method: "DELETE" });
      const result = (await response.json()) as ApiResponse<{ deleted: boolean }>;
      if (!response.ok || !result.success) throw new Error(result.error ?? "KhÃ´ng thá»ƒ xÃ³a sáº£n pháº©m.");
      setToast({ type: "success", message: "ÄÃ£ xÃ³a sáº£n pháº©m." });
      router.push("/admin/products");
    } catch (error) {
      setToast({ type: "error", message: error instanceof Error ? error.message : "KhÃ´ng thá»ƒ xÃ³a sáº£n pháº©m." });
    } finally {
      setSaving(false);
    }
  }

  function handleBackToList() {
    if (isDirty) {
      const confirmed = window.confirm("Báº¡n cÃ³ thay Ä‘á»•i chÆ°a lÆ°u. Váº«n rá»i trang?");
      if (!confirmed) return;
    }
    router.push("/admin/products");
  }

  function renderDynamicList(field: keyof ProductFormState, label: string, placeholder: string) {
    const items = form[field] as string[];
    return (
      <div className="rounded-2xl border border-slate-200 p-3">
        <div className="mb-2 flex items-center justify-between">
          <p className="text-sm font-semibold text-slate-800">{label}</p>
          <button
            type="button"
            onClick={() => setListField(field, [...items, ""])}
            className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700"
          >
            + ThÃªm
          </button>
        </div>
        <div className="space-y-2">
          {items.map((item, index) => (
            <div key={`${String(field)}-${index}`} className="flex items-center gap-2">
              <input
                value={item}
                onChange={(event) => {
                  const next = [...items];
                  next[index] = event.target.value;
                  setListField(field, next);
                }}
                placeholder={placeholder}
                className="h-10 flex-1 rounded-lg border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
              />
              <button
                type="button"
                onClick={() => {
                  const next = items.filter((_, idx) => idx !== index);
                  setListField(field, next);
                }}
                className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs font-semibold text-red-700"
              >
                XÃ³a
              </button>
            </div>
          ))}
          {items.length === 0 ? <p className="text-xs text-slate-500">ChÆ°a cÃ³ dá»¯ liá»‡u.</p> : null}
        </div>
      </div>
    );
  }

  function renderSpecsEditor() {
    return (
      <div className="rounded-2xl border border-slate-200 p-3">
        <div className="mb-2 flex items-center justify-between">
          <p className="text-sm font-semibold text-slate-800">ThÃ´ng sá»‘ ká»¹ thuáº­t</p>
          <button
            type="button"
            onClick={() =>
              setForm((prev) => ({
                ...prev,
                specsItems: [...prev.specsItems, { key: "", value: "", group: "", sortOrder: prev.specsItems.length }],
              }))
            }
            className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700"
          >
            + ThÃªm
          </button>
        </div>
        <div className="space-y-2">
          {form.specsItems.map((spec, index) => (
            <div key={`spec-${index}`} className="grid gap-2 rounded-xl border border-slate-200 p-2 sm:grid-cols-4">
              <input
                value={spec.key}
                onChange={(event) =>
                  setForm((prev) => {
                    const next = [...prev.specsItems];
                    next[index] = { ...next[index], key: event.target.value };
                    return { ...prev, specsItems: next };
                  })
                }
                placeholder="Key"
                className="h-10 rounded-lg border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
              />
              <input
                value={spec.value}
                onChange={(event) =>
                  setForm((prev) => {
                    const next = [...prev.specsItems];
                    next[index] = { ...next[index], value: event.target.value };
                    return { ...prev, specsItems: next };
                  })
                }
                placeholder="Value"
                className="h-10 rounded-lg border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
              />
              <input
                value={spec.group}
                onChange={(event) =>
                  setForm((prev) => {
                    const next = [...prev.specsItems];
                    next[index] = { ...next[index], group: event.target.value };
                    return { ...prev, specsItems: next };
                  })
                }
                placeholder="Group (optional)"
                className="h-10 rounded-lg border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
              />
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() =>
                    setForm((prev) => {
                      if (index === 0) return prev;
                      const next = [...prev.specsItems];
                      [next[index - 1], next[index]] = [next[index], next[index - 1]];
                      return { ...prev, specsItems: next };
                    })
                  }
                  className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs"
                >
                  â†‘
                </button>
                <button
                  type="button"
                  onClick={() =>
                    setForm((prev) => {
                      if (index >= prev.specsItems.length - 1) return prev;
                      const next = [...prev.specsItems];
                      [next[index + 1], next[index]] = [next[index], next[index + 1]];
                      return { ...prev, specsItems: next };
                    })
                  }
                  className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs"
                >
                  â†“
                </button>
                <button
                  type="button"
                  onClick={() =>
                    setForm((prev) => ({
                      ...prev,
                      specsItems: prev.specsItems.filter((_, idx) => idx !== index),
                    }))
                  }
                  className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs text-red-700"
                >
                  XÃ³a
                </button>
              </div>
            </div>
          ))}
          {errors.specsItems ? <p className="text-xs text-red-600">{errors.specsItems}</p> : null}
        </div>
      </div>
    );
  }

  function renderGalleryEditor() {
    return (
      <div className="rounded-2xl border border-slate-200 p-3">
        <div className="mb-2 flex items-center justify-between">
          <p className="text-sm font-semibold text-slate-800">Gallery images</p>
          <button
            type="button"
            onClick={() =>
              setForm((prev) => ({
                ...prev,
                galleryImages: [...prev.galleryImages, { url: "", alt: "", sortOrder: prev.galleryImages.length }],
              }))
            }
            className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs font-semibold text-slate-700"
          >
            + ThÃªm
          </button>
        </div>
        <div className="space-y-2">
          {form.galleryImages.map((image, index) => (
            <div key={`gallery-${index}`} className="rounded-xl border border-slate-200 p-2">
              <div className="grid gap-2 sm:grid-cols-2">
                <input
                  value={image.url}
                  onChange={(event) =>
                    setForm((prev) => {
                      const next = [...prev.galleryImages];
                      next[index] = { ...next[index], url: event.target.value };
                      return { ...prev, galleryImages: next };
                    })
                  }
                  placeholder="https://..."
                  className={`h-10 rounded-lg border px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 ${
                    errors[`gallery_${index}_url`] ? "border-red-300 bg-red-50" : "border-slate-200"
                  }`}
                />
                <input
                  value={image.alt}
                  onChange={(event) =>
                    setForm((prev) => {
                      const next = [...prev.galleryImages];
                      next[index] = { ...next[index], alt: event.target.value };
                      return { ...prev, galleryImages: next };
                    })
                  }
                  placeholder="Alt text"
                  className="h-10 rounded-lg border border-slate-200 px-3 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                />
              </div>
              {errors[`gallery_${index}_url`] ? (
                <p className="mt-1 text-xs text-red-600">{errors[`gallery_${index}_url`]}</p>
              ) : null}
              {image.url ? (
                <div className="mt-2 relative h-24 w-24 overflow-hidden rounded-lg border border-slate-200 bg-slate-100">
                  <Image src={image.url} alt={image.alt || `Gallery ${index + 1}`} fill className="object-cover" unoptimized />
                </div>
              ) : null}
              <div className="mt-2 flex items-center gap-2">
                <button
                  type="button"
                  onClick={() =>
                    setForm((prev) => {
                      if (index === 0) return prev;
                      const next = [...prev.galleryImages];
                      [next[index - 1], next[index]] = [next[index], next[index - 1]];
                      return { ...prev, galleryImages: next };
                    })
                  }
                  className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs"
                >
                  â†‘
                </button>
                <button
                  type="button"
                  onClick={() =>
                    setForm((prev) => {
                      if (index >= prev.galleryImages.length - 1) return prev;
                      const next = [...prev.galleryImages];
                      [next[index + 1], next[index]] = [next[index], next[index + 1]];
                      return { ...prev, galleryImages: next };
                    })
                  }
                  className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs"
                >
                  â†“
                </button>
                <button
                  type="button"
                  onClick={() =>
                    setForm((prev) => ({
                      ...prev,
                      galleryImages: prev.galleryImages.filter((_, idx) => idx !== index),
                    }))
                  }
                  className="rounded-lg border border-red-200 bg-red-50 px-2 py-1 text-xs text-red-700"
                >
                  XÃ³a
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="mt-4 grid gap-2">
        <LoadingSkeleton className="h-12 w-full" />
        <LoadingSkeleton className="h-12 w-full" />
        <LoadingSkeleton className="h-12 w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap gap-2">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveTab(tab.id)}
              className={`rounded-xl border px-3 py-1.5 text-sm font-semibold ${
                activeTab === tab.id
                  ? "border-blue-400 bg-blue-50 text-blue-700"
                  : "border-slate-200 bg-white text-slate-700"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
        <button
          type="button"
          onClick={handleBackToList}
          className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700"
        >
          Vá» danh sÃ¡ch
        </button>
      </div>

      {toast ? (
        <div
          className={`rounded-xl border px-3 py-2 text-sm ${
            toast.type === "success"
              ? "border-emerald-200 bg-emerald-50 text-emerald-700"
              : "border-red-200 bg-red-50 text-red-700"
          }`}
        >
          {toast.message}
        </div>
      ) : null}
      {apiError ? <ErrorState title="Lá»—i dá»¯ liá»‡u sáº£n pháº©m" message={apiError} /> : null}

      {activeTab === "basic" ? (
        <div className="grid gap-3 lg:grid-cols-2">
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">TÃªn sáº£n pháº©m *</span>
            <input
              value={form.name}
              onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
              className={fieldClass("name")}
              placeholder="Sony WH-CH520"
            />
            {errors.name ? <span className="text-xs text-red-600">{errors.name}</span> : null}
          </label>

          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Slug *</span>
            <input
              value={form.slug}
              onChange={(event) => {
                setSlugTouched(true);
                setForm((prev) => ({ ...prev, slug: event.target.value }));
              }}
              className={fieldClass("slug")}
              placeholder="sony-wh-ch520"
            />
            {errors.slug ? <span className="text-xs text-red-600">{errors.slug}</span> : null}
          </label>

          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Brand *</span>
            <select
              value={form.brandId}
              onChange={(event) => setForm((prev) => ({ ...prev, brandId: event.target.value }))}
              className={fieldClass("brandId")}
            >
              <option value="">Chá»n brand</option>
              {brands.map((brand) => (
                <option key={brand.id} value={brand.id}>
                  {brand.name}
                </option>
              ))}
            </select>
            {errors.brandId ? <span className="text-xs text-red-600">{errors.brandId}</span> : null}
          </label>

          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Category *</span>
            <select
              value={form.categoryId}
              onChange={(event) => setForm((prev) => ({ ...prev, categoryId: event.target.value }))}
              className={fieldClass("categoryId")}
            >
              <option value="">Chá»n category</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
            {errors.categoryId ? <span className="text-xs text-red-600">{errors.categoryId}</span> : null}
          </label>

          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Status</span>
            <select
              value={form.status}
              onChange={(event) => setForm((prev) => ({ ...prev, status: event.target.value as ProductStatus }))}
              className={fieldClass("status")}
            >
              <option value="DRAFT">Draft</option>
              <option value="PUBLISHED">Published</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </label>

          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Badge</span>
            <input
              value={form.badge}
              onChange={(event) => setForm((prev) => ({ ...prev, badge: event.target.value }))}
              className={fieldClass("badge")}
              placeholder="Top pick 2026"
            />
          </label>

          <label className="space-y-1 text-sm lg:col-span-2">
            <span className="font-semibold text-slate-800">Short description</span>
            <textarea
              value={form.shortDescription}
              onChange={(event) => setForm((prev) => ({ ...prev, shortDescription: event.target.value }))}
              rows={3}
              className={`w-full rounded-xl border px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 ${errors.shortDescription ? "border-red-300 bg-red-50" : "border-slate-200"}`}
            />
            {errors.shortDescription ? <span className="text-xs text-red-600">{errors.shortDescription}</span> : null}
          </label>

          <label className="space-y-1 text-sm lg:col-span-2">
            <span className="font-semibold text-slate-800">Full description</span>
            <textarea
              value={form.fullDescription}
              onChange={(event) => setForm((prev) => ({ ...prev, fullDescription: event.target.value }))}
              rows={6}
              className={`w-full rounded-xl border px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 ${errors.fullDescription ? "border-red-300 bg-red-50" : "border-slate-200"}`}
            />
            {errors.fullDescription ? <span className="text-xs text-red-600">{errors.fullDescription}</span> : null}
          </label>
        </div>
      ) : null}

      {activeTab === "price" ? (
        <div className="grid gap-3 lg:grid-cols-2">
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Current price</span>
            <input
              value={form.currentPrice}
              onChange={(event) => setForm((prev) => ({ ...prev, currentPrice: event.target.value }))}
              className={fieldClass("currentPrice")}
            />
            {errors.currentPrice ? <span className="text-xs text-red-600">{errors.currentPrice}</span> : null}
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Original price</span>
            <input
              value={form.originalPrice}
              onChange={(event) => setForm((prev) => ({ ...prev, originalPrice: event.target.value }))}
              className={fieldClass("originalPrice")}
            />
            {errors.originalPrice ? <span className="text-xs text-red-600">{errors.originalPrice}</span> : null}
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Discount % (auto)</span>
            <input value={String(discountAuto)} readOnly className="h-11 w-full rounded-xl border border-slate-200 bg-slate-100 px-3 text-sm" />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Currency</span>
            <input
              value={form.currency}
              onChange={(event) => setForm((prev) => ({ ...prev, currency: event.target.value.toUpperCase() }))}
              className={fieldClass("currency")}
            />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Price range min</span>
            <input
              value={form.priceRangeMin}
              onChange={(event) => setForm((prev) => ({ ...prev, priceRangeMin: event.target.value }))}
              className={fieldClass("priceRangeMin")}
            />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Price range max</span>
            <input
              value={form.priceRangeMax}
              onChange={(event) => setForm((prev) => ({ ...prev, priceRangeMax: event.target.value }))}
              className={fieldClass("priceRangeMax")}
            />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Price updated at</span>
            <input
              type="date"
              value={form.priceUpdatedAt}
              onChange={(event) => setForm((prev) => ({ ...prev, priceUpdatedAt: event.target.value }))}
              className={fieldClass("priceUpdatedAt")}
            />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Score 0-10</span>
            <input
              value={form.worthScore}
              onChange={(event) => setForm((prev) => ({ ...prev, worthScore: event.target.value }))}
              className={fieldClass("worthScore")}
            />
            {errors.worthScore ? <span className="text-xs text-red-600">{errors.worthScore}</span> : null}
          </label>
        </div>
      ) : null}

      {activeTab === "media" ? (
        <div className="space-y-3">
          <div className="grid gap-3 lg:grid-cols-2">
            <label className="space-y-1 text-sm">
              <span className="font-semibold text-slate-800">Main image URL</span>
              <input
                value={form.thumbnail}
                onChange={(event) => setForm((prev) => ({ ...prev, thumbnail: event.target.value }))}
                className={fieldClass("thumbnail")}
                placeholder="https://..."
              />
              {errors.thumbnail ? <span className="text-xs text-red-600">{errors.thumbnail}</span> : null}
            </label>
            <label className="space-y-1 text-sm">
              <span className="font-semibold text-slate-800">Main image alt</span>
              <input
                value={form.thumbnailAlt}
                onChange={(event) => setForm((prev) => ({ ...prev, thumbnailAlt: event.target.value }))}
                className={fieldClass("thumbnailAlt")}
              />
              {errors.thumbnailAlt ? <span className="text-xs text-red-600">{errors.thumbnailAlt}</span> : null}
            </label>
          </div>
          {form.thumbnail ? (
            <div className="relative h-40 w-56 overflow-hidden rounded-xl border border-slate-200 bg-slate-100">
              <Image src={form.thumbnail} alt={form.thumbnailAlt || form.name || "Preview"} fill className="object-cover" unoptimized />
            </div>
          ) : null}
          {renderGalleryEditor()}
        </div>
      ) : null}

      {activeTab === "affiliate" ? (
        <div className="grid gap-3 lg:grid-cols-2">
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Shopee original URL</span>
            <input value={form.shopeeOriginalUrl} onChange={(event) => setForm((prev) => ({ ...prev, shopeeOriginalUrl: event.target.value }))} className={fieldClass("shopeeOriginalUrl")} />
            {errors.shopeeOriginalUrl ? <span className="text-xs text-red-600">{errors.shopeeOriginalUrl}</span> : null}
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Shopee affiliate URL</span>
            <input value={form.shopeeAffiliateUrl} onChange={(event) => setForm((prev) => ({ ...prev, shopeeAffiliateUrl: event.target.value }))} className={fieldClass("shopeeAffiliateUrl")} />
            {errors.shopeeAffiliateUrl ? <span className="text-xs text-red-600">{errors.shopeeAffiliateUrl}</span> : null}
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Shopee tracking note/subId</span>
            <input value={form.shopeeTrackingNote} onChange={(event) => setForm((prev) => ({ ...prev, shopeeTrackingNote: event.target.value }))} className={fieldClass("shopeeTrackingNote")} />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Lazada original URL</span>
            <input value={form.lazadaOriginalUrl} onChange={(event) => setForm((prev) => ({ ...prev, lazadaOriginalUrl: event.target.value }))} className={fieldClass("lazadaOriginalUrl")} />
            {errors.lazadaOriginalUrl ? <span className="text-xs text-red-600">{errors.lazadaOriginalUrl}</span> : null}
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Lazada affiliate URL</span>
            <input value={form.lazadaAffiliateUrl} onChange={(event) => setForm((prev) => ({ ...prev, lazadaAffiliateUrl: event.target.value }))} className={fieldClass("lazadaAffiliateUrl")} />
            {errors.lazadaAffiliateUrl ? <span className="text-xs text-red-600">{errors.lazadaAffiliateUrl}</span> : null}
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Tiki original URL</span>
            <input value={form.tikiOriginalUrl} onChange={(event) => setForm((prev) => ({ ...prev, tikiOriginalUrl: event.target.value }))} className={fieldClass("tikiOriginalUrl")} />
            {errors.tikiOriginalUrl ? <span className="text-xs text-red-600">{errors.tikiOriginalUrl}</span> : null}
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Tiki affiliate URL</span>
            <input value={form.tikiAffiliateUrl} onChange={(event) => setForm((prev) => ({ ...prev, tikiAffiliateUrl: event.target.value }))} className={fieldClass("tikiAffiliateUrl")} />
            {errors.tikiAffiliateUrl ? <span className="text-xs text-red-600">{errors.tikiAffiliateUrl}</span> : null}
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Official store URL</span>
            <input value={form.officialStoreUrl} onChange={(event) => setForm((prev) => ({ ...prev, officialStoreUrl: event.target.value }))} className={fieldClass("officialStoreUrl")} />
            {errors.officialStoreUrl ? <span className="text-xs text-red-600">{errors.officialStoreUrl}</span> : null}
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Primary platform</span>
            <select value={form.primaryPlatform} onChange={(event) => setForm((prev) => ({ ...prev, primaryPlatform: event.target.value }))} className={fieldClass("primaryPlatform")}>
              <option value="">Chá»n platform</option>
              <option value="Shopee">Shopee</option>
              <option value="Lazada">Lazada</option>
              <option value="Tiki">Tiki</option>
              <option value="Official">Official Store</option>
            </select>
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Button label</span>
            <input value={form.ctaLabel} onChange={(event) => setForm((prev) => ({ ...prev, ctaLabel: event.target.value }))} className={fieldClass("ctaLabel")} />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Link status</span>
            <select value={form.linkStatus} onChange={(event) => setForm((prev) => ({ ...prev, linkStatus: event.target.value as LinkStatus }))} className={fieldClass("linkStatus")}>
              <option value="active">active</option>
              <option value="missing">missing</option>
              <option value="expired">expired</option>
              <option value="disabled">disabled</option>
            </select>
          </label>
          <div className="space-y-1 text-sm lg:col-span-2">
            <span className="font-semibold text-slate-800">Affiliate links CMS gáº¯n vá»›i product</span>
            <div className="grid max-h-56 grid-cols-1 gap-2 overflow-y-auto rounded-xl border border-slate-200 p-3 sm:grid-cols-2">
              {affiliateLinks.map((link) => {
                const checked = form.affiliateLinkIds.includes(link.id);
                return (
                  <label key={link.id} className="flex items-center gap-2 text-sm text-slate-700">
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={(event) =>
                        setForm((prev) => ({
                          ...prev,
                          affiliateLinkIds: event.target.checked
                            ? [...prev.affiliateLinkIds, link.id]
                            : prev.affiliateLinkIds.filter((id) => id !== link.id),
                        }))
                      }
                    />
                    <span className="truncate">{link.label} ({link.platform})</span>
                  </label>
                );
              })}
            </div>
          </div>
        </div>
      ) : null}

      {activeTab === "verdict" ? (
        <div className="grid gap-3 lg:grid-cols-2">
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Verdict label</span>
            <input value={form.verdictLabel} onChange={(event) => setForm((prev) => ({ ...prev, verdictLabel: event.target.value }))} className={fieldClass("verdictLabel")} />
            {errors.verdictLabel ? <span className="text-xs text-red-600">{errors.verdictLabel}</span> : null}
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Ph? h?p n?u</span>
            <input value={form.shouldBuyIf} onChange={(event) => setForm((prev) => ({ ...prev, shouldBuyIf: event.target.value }))} className={fieldClass("shouldBuyIf")} />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Consider if</span>
            <input value={form.considerIf} onChange={(event) => setForm((prev) => ({ ...prev, considerIf: event.target.value }))} className={fieldClass("considerIf")} />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Kh?ng ph? h?p n?u</span>
            <input value={form.avoidIfText} onChange={(event) => setForm((prev) => ({ ...prev, avoidIfText: event.target.value }))} className={fieldClass("avoidIfText")} />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Buy under price</span>
            <input value={form.buyUnderPrice} onChange={(event) => setForm((prev) => ({ ...prev, buyUnderPrice: event.target.value }))} className={fieldClass("buyUnderPrice")} />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Consider above price</span>
            <input value={form.considerAbovePrice} onChange={(event) => setForm((prev) => ({ ...prev, considerAbovePrice: event.target.value }))} className={fieldClass("considerAbovePrice")} />
          </label>
          <label className="space-y-1 text-sm lg:col-span-2">
            <span className="font-semibold text-slate-800">Final verdict</span>
            <textarea value={form.finalVerdict} onChange={(event) => setForm((prev) => ({ ...prev, finalVerdict: event.target.value }))} rows={4} className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100" />
          </label>
        </div>
      ) : null}

      {activeTab === "content" ? (
        <div className="space-y-3">
          {renderDynamicList("prosList", "Pros", "Æ¯u Ä‘iá»ƒm")}
          {errors.prosList ? <p className="text-xs text-red-600">{errors.prosList}</p> : null}
          {renderDynamicList("consList", "Cons", "NhÆ°á»£c Ä‘iá»ƒm")}
          {errors.consList ? <p className="text-xs text-red-600">{errors.consList}</p> : null}
          {renderDynamicList("suitableForList", "Suitable for", "PhÃ¹ há»£p cho")}
          {renderDynamicList("notSuitableForList", "Not suitable for", "KhÃ´ng phÃ¹ há»£p cho")}
          {renderSpecsEditor()}
          <div className="rounded-2xl border border-slate-200 p-3">
            <p className="mb-2 text-sm font-semibold text-slate-800">Tags</p>
            <div className="grid max-h-40 grid-cols-2 gap-2 overflow-y-auto sm:grid-cols-3">
              {tags.map((tag) => (
                <label key={tag.id} className="flex items-center gap-2 text-sm text-slate-700">
                  <input
                    type="checkbox"
                    checked={form.tagIds.includes(tag.id)}
                    onChange={(event) =>
                      setForm((prev) => ({
                        ...prev,
                        tagIds: event.target.checked
                          ? [...prev.tagIds, tag.id]
                          : prev.tagIds.filter((id) => id !== tag.id),
                      }))
                    }
                  />
                  <span>{tag.name}</span>
                </label>
              ))}
            </div>
          </div>
        </div>
      ) : null}

      {activeTab === "insight" ? (
        <div className="space-y-3">
          {renderDynamicList("praisedPoints", "Praised points", "Äiá»ƒm Ä‘Æ°á»£c khen")}
          {renderDynamicList("complainedPoints", "Complained points", "Äiá»ƒm bá»‹ chÃª")}
          <div className="grid gap-3 lg:grid-cols-2">
            <label className="space-y-1 text-sm">
              <span className="font-semibold text-slate-800">Sentiment score</span>
              <input value={form.sentimentScore} onChange={(event) => setForm((prev) => ({ ...prev, sentimentScore: event.target.value }))} className={fieldClass("sentimentScore")} />
            </label>
            <label className="space-y-1 text-sm">
              <span className="font-semibold text-slate-800">Data source note</span>
              <input value={form.dataSourceNote} onChange={(event) => setForm((prev) => ({ ...prev, dataSourceNote: event.target.value }))} className={fieldClass("dataSourceNote")} />
            </label>
            <label className="space-y-1 text-sm lg:col-span-2">
              <span className="font-semibold text-slate-800">Insight note</span>
              <textarea value={form.insightNote} onChange={(event) => setForm((prev) => ({ ...prev, insightNote: event.target.value }))} rows={4} className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100" />
            </label>
          </div>
        </div>
      ) : null}

      {activeTab === "seo" ? (
        <div className="grid gap-3 lg:grid-cols-2">
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Meta title</span>
            <input value={form.seoTitle} onChange={(event) => setForm((prev) => ({ ...prev, seoTitle: event.target.value }))} className={fieldClass("seoTitle")} />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Meta description</span>
            <input value={form.seoDescription} onChange={(event) => setForm((prev) => ({ ...prev, seoDescription: event.target.value }))} className={fieldClass("seoDescription")} />
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">OG image</span>
            <input value={form.seoOgImage} onChange={(event) => setForm((prev) => ({ ...prev, seoOgImage: event.target.value }))} className={fieldClass("seoOgImage")} />
            {errors.seoOgImage ? <span className="text-xs text-red-600">{errors.seoOgImage}</span> : null}
          </label>
          <label className="space-y-1 text-sm">
            <span className="font-semibold text-slate-800">Canonical URL</span>
            <input value={form.canonicalUrl} onChange={(event) => setForm((prev) => ({ ...prev, canonicalUrl: event.target.value }))} className={fieldClass("canonicalUrl")} />
            {errors.canonicalUrl ? <span className="text-xs text-red-600">{errors.canonicalUrl}</span> : null}
          </label>
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input type="checkbox" checked={form.noindex} onChange={(event) => setForm((prev) => ({ ...prev, noindex: event.target.checked }))} />
            <span>Noindex</span>
          </label>
        </div>
      ) : null}

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => void submit("DRAFT")}
          disabled={saving}
          className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-2 text-sm font-semibold text-amber-700 disabled:opacity-60"
        >
          {saving ? "Äang lÆ°u..." : "LÆ°u draft"}
        </button>
        <button
          type="button"
          onClick={() => void submit("PUBLISHED")}
          disabled={saving}
          className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-700 disabled:opacity-60"
        >
          {saving ? "Äang publish..." : "Publish"}
        </button>
        {mode === "edit" ? (
          <>
            <button
              type="button"
              onClick={() => void archiveProduct()}
              disabled={saving}
              className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 disabled:opacity-60"
            >
              Archive
            </button>
            <button
              type="button"
              onClick={() => void deleteProduct()}
              disabled={saving}
              className="rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-sm font-semibold text-red-700 disabled:opacity-60"
            >
              Delete
            </button>
          </>
        ) : null}
      </div>
    </div>
  );
}

