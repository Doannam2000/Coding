const SUPPORTED_SHOPEE_HOSTS = new Set(["s.shopee.vn", "shopee.ee", "shp.ee", "shopee.vn"]);

export function parseHttpUrl(input: string): URL | null {
  try {
    const url = new URL(input.trim());
    if (url.protocol !== "http:" && url.protocol !== "https:") return null;
    return url;
  } catch {
    return null;
  }
}

export function isSupportedShopeeHost(host: string): boolean {
  return SUPPORTED_SHOPEE_HOSTS.has(host.toLowerCase());
}

export function normalizePaginationParams(searchParams: URLSearchParams) {
  const limitRaw = Number(searchParams.get("limit") ?? 10);
  const pageRaw = Number(searchParams.get("page") ?? 1);
  const limit = Number.isFinite(limitRaw) ? Math.min(Math.max(limitRaw, 1), 100) : 10;
  const page = Number.isFinite(pageRaw) ? Math.max(pageRaw, 1) : 1;
  const skip = (page - 1) * limit;
  return { page, limit, skip };
}
