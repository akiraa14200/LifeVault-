import Link from "next/link";
import { requestPasswordReset } from "@/app/actions/auth";

type Props = { searchParams: Promise<{ error?: string; message?: string }> };

export default async function ForgotPasswordPage({ searchParams }: Props) {
  const params = await searchParams;
  return <main className="soft-grid flex min-h-screen items-center justify-center px-4 py-10">
    <section className="glass w-full max-w-md rounded-[28px] p-7 sm:p-9">
      <Link href="/login" className="text-xl font-bold tracking-tight">lifevault<span className="text-[#7db89b]">.</span></Link>
      <h1 className="mt-8 text-2xl font-semibold tracking-tight">Reset your password.</h1>
      <p className="mb-6 mt-2 text-sm text-[#829087]">We’ll send a secure reset link if an account exists for that email.</p>
      {params.error&&<p role="alert" className="mb-4 rounded-xl bg-red-50 px-3 py-2 text-xs text-red-700">{params.error}</p>}
      {params.message&&<p role="status" className="mb-4 rounded-xl bg-emerald-50 px-3 py-2 text-xs text-emerald-800">{params.message}</p>}
      <form action={requestPasswordReset} className="space-y-4">
        <label className="block text-xs font-medium text-[#536258]">Email address<input name="email" type="email" required maxLength={254} className="mt-2 w-full rounded-xl border border-[#e2e9e2] bg-white/80 px-4 py-3 text-sm outline-none focus:border-[#83ad91]" placeholder="you@example.com"/></label>
        <button className="w-full rounded-xl bg-[#234c3d] px-4 py-3 text-sm font-semibold text-white">Send reset link</button>
      </form>
      <Link href="/login" className="mt-6 block text-center text-xs font-semibold text-[#63816e]">Back to sign in</Link>
    </section>
  </main>;
}