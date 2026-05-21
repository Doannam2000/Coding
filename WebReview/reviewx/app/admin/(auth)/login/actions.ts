"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { verifyAdminSessionToken } from "@/lib/admin-auth";

export async function redirectIfAuthenticated() {
  const cookieStore = await cookies();
  const token = cookieStore.get("reviewx_admin_session")?.value;
  const role = cookieStore.get("reviewx_admin_role")?.value;
  if (verifyAdminSessionToken(token) && role === "admin") {
    redirect("/admin");
  }
}