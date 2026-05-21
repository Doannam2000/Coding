import { NextRequest, NextResponse } from "next/server";
import { CrawlerJobStatus, Prisma } from "@prisma/client";
import { prisma } from "@/lib/prisma";
import { requireAdminApiAccess } from "@/lib/admin-api-guard";
import { fail, ok } from "@/lib/api-response";

const CRAWLER_JOB_STATUSES = new Set<CrawlerJobStatus>([
  "PENDING",
  "RUNNING",
  "SUCCESS",
  "FAILED",
  "RETRYING",
  "CAPTCHA_REQUIRED",
]);

export async function GET(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const item = await prisma.crawlerJob.findUnique({ where: { id } });
  if (!item) return NextResponse.json(fail("Không tìm thấy crawler job."), { status: 404 });
  return NextResponse.json(ok(item));
}

export async function PATCH(request: NextRequest, context: { params: Promise<{ id: string }> }) {
  const denied = requireAdminApiAccess(request);
  if (denied) return denied;
  const { id } = await context.params;
  const body = (await request.json()) as { status?: string; appendLog?: string; rawResult?: string };
  const exists = await prisma.crawlerJob.findUnique({ where: { id } });
  if (!exists) return NextResponse.json(fail("Không tìm thấy crawler job."), { status: 404 });
  const data: Prisma.CrawlerJobUpdateInput = {};
  if (body.status) {
    const nextStatus = body.status.trim() as CrawlerJobStatus;
    if (!CRAWLER_JOB_STATUSES.has(nextStatus)) {
      return NextResponse.json(fail("Tráº¡ng thÃ¡i crawler job khÃ´ng há»£p lá»‡."), { status: 400 });
    }
    data.status = nextStatus;
  }
  if (body.appendLog) data.logs = (exists.logs || "") + "\n" + body.appendLog;
  if (body.rawResult) data.rawResult = body.rawResult;
  const updated = await prisma.crawlerJob.update({ where: { id }, data });
  return NextResponse.json(ok(updated));
}
