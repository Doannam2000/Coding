const { PrismaClient } = require("@prisma/client");
const prisma = new PrismaClient();

async function main() {
  const cat = await prisma.category.findUnique({ where: { slug: "cong-nghe" } });
  const prod = await prisma.product.findUnique({ where: { slug: "sony-wh-ch520" }, include: { affiliateLinks: true, deals: true } });
  const rev = await prisma.review.findUnique({ where: { slug: "sony-wh-ch520" } });
  
  console.log("Category cong-nghe:", cat ? "EXISTS" : "MISSING");
  console.log("Product sony-wh-ch520:", prod ? "EXISTS" : "MISSING");
  console.log("Review sony-wh-ch520:", rev ? "EXISTS" : "MISSING");
  if (prod) {
    console.log("Affiliate links:", prod.affiliateLinks.length);
    console.log("Deals:", prod.deals.length);
  }
}

main().catch(console.error).finally(() => prisma.$disconnect());
