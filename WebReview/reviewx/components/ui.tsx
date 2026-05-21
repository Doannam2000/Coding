import Link from "next/link";
import { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from "react";
import { buttonStyles, inputStyles, badgeStyles, cardStyles } from "@/lib/design-system";

type ButtonProps = {
  children: ReactNode;
  href?: string;
  className?: string;
  disabled?: boolean;
  ariaLabel?: string;
};

type GlassSearchInputProps = {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  ariaLabel?: string;
  className?: string;
};

type TextInputProps = InputHTMLAttributes<HTMLInputElement> & {
  error?: string;
};

type SelectInputProps = SelectHTMLAttributes<HTMLSelectElement> & {
  options: Array<{ label: string; value: string }>;
  error?: string;
};

type CheckboxFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, "type"> & {
  label: string;
  description?: string;
};

type RadioOption = {
  label: string;
  value: string;
  description?: string;
};

type RadioGroupProps = {
  name: string;
  value: string;
  options: RadioOption[];
  onChange: (value: string) => void;
  className?: string;
};

type TabsProps = {
  value: string;
  tabs: Array<{ label: string; value: string }>;
  onChange: (value: string) => void;
  className?: string;
};

type ModalProps = {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
};

type DrawerProps = {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  side?: "left" | "right";
};

type ToastTone = "success" | "error" | "info";

type ToastNoticeProps = {
  message: string;
  tone?: ToastTone;
  onClose?: () => void;
  className?: string;
};

type BadgeTone = "neutral" | "success" | "warning" | "danger" | "info";

type PaginationProps = {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
};

type BreadcrumbItem = {
  label: string;
  href?: string;
};

type TooltipProps = {
  content: string;
  children: ReactNode;
  className?: string;
};

export function PageContainer({ children }: { children: ReactNode }) {
  return <div className="mx-auto w-full max-w-[1360px] px-4 sm:px-6 lg:px-8">{children}</div>;
}

export function SectionHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="space-y-2">
      <h2 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">{title}</h2>
      {subtitle ? <p className="text-slate-600">{subtitle}</p> : null}
    </div>
  );
}

export function GlassSearchInput({
  value,
  onChange,
  placeholder = "Bạn đang tìm sản phẩm gì?",
  ariaLabel = "Tìm kiếm sản phẩm",
  className = "",
}: GlassSearchInputProps) {
  return (
    <input
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      aria-label={ariaLabel}
      className={`${inputStyles.base} py-3 ${className}`}
    />
  );
}

export function TextInput({ className = "", error, ...props }: TextInputProps) {
  return (
    <div className="space-y-1">
      <input
        {...props}
        className={`${inputStyles.base} ${error ? inputStyles.error : ""} ${className}`}
      />
      {error ? <p className="text-xs font-medium text-red-600">{error}</p> : null}
    </div>
  );
}

export function SelectInput({ className = "", options, error, ...props }: SelectInputProps) {
  return (
    <div className="space-y-1">
      <select
        {...props}
        className={`${inputStyles.base} ${error ? inputStyles.error : ""} ${className}`}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error ? <p className="text-xs font-medium text-red-600">{error}</p> : null}
    </div>
  );
}

export function CheckboxField({ label, description, className = "", ...props }: CheckboxFieldProps) {
  return (
    <label className={`flex cursor-pointer items-start gap-2 rounded-xl border border-slate-200/70 ${cardStyles.base} p-3 transition-all hover:shadow-md ${className}`}>
      <input
        {...props}
        type="checkbox"
        className="mt-0.5 h-4 w-4 rounded border-slate-300 text-blue-600 transition focus-visible:ring-2 focus-visible:ring-blue-500"
      />
      <span className="space-y-0.5">
        <span className="block text-sm font-medium text-slate-800">{label}</span>
        {description ? <span className="block text-xs text-slate-500">{description}</span> : null}
      </span>
    </label>
  );
}

export function RadioGroup({ name, value, options, onChange, className = "" }: RadioGroupProps) {
  return (
    <div className={`grid gap-2 ${className}`} role="radiogroup" aria-label={name}>
      {options.map((option) => {
        const checked = value === option.value;
        return (
          <label
            key={option.value}
            className={`flex cursor-pointer items-start gap-2 rounded-xl border p-3 transition ${
              checked ? "border-blue-300 bg-blue-50/60" : "border-slate-200 bg-white"
            }`}
          >
            <input
              type="radio"
              name={name}
              value={option.value}
              checked={checked}
              onChange={() => onChange(option.value)}
              className="mt-0.5 h-4 w-4 border-slate-300 text-blue-600 focus-visible:ring-2 focus-visible:ring-blue-500"
            />
            <span className="space-y-0.5">
              <span className="block text-sm font-medium text-slate-800">{option.label}</span>
              {option.description ? <span className="block text-xs text-slate-500">{option.description}</span> : null}
            </span>
          </label>
        );
      })}
    </div>
  );
}

export function Tabs({ value, tabs, onChange, className = "" }: TabsProps) {
  return (
    <div className={`inline-flex flex-wrap rounded-xl border border-slate-200 bg-white p-1 ${className}`} role="tablist">
      {tabs.map((tab) => {
        const active = value === tab.value;
        return (
          <button
            key={tab.value}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(tab.value)}
            className={`rounded-lg px-3 py-1.5 text-sm font-medium transition ${
              active ? "bg-slate-900 text-white" : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}

export function Modal({ open, title, onClose, children, footer }: ModalProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button type="button" aria-label="Đóng modal" onClick={onClose} className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" />
      <div className={`relative z-10 w-full max-w-lg ${cardStyles.base} overflow-hidden`}>
        <div className="flex items-center justify-between border-b border-slate-200/50 px-5 py-4">
          <h3 className="text-base font-semibold text-slate-900">{title}</h3>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng"
            className={`${buttonStyles.secondary} px-3 py-1.5 text-xs`}
          >
            Đóng
          </button>
        </div>
        <div className="px-5 py-4">{children}</div>
        {footer ? <div className="border-t border-slate-200/50 px-5 py-4">{footer}</div> : null}
      </div>
    </div>
  );
}

export function Drawer({ open, title, onClose, children, side = "right" }: DrawerProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50">
      <button type="button" aria-label="Close drawer overlay" onClick={onClose} className="absolute inset-0 bg-slate-900/40" />
      <aside
        className={`absolute top-0 h-full w-full max-w-md bg-white shadow-xl ${side === "right" ? "right-0" : "left-0"}`}
        role="dialog"
        aria-modal="true"
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
          <h3 className="text-base font-semibold text-slate-900">{title}</h3>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg border border-slate-200 px-2 py-1 text-xs font-semibold text-slate-600 transition hover:bg-slate-100"
          >
            Close
          </button>
        </div>
        <div className="h-[calc(100%-57px)] overflow-y-auto px-4 py-4">{children}</div>
      </aside>
    </div>
  );
}

export function ToastNotice({ message, tone = "info", onClose, className = "" }: ToastNoticeProps) {
  const toneClass =
    tone === "success"
      ? "border-emerald-200 bg-emerald-50 text-emerald-800"
      : tone === "error"
        ? "border-red-200 bg-red-50 text-red-800"
        : "border-blue-200 bg-blue-50 text-blue-800";

  return (
    <div className={`inline-flex min-h-11 items-center gap-3 rounded-xl border px-3 py-2 text-sm shadow-sm ${toneClass} ${className}`}>
      <span className="font-medium">{message}</span>
      {onClose ? (
        <button
          type="button"
          onClick={onClose}
          className="ml-auto rounded-md px-1.5 py-0.5 text-xs font-semibold transition hover:bg-black/5"
        >
          Close
        </button>
      ) : null}
    </div>
  );
}

export function Badge({ children, tone = "neutral", className = "" }: { children: ReactNode; tone?: BadgeTone; className?: string }) {
  const toneClass = badgeStyles[tone] || badgeStyles.neutral;
  return <span className={`${toneClass} ${className}`}>{children}</span>;
}

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={`${cardStyles.base} p-5 ${className}`}>{children}</div>;
}

export function Pagination({ currentPage, totalPages, onPageChange, className = "" }: PaginationProps) {
  if (totalPages <= 1) return null;

  const pages = Array.from({ length: totalPages }, (_, index) => index + 1);

  return (
    <nav className={`flex flex-wrap items-center gap-2 ${className}`} aria-label="Pagination">
      <button
        type="button"
        onClick={() => onPageChange(Math.max(1, currentPage - 1))}
        disabled={currentPage === 1}
        className="inline-flex min-h-10 items-center rounded-lg border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
      >
        Prev
      </button>
      {pages.map((page) => (
        <button
          key={page}
          type="button"
          onClick={() => onPageChange(page)}
          aria-current={page === currentPage ? "page" : undefined}
          className={`inline-flex min-h-10 min-w-10 items-center justify-center rounded-lg border px-3 text-sm font-medium transition ${
            page === currentPage ? "border-slate-900 bg-slate-900 text-white" : "border-slate-200 bg-white text-slate-700 hover:bg-slate-100"
          }`}
        >
          {page}
        </button>
      ))}
      <button
        type="button"
        onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
        disabled={currentPage === totalPages}
        className="inline-flex min-h-10 items-center rounded-lg border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
      >
        Next
      </button>
    </nav>
  );
}

export function Breadcrumb({ items, className = "" }: { items: BreadcrumbItem[]; className?: string }) {
  return (
    <nav className={`flex flex-wrap items-center gap-2 text-sm text-slate-500 ${className}`} aria-label="Breadcrumb">
      {items.map((item, index) => (
        <span key={`${item.label}-${index}`} className="inline-flex items-center gap-2">
          {item.href ? <Link href={item.href} className="hover:text-slate-700">{item.label}</Link> : <span className="font-medium text-slate-700">{item.label}</span>}
          {index < items.length - 1 ? <span>/</span> : null}
        </span>
      ))}
    </nav>
  );
}

export function Tooltip({ content, children, className = "" }: TooltipProps) {
  return (
    <span className={`group relative inline-flex ${className}`}>
      {children}
      <span className="pointer-events-none absolute left-1/2 top-full z-20 mt-2 -translate-x-1/2 whitespace-nowrap rounded-md bg-slate-900 px-2 py-1 text-xs font-medium text-white opacity-0 shadow-lg transition group-hover:opacity-100 group-focus-within:opacity-100">
        {content}
      </span>
    </span>
  );
}

export function GradientButton({ children, href, className = "", disabled = false, ariaLabel }: ButtonProps) {
  const classes = `${buttonStyles.primary} ${className}`;

  if (href) {
    return (
      <Link href={href} className={classes} aria-label={ariaLabel} aria-disabled={disabled}>
        {children}
      </Link>
    );
  }

  return (
    <button className={classes} disabled={disabled} aria-label={ariaLabel}>
      {children}
    </button>
  );
}

export function ShopeeCTAButton({
  href = "#",
  children = "Xem giá Shopee",
  disabled = false,
  className = "",
  ariaLabel = "Xem giá trên Shopee",
}: {
  href?: string;
  children?: ReactNode;
  disabled?: boolean;
  className?: string;
  ariaLabel?: string;
}) {
  const classes = `${buttonStyles.shopee} ${disabled ? "pointer-events-none opacity-60" : ""} ${className}`;

  return (
    <Link href={href} className={classes} aria-disabled={disabled} aria-label={ariaLabel}>
      {children}
    </Link>
  );
}

export function ProductScoreBadge({ score }: { score: number }) {
  const tone = score >= 8.5 ? "success" : score >= 7 ? "warning" : "danger";
  return (
    <span className={`${badgeStyles[tone]} px-3 py-1.5`}>
      {score.toFixed(1)} / 10
    </span>
  );
}

export function LoadingSkeleton({ className = "h-6 w-full", animate = true }: { className?: string; animate?: boolean }) {
  return <div className={`${animate ? "animate-pulse" : ""} rounded-xl bg-slate-200/90 ${className}`} />;
}

export function EmptyState({ title, message }: { title: string; message: string }) {
  return (
    <div className={`${cardStyles.base} p-8 text-center`}>
      <div className="mx-auto mb-3 inline-flex h-12 w-12 items-center justify-center rounded-full border border-slate-200/70 bg-slate-50/80 text-slate-500">
        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <h3 className="text-lg font-semibold text-slate-900">{title}</h3>
      <p className="mt-2 text-sm text-slate-600">{message}</p>
    </div>
  );
}

export function ErrorState({ title, message }: { title: string; message: string }) {
  return (
    <div className="rounded-2xl border border-red-200/70 bg-red-50/80 backdrop-blur-sm p-8 text-center shadow-sm">
      <div className="mx-auto mb-3 inline-flex h-12 w-12 items-center justify-center rounded-full border border-red-200 bg-white/90 text-red-600">
        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      </div>
      <h3 className="text-lg font-semibold text-red-700">{title}</h3>
      <p className="mt-2 text-sm text-red-600">{message}</p>
    </div>
  );
}
