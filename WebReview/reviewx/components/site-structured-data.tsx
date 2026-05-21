const organizationSchema = {
  "@context": "https://schema.org",
  "@type": "Organization",
  name: "ReviewX",
  url: "https://reviewx.vn",
  description: "ReviewX giup nguoi dung so sanh, danh gia va chon mua san pham tot hon.",
};

const websiteSchema = {
  "@context": "https://schema.org",
  "@type": "WebSite",
  name: "ReviewX",
  url: "https://reviewx.vn",
  potentialAction: {
    "@type": "SearchAction",
    target: "https://reviewx.vn/tim-kiem?q={search_term_string}",
    "query-input": "required name=search_term_string",
  },
};

export function SiteStructuredData() {
  return (
    <>
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(organizationSchema) }} />
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(websiteSchema) }} />
    </>
  );
}
