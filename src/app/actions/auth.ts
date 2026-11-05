"use server";

import { z } from "zod";
import { headers } from "next/headers";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

const credentials = z.object({
  email: z.email().max(254),
  password: z.string().min(8).max(128)
});

function safeNext(value: FormDataEntryValue | null) {
  const path = typeof value === "string" ? value : "";
  return path.startsWith("/") && !path.startsWith("//") ? path : "/dashboard";
}

export async function signIn(formData: FormData) {
  const parsed = credentials.safeParse({
    email: formData.get("email"),
    password: formData.get("password")
  });
  if (!parsed.success) redirect("/login?error=Check%20your%20email%20and%20password");
  const supabase = await createClient();
  const { error } = await supabase.auth.signInWithPassword(parsed.data);
  if (error) redirect("/login?error=Email%20or%20password%20is%20incorrect");
  redirect(safeNext(formData.get("next")));
}

export async function signUp(formData: FormData) {
  const parsed = credentials.extend({
    fullName: z.string().trim().min(2).max(80)
  }).safeParse({
    email: formData.get("email"),
    password: formData.get("password"),
    fullName: formData.get("fullName")
  });
  if (!parsed.success) redirect("/signup?error=Enter%20a%20valid%20name%2C%20email%20and%20password%20of%208%2B%20characters");
  const supabase = await createClient();
  const requestHeaders = await headers();
  const origin = requestHeaders.get("origin") || process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000";
  const { data, error } = await supabase.auth.signUp({
    email: parsed.data.email,
    password: parsed.data.password,
    options: {
      data: { full_name: parsed.data.fullName },
      emailRedirectTo: new URL("/auth/callback?next=/dashboard", origin).toString()
    }
  });
  if (error) redirect("/signup?error=Could%20not%20create%20your%20account.%20Please%20try%20again");
  if (data.session) redirect("/dashboard");
  redirect("/login?message=Check%20your%20email%20to%20confirm%20your%20account");
}

export async function requestPasswordReset(formData: FormData) {
  const email = z.email().safeParse(formData.get("email"));
  if (!email.success) redirect("/forgot-password?error=Enter%20a%20valid%20email");
  const supabase = await createClient();
  const requestHeaders = await headers();
  const origin = requestHeaders.get("origin") || process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000";
  const { error } = await supabase.auth.resetPasswordForEmail(email.data, {
    redirectTo: new URL("/auth/callback?next=/auth/update-password", origin).toString()
  });
  if (error) redirect("/forgot-password?error=Unable%20to%20send%20the%20reset%20email");
  redirect("/forgot-password?message=If%20that%20email%20has%20an%20account%2C%20a%20reset%20link%20has%20been%20sent");
}

export async function updatePassword(formData: FormData) {
  const password = z.string().min(8).max(128).safeParse(formData.get("password"));
  if (!password.success) redirect("/auth/update-password?error=Password%20must%20be%20at%20least%208%20characters");
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) redirect("/login?error=Your%20reset%20session%20expired");
  const { error } = await supabase.auth.updateUser({ password: password.data });
  if (error) redirect("/auth/update-password?error=Could%20not%20update%20password");
  redirect("/login?message=Password%20updated.%20Please%20sign%20in");
}

export async function signOut() {
  const supabase = await createClient();
  await supabase.auth.signOut();
  redirect("/login?message=You%20have%20been%20signed%20out");
}