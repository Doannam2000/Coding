const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

async function main() {
  const review = await prisma.review.findUnique({
    where: { slug: "sony-wh-ch520" },
  });

  console.log("Review found:", review);

  if (!review) {
    console.log("Review not found, checking all reviews:");
    const allReviews = await prisma.review.findMany();
    console.log("All reviews:", allReviews.map(r => ({ id: r.id, slug: r.slug, title: r.title })));
  }

  await prisma.$disconnect();
}

main().catch(console.error);