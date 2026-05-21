export const spacing = {
  xs: 'var(--space-xs)',
  sm: 'var(--space-sm)',
  md: 'var(--space-md)',
  lg: 'var(--space-lg)',
  xl: 'var(--space-xl)',
  '2xl': 'var(--space-2xl)',
} as const;

export const radius = {
  sm: 'var(--radius-sm)',
  md: 'var(--radius-md)',
  lg: 'var(--radius-lg)',
  xl: 'var(--radius-xl)',
  '2xl': 'var(--radius-2xl)',
  '3xl': 'var(--radius-3xl)',
} as const;

export const colors = {
  primary: {
    blue: 'var(--primary-blue)',
    indigo: 'var(--primary-indigo)',
  },
  shopee: 'var(--shopee-orange)',
  success: 'var(--success-green)',
  warning: 'var(--warning-amber)',
  danger: 'var(--danger-red)',
  text: {
    primary: 'var(--text)',
    secondary: 'var(--text-secondary)',
    muted: 'var(--text-muted)',
  },
} as const;

export const glass = {
  base: 'glass',
  strong: 'glass-strong',
} as const;

export const typography = {
  xs: 'text-xs',
  sm: 'text-sm',
  base: 'text-base',
  lg: 'text-lg',
  xl: 'text-xl',
  '2xl': 'text-2xl',
  '3xl': 'text-3xl',
} as const;

export const cardStyles = {
  base: `rounded-2xl border border-slate-200/50 ${glass.base} shadow-sm transition-all duration-300`,
  hover: 'hover:-translate-y-1 hover:shadow-lg',
  interactive: `${glass.base} rounded-2xl border border-slate-200/50 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-lg`,
} as const;

export const buttonStyles = {
  primary: 'inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-all hover:shadow-md hover:opacity-95 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60',
  secondary: 'inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200/70 bg-white/80 backdrop-blur-sm px-4 py-2.5 text-sm font-semibold text-slate-700 shadow-sm transition-all hover:bg-white hover:shadow-md active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60',
  shopee: 'inline-flex items-center justify-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-all hover:bg-orange-600 hover:shadow-md active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-orange-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60',
} as const;

export const inputStyles = {
  base: `w-full rounded-xl border border-slate-200/70 ${glass.base} px-4 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 transition-all focus-visible:border-blue-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/20`,
  error: 'border-red-300 bg-red-50/50 focus-visible:border-red-400 focus-visible:ring-red-500/20',
} as const;

export const badgeStyles = {
  neutral: 'inline-flex items-center rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-semibold text-slate-700',
  success: 'inline-flex items-center rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700',
  warning: 'inline-flex items-center rounded-lg border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700',
  danger: 'inline-flex items-center rounded-lg border border-red-200 bg-red-50 px-2.5 py-1 text-xs font-semibold text-red-700',
  info: 'inline-flex items-center rounded-lg border border-blue-200 bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700',
} as const;
