import type { Metadata } from "next";
import "./globals.css";
import { AppHeader } from "@/components/app-header";
import { AppFooter } from "@/components/app-footer";
import { RouteTitle } from "@/components/route-title";
import { SiteStructuredData } from "@/components/site-structured-data";

export const metadata: Metadata = {
  title: "ReviewX",
  description: "Check trước khi mua. Mua gì cũng đáng.",
  metadataBase: new URL("https://reviewx.vn"),
  alternates: {
    canonical: "/",
  },
  robots: {
    index: true,
    follow: true,
  },
  openGraph: {
    title: "ReviewX",
    description: "Check trước khi mua. Mua gì cũng đáng.",
    url: "/",
    images: [
      {
        url: "https://images.unsplash.com/photo-1519389950473-47ba0277781c?q=80&w=1200&auto=format&fit=crop",
        width: 1200,
        height: 630,
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "ReviewX",
    description: "Check trÆ°á»›c khi mua. Mua gÃ¬ cÅ©ng Ä‘Ã¡ng.",
    images: ["https://images.unsplash.com/photo-1519389950473-47ba0277781c?q=80&w=1200&auto=format&fit=crop"],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" className="h-full antialiased">
      <body className="min-h-full bg-slate-50 text-slate-900">
        <RouteTitle />
        <SiteStructuredData />
        <div className="pointer-events-none fixed inset-0 -z-10 overflow-hidden" aria-hidden="true">
          <div className="absolute -top-24 -left-20 h-96 w-96 rounded-full bg-blue-200/30 blur-3xl" />
          <div className="absolute top-40 right-0 h-96 w-96 rounded-full bg-indigo-200/30 blur-3xl" />
          <div className="absolute bottom-10 left-1/3 h-80 w-80 rounded-full bg-pink-100/30 blur-3xl" />
        </div>
        <AppHeader />
        <main className="flex-1 py-8 sm:py-10">{children}</main>
        <AppFooter />
      </body>
    </html>
  );
}
