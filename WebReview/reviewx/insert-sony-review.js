const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

async function main() {
  // First check if the sony product exists
  const sonyProduct = await prisma.product.findUnique({
    where: { slug: "sony-wh-ch520" },
  });

  if (!sonyProduct) {
    console.log("Sony product not found!");
    return;
  }

  console.log("Sony product found:", sonyProduct);

  // Check if review already exists
  const existingReview = await prisma.review.findUnique({
    where: { slug: "sony-wh-ch520" },
  });

  if (existingReview) {
    console.log("Review already exists:", existingReview);
    return;
  }

  // Create the review
  const review = await prisma.review.create({
    data: {
      productId: sonyProduct.id,
      categoryId: sonyProduct.categoryId,
      title: "Review Tai nghe Sony WH-CH520",
      slug: "sony-wh-ch520",
      summary: "Đánh giá nhanh Tai nghe Sony WH-CH520",
      content: "Nội dung review mẫu cho Tai nghe Sony WH-CH520",
      score: 8.5,
      status: "PUBLISHED",
      publishedAt: new Date(),
    },
  });

  console.log("Review created:", review);

  // Create an affiliate link for the review
  const affiliateLink = await prisma.affiliateLink.create({
    data: {
      productId: sonyProduct.id,
      reviewId: review.id,
      platform: "Shopee",
      label: "Tai nghe Sony WH-CH520 chính hãng",
      originalUrl: "https://shopee.vn",
      affiliateUrl: "https://shopee.vn",
      internalUrl: `/recommends/${sonyProduct.slug}`,
      status: "ACTIVE",
    },
  });

  console.log("Affiliate link created:", affiliateLink);

  await prisma.$disconnect();
}

main().catch(console.error);