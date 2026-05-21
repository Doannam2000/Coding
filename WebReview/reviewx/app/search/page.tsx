import { redirect } from "next/navigation";

export default function SearchPage({ searchParams }: { searchParams: Promise<{ q?: string }> }) {
  const params = searchParams as unknown as { q?: string };
  const query = params.q ?? "";
  redirect(query ? `/tim-kiem?q=${encodeURIComponent(query)}` : "/tim-kiem");
}
