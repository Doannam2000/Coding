import Link from "next/link";
import { cardStyles } from "@/lib/design-system";

type MiniProductCardProps = {
  name: string;
  price: string;
  href: string;
};

export function MiniProductCard({ name, price, href }: MiniProductCardProps) {
  return (
    <Link
      href={href}
      className={`${cardStyles.interactive} block p-3`}
      aria-label={`Xem sản phẩm ${name}`}
    >
      <p className="truncate text-sm font-medium text-slate-900">{name}</p>
      <p className="mt-1 truncate text-sm text-slate-600">{price}</p>
    </Link>
  );
}
