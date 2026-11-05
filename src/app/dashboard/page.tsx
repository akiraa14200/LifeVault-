import Link from "next/link";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { completeReminder, createExpense, createReminder } from "@/app/actions/data";
import { signOut } from "@/app/actions/auth";

export const dynamic = "force-dynamic";

type Props = { searchParams: Promise<{ error?: string }> };

const currency = (value: number) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(value);

export default async function DashboardPage({ searchParams }: Props) {
  const params = await searchParams;
  const supabase = await createClient();
  const { data: { user }, error: authError } = await supabase.auth.getUser();
  if (authError || !user) redirect("/login");

  const [remindersResult, expensesResult, docsResult] = await Promise.all([
    supabase.from("reminders").select("id,title,due_at,priority,completed_at,category").order("due_at",{ascending:true}).limit(8),
    supabase.from("expenses").select("id,description,amount,kind,occurred_on").order("occurred_on",{ascending:false}).limit(8),
    supabase.from("documents").select("id",{count:"exact",head:true})
  ]);
  const reminders = remindersResult.data || [];
  const expenses = expensesResult.data || [];
  const monthStart = new Date(); monthStart.setDate(1); monthStart.setHours(0,0,0,0);
  const monthExpenses = expenses.filter(item=>item.kind==="expense" && new Date(item.occurred_on+"T00:00:00")>=monthStart).reduce((sum,item)=>sum+Number(item.amount),0);
  const name = user.user_metadata?.full_name || user.email?.split("@")[0] || "there";

  return <main className="min-h-screen px-4 py-7 md:px-10 md:py-10">
    <div className="mx-auto max-w-6xl">
      <header className="mb-8 flex flex-wrap items-center justify-between gap-4">
        <div><Link href="/" className="text-sm font-bold tracking-tight">lifevault<span className="text-[#7db89b]">.</span></Link><h1 className="mt-3 text-2xl font-semibold tracking-tight">Welcome, {name}.</h1><p className="mt-1 text-sm text-[#829087]">Your private workspace, connected to your account.</p></div>
        <div className="flex items-center gap-3"><span className="max-w-[190px] truncate text-xs text-[#829087]">{user.email}</span><form action={signOut}><button className="rounded-full border border-[#e1e9e1] bg-white px-4 py-2 text-xs font-semibold text-[#536258]">Sign out</button></form></div>
      </header>
      {params.error&&<p role="alert" className="mb-5 rounded-xl bg-red-50 px-4 py-3 text-xs text-red-700">{params.error}</p>}
      <section className="mb-6 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <div className="metric-card"><p className="text-xs text-[#829087]">Saved documents</p><p className="mt-2 text-2xl font-semibold">{docsResult.count ?? 0}</p></div>
        <div className="metric-card"><p className="text-xs text-[#829087]">Open reminders</p><p className="mt-2 text-2xl font-semibold">{reminders.filter(r=>!r.completed_at).length}</p></div>
        <div className="metric-card"><p className="text-xs text-[#829087]">This month’s expenses</p><p className="mt-2 text-2xl font-semibold">{currency(monthExpenses)}</p></div>
      </section>
      <section className="grid gap-5 lg:grid-cols-2">
        <div className="glass rounded-[24px] p-5">
          <h2 className="text-sm font-semibold">Add a reminder</h2><p className="mb-4 mt-1 text-xs text-[#829087]">Saved to your account and protected by database policies.</p>
          <form action={createReminder} className="grid gap-3">
            <input name="title" required maxLength={180} placeholder="What should you remember?" className="rounded-xl border border-[#e2e9e2] bg-white/80 px-3 py-3 text-sm outline-none focus:border-[#83ad91]"/>
            <input name="dueAt" type="datetime-local" required className="rounded-xl border border-[#e2e9e2] bg-white/80 px-3 py-3 text-sm outline-none focus:border-[#83ad91]"/>
            <div className="grid grid-cols-2 gap-3"><select name="category" className="rounded-xl border border-[#e2e9e2] bg-white px-3 py-3 text-xs"><option>Personal</option><option>Home</option><option>Finance</option><option>Health</option><option>Work</option></select><select name="priority" className="rounded-xl border border-[#e2e9e2] bg-white px-3 py-3 text-xs"><option value="normal">Normal priority</option><option value="high">High priority</option><option value="low">Low priority</option></select></div>
            <button className="rounded-xl bg-[#234c3d] px-4 py-3 text-xs font-semibold text-white">Save reminder</button>
          </form>
          <div className="mt-6 border-t border-[#e8ede8] pt-4"><h3 className="mb-3 text-xs font-semibold">Your reminders</h3>
            {reminders.length===0?<p className="text-xs text-[#9aa69e]">No reminders yet. Add one above.</p>:<div className="space-y-2">{reminders.map(item=><div key={item.id} className="flex items-center gap-3 rounded-xl bg-[#f7f9f6] px-3 py-3"><div className="min-w-0 flex-1"><p className={`text-xs font-semibold ${item.completed_at?"text-[#9aa69e] line-through":""}`}>{item.title}</p><p className="mt-1 text-[10px] text-[#9aa69e]">{new Date(item.due_at).toLocaleString("en-IN",{dateStyle:"medium",timeStyle:"short"})} · {item.category}</p></div>{!item.completed_at&&<form action={completeReminder}><input type="hidden" name="id" value={item.id}/><button className="rounded-full bg-[#e5f1e9] px-3 py-2 text-[10px] font-semibold text-[#315b46]">Done</button></form>}</div>)}</div>}
          </div>
        </div>
        <div className="glass rounded-[24px] p-5">
          <h2 className="text-sm font-semibold">Track an expense</h2><p className="mb-4 mt-1 text-xs text-[#829087]">Record income or spending manually. No bank connection.</p>
          <form action={createExpense} className="grid gap-3">
            <input name="description" required maxLength={180} placeholder="What was it for?" className="rounded-xl border border-[#e2e9e2] bg-white/80 px-3 py-3 text-sm outline-none focus:border-[#83ad91]"/>
            <div className="grid grid-cols-2 gap-3"><input name="amount" type="number" min="0.01" max="999999999" step="0.01" required placeholder="Amount (₹)" className="min-w-0 rounded-xl border border-[#e2e9e2] bg-white/80 px-3 py-3 text-sm outline-none focus:border-[#83ad91]"/><select name="kind" className="rounded-xl border border-[#e2e9e2] bg-white px-3 py-3 text-xs"><option value="expense">Expense</option><option value="income">Income</option></select></div>
            <div className="grid grid-cols-2 gap-3"><input name="occurredOn" type="date" required defaultValue={new Date().toISOString().slice(0,10)} className="min-w-0 rounded-xl border border-[#e2e9e2] bg-white/80 px-3 py-3 text-xs"/><select name="paymentMethod" className="rounded-xl border border-[#e2e9e2] bg-white px-3 py-3 text-xs"><option value="">Payment method</option><option>Cash</option><option>UPI</option><option>Card</option><option>Bank transfer</option><option>Other</option></select></div>
            <button className="rounded-xl bg-[#234c3d] px-4 py-3 text-xs font-semibold text-white">Save transaction</button>
          </form>
          <div className="mt-6 border-t border-[#e8ede8] pt-4"><h3 className="mb-3 text-xs font-semibold">Recent transactions</h3>
            {expenses.length===0?<p className="text-xs text-[#9aa69e]">No transactions yet. Add one above.</p>:<div className="space-y-2">{expenses.map(item=><div key={item.id} className="flex items-center justify-between rounded-xl bg-[#f7f9f6] px-3 py-3"><div><p className="text-xs font-semibold">{item.description}</p><p className="mt-1 text-[10px] text-[#9aa69e]">{new Date(item.occurred_on+"T00:00:00").toLocaleDateString("en-IN",{dateStyle:"medium"})}</p></div><p className={`text-xs font-semibold ${item.kind==="income"?"text-[#477455]":"text-[#536258]"}`}>{item.kind==="income"?"+":"−"}{currency(Number(item.amount))}</p></div>)}</div>}
          </div>
        </div>
      </section>
      <p className="mt-6 text-center text-[10px] text-[#a1aaa4]">LifeVault · Data is loaded from your signed-in Supabase account.</p>
    </div>
  </main>;
}