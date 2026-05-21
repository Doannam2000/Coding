import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";
import { normalizePaginationParams } from "@/lib/validators";

type CrawlerJobType = "PRODUCT_BY_URL" | "PRODUCT_BY_KEYWORD" | "REVIEW_BY_PRODUCT" | "SHORT_LINK_RESOLVE";
type CreatePayload = { type?: CrawlerJobType; input?: string };

export async function GET(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { page, limit, skip } = normalizePaginationParams(request.nextUrl.searchParams);
  const items = await prisma.crawlerJob.findMany({ orderBy: { createdAt: "desc" }, skip, take: limit });
  return NextResponse.json(ok({ items, page, limit }));
}

export async function POST(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const body = (await request.json()) as CreatePayload;
  const input = body.input?.trim() ?? "";
  const type = body.type;
  if (!input || !type) return NextResponse.json(fail("Thiếu input hoặc type."), { status: 400 });
  if (input.length > 2048) return NextResponse.json(fail("Input crawler quá dài."), { status: 400 });
  if (type === "PRODUCT_BY_URL") {
    const parsed = (() => {
      try {
        const url = new URL(input);
        return url.protocol === "http:" || url.protocol === "https:";
      } catch {
        return false;
      }
    })();
    if (!parsed) return NextResponse.json(fail("URL crawler không hợp lệ."), { status: 400 });
  }
  const created = await prisma.crawlerJob.create({ data: { type, input, status: "PENDING", logs: "Job created", rawResult: "" } });
  return NextResponse.json(ok(created));
}
