import Link from "next/link";
import { signUp } from "@/app/actions/auth";

type Props = { searchParams: Promise<{ error?: string }> };

export default async function SignupPage({ searchParams }: Props) {
  const params = await searchParams;
  return <main className="soft-grid flex min-h-screen items-center justify-center px-4 py-10">
    <section className="glass w-full max-w-md rounded-[28px] p-7 sm:p-9">
      <Link href="/login" className="mb-8 flex items-center gap-3"><span className="grid h-11 w-11 place-items-center rounded-2xl bg-[#234c3d] text-lg font-bold text-[#d8f2e4]">L</span><span className="text-xl font-bold tracking-tight">lifevault<span className="text-[#7db89b]">.</span></span></Link>
      <p className="mb-2 text-[11px] font-semibold uppercase tracking-[1.8px] text-[#7ca78c]">A FRESH START</p>
      <h1 className="text-2xl font-semibold tracking-tight">Create your LifeVault.</h1>
      <p className="mb-6 mt-2 text-sm text-[#829087]">One thoughtful place for the details of everyday life.</p>
      {params.error&&<p role="alert" className="mb-4 rounded-xl bg-red-50 px-3 py-2 text-xs text-red-700">{params.error}</p>}
      <form action={signUp} className="space-y-4">
        <label className="block text-xs font-medium text-[#536258]">Your name<input name="fullName" type="text" autoComplete="name" required minLength={2} maxLength={80} className="mt-2 w-full rounded-xl border border-[#e2e9e2] bg-white/80 px-4 py-3 text-sm outline-none focus:border-[#83ad91]" placeholder="Your name"/></label>
        <label className="block text-xs font-medium text-[#536258]">Email address<input name="email" type="email" autoComplete="email" required maxLength={254} className="mt-2 w-full rounded-xl border border-[#e2e9e2] bg-white/80 px-4 py-3 text-sm outline-none focus:border-[#83ad91]" placeholder="you@example.com"/></label>
        <label className="block text-xs font-medium text-[#536258]">Create password<input name="password" type="password" autoComplete="new-password" required minLength={8} maxLength={128} className="mt-2 w-full rounded-xl border border-[#e2e9e2] bg-white/80 px-4 py-3 text-sm outline-none focus:border-[#83ad91]" placeholder="At least 8 characters"/><span className="mt-1 block text-[10px] text-[#9aa69e]">Use a unique password you don’t use on other sites.</span></label>
        <button className="w-full rounded-xl bg-[#234c3d] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#193d30]">Create account</button>
      </form>
      <p className="mt-6 text-center text-xs text-[#829087]">Already have an account? <Link href="/login" className="font-semibold text-[#315b46]">Sign in</Link></p>
    </section>
  </main>;
}