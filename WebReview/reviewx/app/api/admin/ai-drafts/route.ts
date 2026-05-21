import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";

type CreatePayload = { productId?: string; quickVerdict?: string; seoTitle?: string; seoDescription?: string; articleDraft?: string };

export async function GET(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const items = await prisma.review.findMany({ where: { status: "DRAFT" }, include: { product: true }, orderBy: { updatedAt: "desc" }, take: 100 });
  return NextResponse.json(ok({ items }));
}

export async function POST(request: NextRequest) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const body = (await request.json()) as CreatePayload;
  const productId = body.productId?.trim() ?? "";
  const quickVerdict = body.quickVerdict?.trim() ?? "";
  const seoTitle = body.seoTitle?.trim() ?? "";
  const seoDescription = body.seoDescription?.trim() ?? "";
  const articleDraft = body.articleDraft?.trim() ?? "";
  if (!productId || !quickVerdict || !articleDraft) return NextResponse.json(fail("Thiếu trường bắt buộc."), { status: 400 });
  const product = await prisma.product.findUnique({ where: { id: productId } });
  if (!product) return NextResponse.json(fail("Product không tồn tại."), { status: 400 });
  const slugBase = product.slug || `draft-${Date.now()}`;
  const slug = `${slugBase}-ai-${Date.now()}`;
  const created = await prisma.review.create({ data: { productId, title: `AI Draft - ${product.name}`, slug, summary: quickVerdict, content: articleDraft, seoTitle: seoTitle || null, seoDescription: seoDescription || null, status: "DRAFT" } });
  return NextResponse.json(ok(created));
}
