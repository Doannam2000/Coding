import { prisma } from "@/lib/prisma";

export type ClickEvent = {
  id: string;
  date: string;
  platform: "Shopee" | "Lazada" | "Tiki";
  product: string;
  review: string;
  affiliateLabel: string;
  category: string;
};

function rangeStart(range: "7D" | "30D" | "90D") {
  const days = range === "90D" ? 90 : range === "30D" ? 30 : 7;
  const start = new Date();
  start.setHours(0, 0, 0, 0);
  start.setDate(start.getDate() - (days - 1));
  return start;
}

export async function getClickEventsFromDb(range: "7D" | "30D" | "90D") {
  const start = rangeStart(range);
  const rows = await prisma.clickEvent.findMany({
    where: { createdAt: { gte: start } },
    include: {
      product: { select: { name: true, category: { select: { name: true } } } },
      review: { select: { title: true } },
      affiliateLink: { select: { label: true } },
    },
    orderBy: { createdAt: "desc" },
    take: 500,
  });

  return rows.map((row: {
    id: string;
    createdAt: Date;
    platform: string | null;
    product: { name: string; category: { name: string } | null } | null;
    review: { title: string } | null;
    affiliateLink: { label: string } | null;
  }) => ({
    id: row.id,
    date: row.createdAt.toISOString().slice(0, 10),
    platform: (row.platform as "Shopee" | "Lazada" | "Tiki") ?? "Shopee",
    product: row.product?.name ?? "Unknown product",
    review: row.review?.title ?? "Unknown review",
    affiliateLabel: row.affiliateLink?.label ?? "Unknown link",
    category: row.product?.category?.name ?? "Khác",
  })) as ClickEvent[];
}
