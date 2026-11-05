import { updatePassword } from "@/app/actions/auth";

type Props = { searchParams: Promise<{ error?: string }> };

export default async function UpdatePasswordPage({ searchParams }: Props) {
  const params = await searchParams;
  return <main className="soft-grid flex min-h-screen items-center justify-center px-4 py-10">
    <section className="glass w-full max-w-md rounded-[28px] p-7 sm:p-9">
      <div className="text-xl font-bold tracking-tight">lifevault<span className="text-[#7db89b]">.</span></div>
      <h1 className="mt-8 text-2xl font-semibold tracking-tight">Choose a new password.</h1>
      <p className="mb-6 mt-2 text-sm text-[#829087]">Use at least 8 characters.</p>
      {params.error&&<p role="alert" className="mb-4 rounded-xl bg-red-50 px-3 py-2 text-xs text-red-700">{params.error}</p>}
      <form action={updatePassword} className="space-y-4">
        <label className="block text-xs font-medium text-[#536258]">New password<input name="password" type="password" autoComplete="new-password" required minLength={8} maxLength={128} className="mt-2 w-full rounded-xl border border-[#e2e9e2] bg-white/80 px-4 py-3 text-sm outline-none focus:border-[#83ad91]"/></label>
        <button className="w-full rounded-xl bg-[#234c3d] px-4 py-3 text-sm font-semibold text-white">Update password</button>
      </form>
    </section>
  </main>;
}