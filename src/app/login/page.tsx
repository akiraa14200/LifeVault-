import Link from "next/link";
import { signIn } from "@/app/actions/auth";

type Props = { searchParams: Promise<{ error?: string; message?: string; setup?: string; next?: string }> };

export default async function LoginPage({ searchParams }: Props) {
  const params = await searchParams;
  return <main className="soft-grid flex min-h-screen items-center justify-center px-4 py-10">
    <section className="glass w-full max-w-md rounded-[28px] p-7 sm:p-9">
      <Link href="/" className="mb-8 flex items-center gap-3"><span className="grid h-11 w-11 place-items-center rounded-2xl bg-[#234c3d] text-lg font-bold text-[#d8f2e4]">L</span><span className="text-xl font-bold tracking-tight">lifevault<span className="text-[#7db89b]">.</span></span></Link>
      <p className="mb-2 text-[11px] font-semibold uppercase tracking-[1.8px] text-[#7ca78c]">WELCOME BACK</p>
      <h1 className="text-2xl font-semibold tracking-tight">Your space is waiting.</h1>
      <p className="mb-6 mt-2 text-sm text-[#829087]">Sign in to keep life’s important details in one place.</p>
      {params.error&&<p role="alert" className="mb-4 rounded-xl bg-red-50 px-3 py-2 text-xs text-red-700">{params.error}</p>}
      {params.message&&<p role="status" className="mb-4 rounded-xl bg-emerald-50 px-3 py-2 text-xs text-emerald-800">{params.message}</p>}
      {params.setup&&<p className="mb-4 rounded-xl bg-amber-50 px-3 py-2 text-xs text-amber-800">Add your Supabase project URL and publishable/anon key to Vercel or .env.local first.</p>}
      <form action={signIn} className="space-y-4">
        <input type="hidden" name="next" value={params.next||"/dashboard"}/>
        <label className="block text-xs font-medium text-[#536258]">Email address<input name="email" type="email" autoComplete="email" required maxLength={254} className="mt-2 w-full rounded-xl border border-[#e2e9e2] bg-white/80 px-4 py-3 text-sm outline-none focus:border-[#83ad91]" placeholder="you@example.com"/></label>
        <label className="block text-xs font-medium text-[#536258]">Password<input name="password" type="password" autoComplete="current-password" required minLength={8} maxLength={128} className="mt-2 w-full rounded-xl border border-[#e2e9e2] bg-white/80 px-4 py-3 text-sm outline-none focus:border-[#83ad91]" placeholder="Your password"/></label>
        <div className="text-right"><Link href="/forgot-password" className="text-xs font-medium text-[#63816e] hover:underline">Forgot password?</Link></div>
        <button className="w-full rounded-xl bg-[#234c3d] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#193d30]">Sign in</button>
      </form>
      <p className="mt-6 text-center text-xs text-[#829087]">New to LifeVault? <Link href="/signup" className="font-semibold text-[#315b46]">Create an account</Link></p>
      <p className="mt-8 text-center text-[10px] text-[#a1aaa4]">Your personal space · Private by design</p>
    </section>
  </main>;
}