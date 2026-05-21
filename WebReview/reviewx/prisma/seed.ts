// @ts-nocheck
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

const categoriesSeed = [
  { name: 'Cong nghe', slug: 'cong-nghe' },
  { name: 'Gia dung', slug: 'gia-dung' },
  { name: 'Lam dep', slug: 'lam-dep' },
  { name: 'Me va be', slug: 'me-va-be' },
  { name: 'Nha cua', slug: 'nha-cua' },
  { name: 'Do bep', slug: 'do-bep' },
  { name: 'Gaming', slug: 'gaming' },
  { name: 'The thao', slug: 'the-thao' },
  { name: 'Sach', slug: 'sach' },
];

const productNames = [
  'Tai nghe Sony WH-CH520',
  'Loa Bluetooth JBL Go 3',
  'Noi chien khong dau Lock&Lock',
  'Ban phim co Aula F75',
  'May hut bui mini Deerma',
  'Sua rua mat CeraVe',
  'Den livestream mini',
  'Chuot Logitech M331',
  'May loc khong khi Xiaomi',
  'Gia deo laptop nhom',
  'Camera wifi Ezviz',
  'Binh giat nhiet Lock&Lock',
];

function slugify(value) {
  return value.normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
}

function getSeedProductSlug(name) {
  if (name === 'Tai nghe Sony WH-CH520') return 'sony-wh-ch520';
  if (name === 'Loa Bluetooth JBL Go 3') return 'jbl-go-3';
  if (name === 'Chuot Logitech M331') return 'logitech-m331';
  return slugify(name);
}
async function main() {
  await prisma.comparisonItem.deleteMany();
  await prisma.comparisonGroup.deleteMany();
  await prisma.deal.deleteMany();
  await prisma.clickEvent.deleteMany();
  await prisma.shortLink.deleteMany();
  await prisma.affiliateLink.deleteMany();
  await prisma.fAQ.deleteMany();
  await prisma.reviewInsight.deleteMany();
  await prisma.review.deleteMany();
  await prisma.productSource.deleteMany();
  await prisma.productImage.deleteMany();
  await prisma.productTag.deleteMany();
  await prisma.tag.deleteMany();
  await prisma.product.deleteMany();
  await prisma.brand.deleteMany();
  await prisma.category.deleteMany();
  await prisma.crawlerJob.deleteMany();

  var categories = [];
  for (var i = 0; i < categoriesSeed.length; i += 1) {
    var cr = await prisma.category.create({ data: { name: categoriesSeed[i].name, slug: categoriesSeed[i].slug, status: "PUBLISHED", sortOrder: i } });
    categories.push(cr);
  }

  var brands = [];
  var bnames = ["Sony", "JBL", "Lock&Lock", "Aula", "Deerma", "CeraVe", "Xiaomi", "Ezviz", "Logitech"];
  for (var _i = 0, bnames_1 = bnames; _i < bnames_1.length; _i++) {
    var n = bnames_1[_i];
    var b = await prisma.brand.create({ data: { name: n, slug: slugify(n), status: "PUBLISHED" } });
    brands.push(b);
  }

  var products = [];
  for (var i = 0; i < productNames.length; i += 1) {
    var n = productNames[i];
    var p = await prisma.product.create({ data: { name: n, slug: n.toLowerCase().includes("aula f75") ? "aula-f75" : getSeedProductSlug(n), brandId: brands[i % brands.length].id, categoryId: categories[i % categories.length].id, status: "PUBLISHED", worthScore: 7.5 + ((i % 4) * 0.4) } });
    products.push(p);
  }

  var sony = products[0];
  var sr = await prisma.review.create({
    data: {
      productId: sony.id, categoryId: sony.categoryId,
      title: "Review Sony WH-CH520: Tai nghe pin tru dang mua duoi 1 trieu?",
      slug: sony.slug, summary: "Dai gia nhanh Sony WH-CH520",
      content: "<p>Day la noi dung review cho Sony WH-CH520.</p>",
      score: 8.6, bestFor: "Hoc tap, di lam, nghe nhac hang ngay",
      avoidIf: "Nguoi yeu thich am thanh bass manh",
      verdict: "Day la sieu lua chon cho nguoi muon tai nghe khong day cap voi pin dai.",
      coverImage: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?q=80&w=800&auto=format&fit=crop",
      status: "PUBLISHED", publishedAt: new Date()
    }
  });

  await prisma.reviewInsight.create({
    data: { reviewId: sr.id, positive: "Pin dai, trong luong nhe, giong cao", negative: "Khong co ANC, bass yeu", keywords: "pin dai, nhe, giong cao, bt5.2" },
  });

  await prisma.fAQ.createMany({
    data: [
      { reviewId: sr.id, question: "San pham nay co ANC khong?", answer: "Khong, WH-CH520 khong co ANC.", sortOrder: 0 },
      { reviewId: sr.id, question: "Pin dung bao lau?", answer: "Toi da 50 gio khi sac day.", sortOrder: 1 },
      { reviewId: sr.id, question: "Cai dat co don gian?", answer: "Co, tai app Sony Headphones Connect.", sortOrder: 2 }
    ]
  });

  var reviews = [sr];
  for (var i = 1; i < 6; i += 1) {
    var p = products[i];
    var r = await prisma.review.create({
      data: { productId: p.id, categoryId: p.categoryId, title: "Review " + p.name, slug: p.slug,
        summary: "Dai gia nhanh " + p.name, content: "<p>Noi dung mau cua " + p.name + ".</p>",
        score: 7.8 + i * 0.2, bestFor: "Nguoi dung thich san pham", avoidIf: "Khong phai cho moi nguoi",
        verdict: "Lua chon tot trong phan khuc.", coverImage: "https://images.unsplash.com/photo-1545127398-14699f92334b?q=80&w=800&auto=format&fit=crop",
        status: "PUBLISHED", publishedAt: new Date() },
    });
    reviews.push(r);
    await prisma.reviewInsight.create({ data: { reviewId: r.id, positive: "San pham tot", negative: "Han che nho", keywords: "tot,gia re" } });
  }

  for (var i = 6; i < products.length; i++) {
    var p = products[i];
    var r = await prisma.review.create({
      data: { productId: p.id, categoryId: p.categoryId, title: "Review " + p.name, slug: p.slug,
        summary: "Dai gia nhanh " + p.name, content: "<p>Noi dung mau.</p>",
        score: 7.0 + (i % 3) * 0.5, bestFor: "San pham tot", avoidIf: "Co han che", verdict: "Lua chon tot.", coverImage: null,
        status: "PUBLISHED", publishedAt: new Date() },
    });
    await prisma.reviewInsight.create({ data: { reviewId: r.id, positive: "Tot", negative: "Han che nho", keywords: "tot" } });
  }

  var aff = [];
  for (var i = 0; i < products.length; i++) {
    var rev = reviews.find(function(r) { return r.productId === products[i].id; }) || null;
    if (i < 8) {
      var l = await prisma.affiliateLink.create({
        data: { productId: products[i].id, reviewId: rev ? rev.id : null, platform: "Shopee", label: products[i].name + " chinh hang",
          originalUrl: "https://shopee.vn", affiliateUrl: "https://shopee.vn", internalUrl: "/recommends/" + products[i].slug, status: "ACTIVE" },
      });
      aff.push(l);
    }
  }

  var ct = Date.now();
  for (var i = 0; i < Math.min(aff.length, 8); i++) {
    var lk = aff[i]; var et = new Date(ct + (7 + i) * 86400000);
    await prisma.deal.create({
      data: { productId: lk.productId, affiliateLinkId: lk.id, currentPrice: (790000 + i * 15000).toString(), oldPrice: (1290000 + i * 20000).toString(), discount: "-" + (35 + i) + "%", startTime: new Date(ct - 86400000), endTime: et, status: "ACTIVE" },
    });
  }

  if (aff.length > 0) {
    await prisma.deal.create({
      data: { productId: aff[0].productId, affiliateLinkId: aff[0].id, currentPrice: "500000", oldPrice: "1000000", discount: "-50%", startTime: new Date(ct - 30 * 86400000), endTime: new Date(ct - 86400000), status: "EXPIRED" },
    });
  }

  for (var i = 0; i < 10; i++) {
    var lk = aff[i % aff.length]; var prd = products.find(function(p) { return p.id === lk.productId; }); if (!prd) continue;
    var rev = reviews.find(function(r) { return r.productId === lk.productId; }) || null;
    await prisma.clickEvent.create({ data: { affiliateLinkId: lk.id, productId: prd.id, reviewId: rev ? rev.id : null, platform: "Shopee", referrer: "direct", userAgent: "seed-bot", ipHash: "ip_seed_" + (i + 1) } });
  }

  for (var i = 0; i < 3; i++) {
    var lk = aff[i];
    await prisma.shortLink.create({ data: { inputUrl: "https://s.shopee.vn/sample-" + (i + 1), resolvedUrl: "https://shopee.vn", platform: "Shopee", shopId: (1000 + i).toString(), itemId: (9000 + i).toString(), productName: products[i].name, affiliateLinkId: lk.id, internalTrackingUrl: "/recommends/" + products[i].slug, status: "CONVERTED" } });
  }

  for (var i = 0; i < 3; i++) {
    await prisma.crawlerJob.create({
      data: { type: i === 0 ? "PRODUCT_BY_URL" : i === 1 ? "PRODUCT_BY_KEYWORD" : "SHORT_LINK_RESOLVE", input: i === 0 ? "https://shopee.vn/sample-product" : i === 1 ? "tai nghe bluetooth" : "https://s.shopee.vn/abc123", status: i === 2 ? "CAPTCHA_REQUIRED" : "SUCCESS", logs: i === 2 ? "Captcha challenge detected" : "Completed", rawResult: "{}" },
    });
  }

  for (var i = 0; i < 3; i++) {
    var g = await prisma.comparisonGroup.create({ data: { title: "Nhom so sanh " + (i + 1), slug: "nhom-so-sanh-" + (i + 1) } });
    await prisma.comparisonItem.create({ data: { comparisonGroupId: g.id, productId: products[i].id, sortOrder: 0 } });
    await prisma.comparisonItem.create({ data: { comparisonGroupId: g.id, productId: products[i + 1].id, sortOrder: 1 } });
  }

  console.log("ReviewX seed complete", { categories: await prisma.category.count(), products: await prisma.product.count(), reviews: await prisma.review.count() });
}

main().catch(function(e) { console.error(e); process.exit(1); }).finally(async function() { await prisma.$disconnect(); });
