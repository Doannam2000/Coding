"use client";

import Link from "next/link";
import { useState } from "react";
import { PageContainer } from "./ui";

const navItems = [
  { label: "Trang chủ", href: "/" },
  { label: "Danh mục", href: "/danh-muc" },
  { label: "Deals", href: "/deals" },
  { label: "So sánh", href: "/so-sanh" },
  { label: "Công cụ", href: "/cong-cu/chon-san-pham" },
];

export function AppHeader() {
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200/50 bg-white/70 backdrop-blur-xl shadow-sm">
      <PageContainer>
        <div className="flex h-16 items-center justify-between gap-4">
          <Link
            href="/"
            className="inline-flex items-center gap-2 rounded-xl px-2 py-1.5 transition-all hover:bg-slate-100/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
            aria-label="ReviewX - Trang chủ"
          >
            <span className="h-7 w-7 shrink-0 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 shadow-sm" aria-hidden="true" />
            <span className="text-lg font-bold text-slate-900">ReviewX</span>
          </Link>

          <nav className="hidden items-center gap-1 lg:flex" aria-label="Điều hướng chính">
            {navItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className="rounded-xl px-3 py-2 text-sm font-medium text-slate-600 transition-all hover:bg-slate-100/80 hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
              >
                {item.label}
              </Link>
            ))}
          </nav>

          <div className="hidden flex-1 items-center justify-end gap-2 lg:flex">
            <input
              aria-label="Tìm kiếm sản phẩm"
              placeholder="Tìm sản phẩm..."
              className="w-full max-w-xs rounded-xl border border-slate-200/70 bg-white/80 backdrop-blur-sm px-4 py-2 text-sm text-slate-900 placeholder:text-slate-400 shadow-sm transition-all focus-visible:border-blue-400 focus-visible:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/20"
            />
            <Link
              href="/admin"
              className="rounded-xl border border-slate-200/70 bg-white/80 backdrop-blur-sm px-3 py-2 text-sm font-medium text-slate-600 shadow-sm transition-all hover:bg-white hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
              aria-label="Trang quản trị"
            >
              Admin
            </Link>
          </div>

          <button
            onClick={() => setMenuOpen(true)}
            className="inline-flex items-center rounded-xl border border-slate-200/70 bg-white/80 backdrop-blur-sm px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition-all hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 lg:hidden"
            aria-label="Mở menu"
            aria-expanded={menuOpen}
          >
            Menu
          </button>
        </div>
      </PageContainer>

      {/* Mobile Menu Drawer */}
      {menuOpen && (
        <div className="fixed inset-0 z-[100] lg:hidden" role="dialog" aria-modal="true" aria-labelledby="mobile-menu-title">
          {/* Backdrop */}
          <div
            className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm transition-opacity"
            onClick={() => setMenuOpen(false)}
            aria-hidden="true"
          />

          {/* Drawer Panel */}
          <div className="absolute right-0 top-0 h-full w-80 max-w-[90vw] overflow-y-auto border-l border-slate-200/50 bg-white/95 backdrop-blur-xl shadow-2xl transition-transform">
            {/* Drawer Header */}
            <div className="sticky top-0 z-10 border-b border-slate-200/50 bg-white/95 backdrop-blur-xl p-4">
              <div className="flex items-center justify-between">
                <h2 id="mobile-menu-title" className="text-base font-semibold text-slate-900">Menu</h2>
                <button
                  onClick={() => setMenuOpen(false)}
                  className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-600 transition-all hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
                  aria-label="Đóng menu"
                >
                  ✕ Đóng
                </button>
              </div>
            </div>

            {/* Search Input */}
            <div className="p-4">
              <input
                aria-label="Tìm kiếm sản phẩm"
                placeholder="Tìm sản phẩm..."
                className="w-full rounded-xl border border-slate-200/70 bg-slate-50 px-4 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 transition-all focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
              />
            </div>

            {/* Navigation Links */}
            <nav className="flex flex-col gap-1 p-4 pt-0" aria-label="Điều hướng di động">
              {navItems.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={() => setMenuOpen(false)}
                  className="rounded-xl px-4 py-3 text-sm font-medium text-slate-700 transition-all hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
                >
                  {item.label}
                </Link>
              ))}

              <div className="my-2 border-t border-slate-200" />

              <Link
                href="/admin"
                onClick={() => setMenuOpen(false)}
                className="rounded-xl bg-slate-100 px-4 py-3 text-sm font-semibold text-slate-800 transition-all hover:bg-slate-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
              >
                ⚙️ Admin
              </Link>
            </nav>
          </div>
        </div>
      )}
    </header>
  );
}
