const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

async function main() {
  // Check products
  const products = await prisma.product.findMany({
    where: { name: { contains: "Sony" } },
    select: { id: true, name: true, slug: true }
  });
  console.log("Sony products:", products);

  // Check reviews for sony product
  const sonyProduct = products[0];
  if (sonyProduct) {
    const review = await prisma.review.findFirst({
      where: { productId: sonyProduct.id },
      select: { id: true, slug: true, title: true, productId: true }
    });
    console.log("Review for Sony product:", review);
  }

  // Check all reviews
  const allReviews = await prisma.review.findMany({
    select: { id: true, slug: true, title: true, productId: true }
  });
  console.log("All reviews:", allReviews);

  await prisma.$disconnect();
}

main().catch(console.error);