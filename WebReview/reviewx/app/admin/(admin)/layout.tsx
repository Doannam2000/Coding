import type { Metadata } from "next";
import Link from "next/link";
import { cookies, headers } from "next/headers";
import { redirect } from "next/navigation";
import { verifyAdminSessionToken } from "@/lib/admin-auth";

export const metadata: Metadata = {
  robots: {
    index: false,
    follow: false,
  },
};

const navItems = [
  { href: "/admin", label: "Dashboard" },
  { href: "/admin/products", label: "Products" },
  { href: "/admin/reviews", label: "Reviews" },
  { href: "/admin/deals", label: "Deals" },
  { href: "/admin/categories", label: "Categories" },
  { href: "/admin/brands", label: "Brands" },
  { href: "/admin/affiliate-links", label: "Affiliate links" },
  { href: "/admin/analytics", label: "Analytics" },
] as const;

function breadcrumbFromPath(pathname: string) {
  const parts = pathname.split("/").filter(Boolean);
  if (parts.length <= 1) return ["Dashboard"];
  return parts.slice(1).map((part) => part.replace(/-/g, " ").replace(/\b\w/g, (ch) => ch.toUpperCase()));
}

function isActivePath(pathname: string, href: string) {
  if (href === "/admin") return pathname === "/admin";
  return pathname === href || pathname.startsWith(`${href}/`);
}

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const cookieStore = await cookies();
  const token = cookieStore.get("reviewx_admin_session")?.value;
  const role = cookieStore.get("reviewx_admin_role")?.value;
  const isAuthenticated = verifyAdminSessionToken(token) && role === "admin";

  if (!isAuthenticated) {
    const reqHeaders = await headers();
    const pathname = reqHeaders.get("x-invoke-path") || "/admin";
    redirect(`/admin/login?from=${encodeURIComponent(pathname)}`);
  }

  const pathCookie = cookieStore.get("reviewx_admin_last_path")?.value ?? "/admin";
  const breadcrumb = breadcrumbFromPath(pathCookie);

  async function logoutAction() {
    "use server";
    const cookieStore = await cookies();
    cookieStore.set("reviewx_admin_session", "", {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      path: "/",
      maxAge: 0,
    });
    cookieStore.set("reviewx_admin_role", "", {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production",
      path: "/",
      maxAge: 0,
    });
    redirect("/admin/login");
  }

  return (
    <div className="min-h-screen bg-slate-100">
      <div className="mx-auto max-w-[1440px] px-3 py-3 sm:px-4">
        <div className="rounded-3xl border border-slate-200/70 bg-white p-3 shadow-sm sm:p-4">
          <div className="grid gap-3 lg:grid-cols-[250px_minmax(0,1fr)]">
            <aside className="rounded-2xl border border-slate-200 bg-white p-3 lg:sticky lg:top-4 lg:h-fit">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Admin</p>
              <h1 className="mt-1 text-lg font-bold text-slate-900">ReviewX CMS</h1>

              <nav className="mt-3 space-y-1.5">
                {navItems.map((item) => {
                  const active = isActivePath(pathCookie, item.href);
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      className={`block rounded-xl border px-3 py-2 text-sm font-medium transition ${active ? "border-blue-200 bg-blue-50 text-blue-700" : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50"}`}
                    >
                      {item.label}
                    </Link>
                  );
                })}
              </nav>

              <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-xs font-medium text-amber-700 lg:hidden">
                Trải nghiệm admin tối ưu trên desktop; trên mobile dùng menu nhanh ở đầu trang.
              </p>
            </aside>

            <div className="space-y-3">
              <header className="rounded-2xl border border-slate-200 bg-white p-3">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="flex flex-wrap items-center gap-1 text-xs text-slate-500">
                      {breadcrumb.map((item, index) => (
                        <span key={`${item}-${index}`} className="inline-flex items-center gap-1">
                          {index > 0 ? <span>/</span> : null}
                          <span>{item}</span>
                        </span>
                      ))}
                    </div>
                    <p className="mt-1 text-sm font-semibold text-slate-900">Bảng quản trị nội dung và affiliate</p>
                  </div>

                  <details className="group relative">
                    <summary className="cursor-pointer list-none rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">
                      Admin menu
                    </summary>
                    <div className="absolute right-0 z-20 mt-2 w-48 rounded-xl border border-slate-200 bg-white p-2 shadow-md">
                      <Link href="/admin" className="block rounded-lg px-2 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100">
                        Dashboard
                      </Link>
                      <Link href="/admin/products" className="block rounded-lg px-2 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100">
                        Products
                      </Link>
                      <form action={logoutAction}>
                        <button type="submit" className="mt-1 w-full rounded-lg border border-red-200 bg-red-50 px-2 py-2 text-left text-xs font-semibold text-red-700 hover:bg-red-100">
                          Đăng xuất
                        </button>
                      </form>
                    </div>
                  </details>
                </div>
              </header>

              <main>{children}</main>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
