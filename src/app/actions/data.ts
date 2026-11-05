"use server";

import { z } from "zod";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

const reminderSchema = z.object({
  title: z.string().trim().min(1).max(180),
  dueAt: z.iso.datetime({ offset: true }),
  category: z.string().trim().min(1).max(60),
  priority: z.enum(["low","normal","high"])
});
const expenseSchema = z.object({
  description: z.string().trim().min(1).max(180),
  amount: z.coerce.number().positive().max(999999999),
  kind: z.enum(["income","expense"]),
  occurredOn: z.iso.date(),
  paymentMethod: z.string().trim().max(60).optional()
});

async function requireUser() {
  const supabase = await createClient();
  const { data: { user }, error } = await supabase.auth.getUser();
  if (error || !user) redirect("/login");
  return { supabase, user };
}

export async function createReminder(formData: FormData) {
  const parsed = reminderSchema.safeParse({
    title: formData.get("title"),
    dueAt: formData.get("dueAt"),
    category: formData.get("category") || "Personal",
    priority: formData.get("priority") || "normal"
  });
  if (!parsed.success) redirect("/dashboard?error=Check%20the%20reminder%20details");
  const { supabase, user } = await requireUser();
  const { error } = await supabase.from("reminders").insert({
    user_id: user.id,
    title: parsed.data.title,
    due_at: parsed.data.dueAt,
    category: parsed.data.category,
    priority: parsed.data.priority
  });
  if (error) redirect("/dashboard?error=Could%20not%20save%20the%20reminder");
  revalidatePath("/dashboard");
}

export async function completeReminder(formData: FormData) {
  const id = z.uuid().safeParse(formData.get("id"));
  if (!id.success) redirect("/dashboard?error=Invalid%20reminder");
  const { supabase, user } = await requireUser();
  const { error } = await supabase.from("reminders")
    .update({ completed_at: new Date().toISOString() })
    .eq("id", id.data).eq("user_id", user.id);
  if (error) redirect("/dashboard?error=Could%20not%20update%20the%20reminder");
  revalidatePath("/dashboard");
}

export async function createExpense(formData: FormData) {
  const parsed = expenseSchema.safeParse({
    description: formData.get("description"),
    amount: formData.get("amount"),
    kind: formData.get("kind") || "expense",
    occurredOn: formData.get("occurredOn"),
    paymentMethod: formData.get("paymentMethod") || ""
  });
  if (!parsed.success) redirect("/dashboard?error=Check%20the%20expense%20details");
  const { supabase, user } = await requireUser();
  const { error } = await supabase.from("expenses").insert({
    user_id: user.id,
    description: parsed.data.description,
    amount: parsed.data.amount,
    kind: parsed.data.kind,
    occurred_on: parsed.data.occurredOn,
    payment_method: parsed.data.paymentMethod || null
  });
  if (error) redirect("/dashboard?error=Could%20not%20save%20the%20transaction");
  revalidatePath("/dashboard");
}