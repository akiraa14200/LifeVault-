"use client";

import { useMemo, useState } from "react";
import {
  LayoutDashboard, FolderLock, BellRing, Wallet, Repeat2, House, CalendarDays,
  Settings2, Search, Plus, ChevronRight, ArrowUpRight, FileText, ShieldCheck,
  Clock3, CircleCheck, MoreHorizontal, Menu, X, Sparkles, ReceiptText, Wrench,
  CalendarCheck, CloudUpload, Command, Sun, CreditCard, TrendingUp
} from "lucide-react";

type Section = "Overview" | "Documents" | "Reminders" | "Expenses" | "Subscriptions" | "Home care" | "Calendar" | "Settings";
const navigation: {label:Section; icon:typeof LayoutDashboard}[] = [
  {label:"Overview",icon:LayoutDashboard},{label:"Documents",icon:FolderLock},{label:"Reminders",icon:BellRing},
  {label:"Expenses",icon:Wallet},{label:"Subscriptions",icon:Repeat2},{label:"Home care",icon:House},
  {label:"Calendar",icon:CalendarDays},{label:"Settings",icon:Settings2}
];
const tasks = [
  {title:"Electricity bill",detail:"Due tomorrow · Home",tag:"₹1,240",tone:"amber"},
  {title:"Car insurance renewal",detail:"Due in 4 days · Documents",tag:"4 days",tone:"rose"},
  {title:"Water purifier service",detail:"Saturday · Home care",tag:"Sat, 8",tone:"blue"}
];
const docs = [
  {name:"Aadhaar card",meta:"Identity · Updated 12 Aug",icon:ShieldCheck,color:"bg-emerald-50 text-emerald-700"},
  {name:"Apartment agreement",meta:"Home · Updated 02 Aug",icon:House,color:"bg-sky-50 text-sky-700"},
  {name:"Health insurance",meta:"Insurance · Updated 28 Jul",icon:FileText,color:"bg-violet-50 text-violet-700"}
];
const money = (n:number) => new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR",maximumFractionDigits:0}).format(n);

export default function HomePage() {
  const [active,setActive] = useState<Section>("Overview");
  const [mobileOpen,setMobileOpen] = useState(false);
  const [query,setQuery] = useState("");
  const [showQuick,setShowQuick] = useState(false);
  const [completed,setCompleted] = useState<string[]>([]);
  const [notice,setNotice] = useState("");
  const filtered = useMemo(()=>tasks.filter(t=>t.title.toLowerCase().includes(query.toLowerCase())),[query]);
  const complete = (title:string) => setCompleted(v=>v.includes(title)?v.filter(x=>x!==title):[...v,title]);
  const greeting = new Intl.DateTimeFormat("en-IN",{weekday:"long",day:"numeric",month:"long"}).format(new Date());
  const comingSoon = active !== "Overview";
  const iconMap:Record<string,typeof ReceiptText> = {Documents:FolderLock,Reminders:BellRing,Expenses:Wallet,Subscriptions:Repeat2,"Home care":Wrench,Calendar:CalendarCheck,Settings:Settings2};
  return <div className="min-h-screen">
    <aside className="sidebar fixed inset-y-0 left-0 z-30 flex w-[248px] flex-col px-5 py-6">
      <div className="mb-10 flex items-center gap-3 px-2">
        <div className="grid h-10 w-10 place-items-center rounded-2xl bg-[#234c3d] text-[#d8f2e4] shadow-lg shadow-emerald-950/10"><Command size={21}/></div>
        <div><div className="text-[17px] font-bold tracking-[-.7px]">lifevault<span className="text-[#7db89b]">.</span></div><div className="text-[10px] tracking-[1.7px] text-[#9aa69e]">LIFE, IN ORDER</div></div>
      </div>
      <div className="mb-3 px-3 text-[10px] font-bold uppercase tracking-[1.7px] text-[#a3aea7]">Workspace</div>
      <nav className="space-y-1">{navigation.map(({label,icon:Icon})=><button key={label} onClick={()=>{setActive(label);setMobileOpen(false)}} className={`nav-item ${active===label?"active":""}`}><Icon size={17} strokeWidth={1.8}/><span>{label}</span>{label==="Reminders"&&<span className="ml-auto rounded-full bg-[#f5e8ce] px-2 py-0.5 text-[10px] text-[#98733b]">3</span>}</button>)}</nav>
      <div className="mt-auto rounded-2xl bg-[#eaf3ec] p-4">
        <div className="mb-2 flex items-center gap-2 text-[#315b46]"><Sparkles size={15}/><span className="text-xs font-semibold">A little more peace</span></div>
        <p className="mb-3 text-[11px] leading-relaxed text-[#708477]">Keep life’s important details together, safely in one place.</p>
        <button onClick={()=>setNotice("Your LifeVault setup checklist will appear here once your account is connected.")} className="text-[11px] font-semibold text-[#315b46]">Explore your vault <ArrowUpRight className="ml-1 inline" size={12}/></button>
      </div>
      <div className="mt-5 flex items-center gap-3 border-t border-[#e7ece7] px-2 pt-5">
        <div className="grid h-9 w-9 place-items-center rounded-full bg-[#dcebe1] text-xs font-bold text-[#315b46]">LV</div>
        <div className="min-w-0 flex-1"><div className="truncate text-xs font-semibold">Your account</div><div className="text-[10px] text-[#9aa69e]">Connect to get started</div></div><MoreHorizontal size={17} className="text-[#9aa69e]"/>
      </div>
    </aside>
    <main className="main-shell ml-0 px-5 pb-10 pt-6 md:ml-[248px] md:px-10 md:pt-9">
      <header className="mb-9 flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <button className="rounded-xl bg-white p-2 md:hidden" onClick={()=>setMobileOpen(!mobileOpen)} aria-label="Toggle navigation">{mobileOpen?<X size={18}/>:<Menu size={18}/>}</button>
          <div><div className="text-[11px] font-medium uppercase tracking-[1.6px] text-[#9aa69e]">{greeting}</div><h1 className="mt-1 text-[23px] font-semibold tracking-[-1px] md:text-[27px]">{active==="Overview"?"A clearer day starts here.":active}</h1></div>
        </div>
        <div className="flex items-center gap-2">
          <label className="hidden items-center gap-2 rounded-full border border-[#e8ede8] bg-white/70 px-4 py-2.5 sm:flex"><Search size={15} className="text-[#9aa69e]"/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search your vault..." className="w-36 bg-transparent text-xs outline-none placeholder:text-[#a6b0a9]"/></label>
          <button onClick={()=>setNotice("Notifications will be available after you connect your LifeVault account.")} className="relative rounded-full border border-[#e8ede8] bg-white/70 p-2.5 text-[#607067]" aria-label="Notifications"><BellRing size={17}/><span className="absolute right-2 top-2 h-1.5 w-1.5 rounded-full bg-[#d99a59]"/></button>
          <button onClick={()=>setShowQuick(true)} className="flex items-center gap-2 rounded-full bg-[#234c3d] px-4 py-2.5 text-xs font-semibold text-white shadow-lg shadow-emerald-950/10"><Plus size={15}/> <span className="hidden sm:inline">Add new</span></button>
        </div>
      </header>
      {notice&&<div role="status" className="mb-5 flex items-center justify-between rounded-xl border border-[#dcebe1] bg-[#edf6ef] px-4 py-3 text-xs text-[#315b46]">{notice}<button onClick={()=>setNotice("")} aria-label="Dismiss"><X size={15}/></button></div>}
      {comingSoon?<section className="glass soft-grid flex min-h-[440px] flex-col items-center justify-center rounded-[28px] px-7 text-center">
        {(()=>{const Icon=iconMap[active]||Sparkles;return <div className="mb-5 grid h-16 w-16 place-items-center rounded-3xl bg-[#e6f1e9] text-[#315b46]"><Icon size={28}/></div>})()}
        <div className="mb-2 text-xl font-semibold tracking-tight">{active} will live here.</div><p className="max-w-sm text-sm leading-6 text-[#849188]">This space is being connected to your private LifeVault database. Your information will appear here once setup is complete.</p>
        <button onClick={()=>setActive("Overview")} className="mt-6 rounded-full bg-[#234c3d] px-5 py-2.5 text-xs font-semibold text-white">Back to overview</button>
      </section>:<>
        <section className="glass relative mb-6 overflow-hidden rounded-[26px] p-6 md:p-8">
          <div className="absolute -right-12 -top-24 h-64 w-64 rounded-full bg-[#d9eee0]/70 blur-3xl"/>
          <div className="absolute right-16 top-10 h-28 w-28 rounded-full border border-[#c8e2d1]/70"/>
          <div className="relative max-w-xl"><div className="mb-3 inline-flex items-center gap-2 rounded-full bg-[#e8f3eb] px-3 py-1.5 text-[10px] font-semibold tracking-wide text-[#527b62]"><Sun size={12}/> YOUR PERSONAL SPACE</div>
            <h2 className="mb-2 text-[25px] font-semibold leading-tight tracking-[-1.2px] md:text-[32px]">Less to remember.<br/><span className="text-[#7ca78c]">More room to live.</span></h2>
            <p className="max-w-md text-[13px] leading-6 text-[#7c8982]">Your documents, plans and everyday essentials — thoughtfully gathered in one calm place.</p>
            <button onClick={()=>setShowQuick(true)} className="mt-5 inline-flex items-center gap-2 rounded-full bg-[#234c3d] px-5 py-3 text-xs font-semibold text-white">Add your first item <ChevronRight size={14}/></button>
          </div>
          <div className="relative mt-6 flex items-center gap-2 text-[10px] text-[#8c9b91] md:absolute md:bottom-8 md:right-8 md:mt-0"><ShieldCheck size={14} className="text-[#6f9d7f}"/> Private by design · Your data stays yours</div>
        </section>
        <section className="mb-7 grid grid-cols-2 gap-3 lg:grid-cols-4">
          {[{label:"Documents saved",value:"—",sub:"Your digital filing cabinet",icon:FolderLock},{label:"Upcoming reminders",value:"3",sub:"Items to keep in mind",icon:BellRing},{label:"Monthly expenses",value:"—",sub:"Track where it goes",icon:Wallet},{label:"Active subscriptions",value:"—",sub:"Stay ahead of renewals",icon:Repeat2}].map(({label,value,sub,icon:Icon},i)=><div className="metric-card" key={label}><div className="mb-4 flex items-center justify-between"><span className="text-[11px] text-[#89968e]">{label}</span><span className="grid h-8 w-8 place-items-center rounded-xl bg-[#edf4ef] text-[#63816e]"><Icon size={15}/></span></div><div className="text-[26px] font-semibold tracking-[-1px]">{value}</div><div className="mt-1 text-[10px] text-[#a1aaa4]">{sub}</div></div>)}
        </section>
        <section className="grid gap-5 xl:grid-cols-[1.25fr_.75fr]">
          <div className="glass rounded-[24px] p-5 md:p-6">
            <div className="mb-5 flex items-center justify-between"><div><h3 className="text-sm font-semibold">Coming up</h3><p className="mt-1 text-[11px] text-[#9aa69e]">A gentle nudge for what’s next</p></div><button onClick={()=>setActive("Reminders")} className="text-[11px] font-semibold text-[#63816e]">View all <ChevronRight className="ml-1 inline" size={13}/></button></div>
            <div className="space-y-2">{filtered.map((task,i)=><div key={task.title} className="flex items-center gap-3 rounded-2xl bg-[#f7f9f6] px-3 py-3"><button onClick={()=>complete(task.title)} aria-label={completed.includes(task.title)?"Mark incomplete":"Mark complete"} className={`grid h-8 w-8 shrink-0 place-items-center rounded-xl ${completed.includes(task.title)?"bg-[#dceee1] text-[#477455]":"bg-white text-[#a0aca3]"}`}>{completed.includes(task.title)?<CircleCheck size={17}/>:<Clock3 size={16}/>}</button><div className="min-w-0 flex-1"><div className={`text-xs font-semibold ${completed.includes(task.title)?"text-[#9ba69e] line-through":"text-[#34443a]"}`}>{task.title}</div><div className="mt-1 text-[10px] text-[#9aa69e]">{task.detail}</div></div><span className="rounded-full bg-white px-2.5 py-1 text-[10px] font-semibold text-[#7e8c82]">{task.tag}</span></div>)}</div>
            {filtered.length===0&&<p className="py-8 text-center text-xs text-[#9aa69e]">No reminders match your search.</p>}
          </div>
          <div className="glass rounded-[24px] p-5 md:p-6">
            <div className="mb-5 flex items-center justify-between"><div><h3 className="text-sm font-semibold">Recently added</h3><p className="mt-1 text-[11px] text-[#9aa69e]">Your important things, together</p></div><button onClick={()=>setActive("Documents")} className="text-[#8c9a90]"><MoreHorizontal size={18}/></button></div>
            <div className="space-y-4">{docs.map(({name,meta,icon:Icon,color})=><div className="flex items-center gap-3" key={name}><div className={`grid h-10 w-10 place-items-center rounded-xl ${color}`}><Icon size={17}/></div><div className="min-w-0 flex-1"><div className="truncate text-xs font-semibold">{name}</div><div className="mt-1 truncate text-[10px] text-[#9aa69e]">{meta}</div></div><ChevronRight size={15} className="text-[#b2bcb4]"/></div>)}</div>
            <button onClick={()=>setActive("Documents")} className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl border border-dashed border-[#d9e3da] py-3 text-[11px] font-semibold text-[#718779]"><CloudUpload size={15}/> Organise your documents</button>
          </div>
        </section>
        <section className="mt-5 grid gap-5 md:grid-cols-2">
          <div className="rounded-[24px] bg-[#e8f1e9] p-5"><div className="mb-3 flex items-center justify-between"><div className="text-xs font-semibold text-[#315b46]">Your month at a glance</div><TrendingUp size={16} className="text-[#789c81]"/></div><div className="text-2xl font-semibold tracking-tight text-[#315b46]">A fresh start.</div><p className="mt-1 text-[11px] leading-5 text-[#789080]">Add a few details to see your spending patterns and monthly progress here.</p><button onClick={()=>setActive("Expenses")} className="mt-4 text-[11px] font-semibold text-[#315b46]">Set up expenses <ArrowUpRight className="ml-1 inline" size={13}/></button></div>
          <div className="rounded-[24px] bg-[#f1eee7] p-5"><div className="mb-3 flex items-center justify-between"><div className="text-xs font-semibold text-[#796b52]">A home that runs smoothly</div><Wrench size={16} className="text-[#a18c69]"/></div><div className="text-2xl font-semibold tracking-tight text-[#665a45]">Never miss a service.</div><p className="mt-1 text-[11px] leading-5 text-[#9b8e77]">Keep appliance warranties, service dates and receipts close at hand.</p><button onClick={()=>setActive("Home care")} className="mt-4 text-[11px] font-semibold text-[#796b52]">Set up home care <ArrowUpRight className="ml-1 inline" size={13}/></button></div>
        </section>
      </>}
    </main>
    <nav className="mobile-nav fixed bottom-0 left-0 right-0 z-20 hidden justify-around border-t border-[#e7ece7] bg-white/90 px-2 py-3 backdrop-blur-xl">{navigation.slice(0,5).map(({label,icon:Icon})=><button key={label} onClick={()=>setActive(label)} className={`flex flex-col items-center gap-1 px-2 text-[9px] ${active===label?"text-[#315b46]":"text-[#9aa69e]"}`}><Icon size={18}/>{label==="Overview"?"Home":label}</button>)}</nav>
    {mobileOpen&&<div className="fixed inset-0 z-40 bg-black/20 md:hidden" onClick={()=>setMobileOpen(false)}><div className="sidebar h-full w-[270px] p-5" onClick={e=>e.stopPropagation()}>{navigation.map(({label,icon:Icon})=><button key={label} onClick={()=>{setActive(label);setMobileOpen(false)}} className={`nav-item ${active===label?"active":""}`}><Icon size={17}/>{label}</button>)}</div></div>}
    {showQuick&&<div className="fixed inset-0 z-50 grid place-items-center bg-[#17211d]/30 p-4 backdrop-blur-sm" onClick={()=>setShowQuick(false)}><div className="glass w-full max-w-md rounded-[26px] p-6" onClick={e=>e.stopPropagation()}><div className="mb-5 flex items-center justify-between"><div><h3 className="text-lg font-semibold">What would you like to add?</h3><p className="mt-1 text-xs text-[#89968e]">Choose a space to get started.</p></div><button onClick={()=>setShowQuick(false)} aria-label="Close" className="rounded-full bg-white p-2"><X size={16}/></button></div><div className="grid grid-cols-2 gap-3">{navigation.slice(1,7).map(({label,icon:Icon})=><button key={label} onClick={()=>{setActive(label);setShowQuick(false)}} className="flex items-center gap-3 rounded-2xl border border-[#e8ede8] bg-white/70 p-4 text-left text-xs font-semibold hover:bg-[#edf4ef]"><Icon size={18} className="text-[#668a73]"/>{label}</button>)}</div></div></div>}
    <div className="fixed bottom-3 right-4 z-10 hidden items-center gap-1 text-[9px] text-[#a6b0a9] md:flex"><span className="rounded border border-[#dce4dc] bg-white px-1.5 py-1">⌘</span> LifeVault preview</div>
  </div>;
}