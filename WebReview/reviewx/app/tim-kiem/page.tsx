import { getSearchItemsFromDb } from "@/lib/search-db";
import SearchClient from "./search-client";

type SearchPageProps = {
  searchParams: Promise<{ q?: string; mode?: string }>;
};

export default async function SearchPage({ searchParams }: SearchPageProps) {
  const params = await searchParams;
  const allItems = await getSearchItemsFromDb();
  return <SearchClient initialQ={params.q ?? ""} initialMode={params.mode ?? ""} allItems={allItems} />;
}
