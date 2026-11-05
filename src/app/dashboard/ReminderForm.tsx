"use client";

import { useRef } from "react";
import { createReminder } from "@/app/actions/data";

export function ReminderForm() {
  const offset = useRef<HTMLInputElement>(null);
  return <form action={createReminder} className="grid gap-3" onSubmit={() => {
    if (offset.current) offset.current.value = String(new Date().getTimezoneOffset());
  }}>
    <input type="hidden" name="timezoneOffset" ref={offset} defaultValue="0"/>
    <input name="title" required maxLength={180} placeholder="What should you remember?" className="rounded-xl border border-[#e2e9e2] bg-white/80 px-3 py-3 text-sm outline-none focus:border-[#83ad91]"/>
    <input name="dueAt" type="datetime-local" required className="rounded-xl border border-[#e2e9e2] bg-white/80 px-3 py-3 text-sm outline-none focus:border-[#83ad91]"/>
    <div className="grid grid-cols-2 gap-3"><select name="category" className="rounded-xl border border-[#e2e9e2] bg-white px-3 py-3 text-xs"><option>Personal</option><option>Home</option><option>Finance</option><option>Health</option><option>Work</option></select><select name="priority" className="rounded-xl border border-[#e2e9e2] bg-white px-3 py-3 text-xs"><option value="normal">Normal priority</option><option value="high">High priority</option><option value="low">Low priority</option></select></div>
    <button className="rounded-xl bg-[#234c3d] px-4 py-3 text-xs font-semibold text-white">Save reminder</button>
  </form>;
}