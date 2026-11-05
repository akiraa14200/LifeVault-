-- LifeVault initial schema. Apply in Supabase SQL Editor or with Supabase CLI.
create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  avatar_url text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create table if not exists public.user_preferences (
  user_id uuid primary key references auth.users(id) on delete cascade,
  theme text not null default 'system' check (theme in ('light','dark','system')),
  timezone text not null default 'Asia/Kolkata',
  currency text not null default 'INR',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create table if not exists public.document_folders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (char_length(name) between 1 and 80),
  created_at timestamptz not null default now()
);
create table if not exists public.documents (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  folder_id uuid references public.document_folders(id) on delete set null,
  name text not null check (char_length(name) between 1 and 180),
  category text not null default 'Other',
  storage_path text not null,
  mime_type text not null check (mime_type in ('application/pdf','image/jpeg','image/png','image/webp')),
  size_bytes bigint not null check (size_bytes > 0 and size_bytes <= 10485760),
  expires_on date,
  is_favorite boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create table if not exists public.reminders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null check (char_length(title) between 1 and 180),
  notes text,
  due_at timestamptz not null,
  recurrence text not null default 'none' check (recurrence in ('none','daily','weekly','monthly','yearly')),
  priority text not null default 'normal' check (priority in ('low','normal','high')),
  category text not null default 'Personal',
  completed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create table if not exists public.expense_categories (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  kind text not null default 'expense' check (kind in ('income','expense')),
  created_at timestamptz not null default now(),
  unique(user_id,name,kind)
);
create table if not exists public.expenses (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  category_id uuid references public.expense_categories(id) on delete set null,
  kind text not null check (kind in ('income','expense')),
  amount numeric(12,2) not null check (amount > 0),
  description text not null check (char_length(description) between 1 and 180),
  payment_method text,
  occurred_on date not null default current_date,
  notes text,
  created_at timestamptz not null default now()
);
create table if not exists public.budgets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  category_id uuid references public.expense_categories(id) on delete set null,
  month date not null,
  amount numeric(12,2) not null check (amount > 0),
  created_at timestamptz not null default now(),
  unique(user_id,category_id,month)
);
create table if not exists public.subscriptions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (char_length(name) between 1 and 120),
  price numeric(12,2) not null check (price >= 0),
  currency text not null default 'INR',
  frequency text not null check (frequency in ('weekly','monthly','quarterly','yearly')),
  next_payment_on date not null,
  payment_label text,
  reminder_days integer not null default 3 check (reminder_days between 0 and 60),
  status text not null default 'active' check (status in ('active','paused','cancelled')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create table if not exists public.appliances (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  brand text,
  model text,
  purchased_on date,
  warranty_until date,
  notes text,
  created_at timestamptz not null default now()
);
create table if not exists public.maintenance_records (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  appliance_id uuid references public.appliances(id) on delete cascade,
  title text not null,
  service_on date not null,
  next_service_on date,
  cost numeric(12,2) check (cost is null or cost >= 0),
  notes text,
  created_at timestamptz not null default now()
);
create table if not exists public.calendar_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  starts_at timestamptz not null,
  ends_at timestamptz,
  all_day boolean not null default false,
  event_type text not null default 'custom' check (event_type in ('custom','bill','reminder','subscription','document_expiry','maintenance')),
  linked_id uuid,
  notes text,
  created_at timestamptz not null default now()
);
create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  body text,
  scheduled_for timestamptz,
  sent_at timestamptz,
  read_at timestamptz,
  created_at timestamptz not null default now()
);
create table if not exists public.user_plans (
  user_id uuid primary key references auth.users(id) on delete cascade,
  plan text not null default 'free' check (plan in ('free','premium')),
  status text not null default 'active' check (status in ('active','pending','cancelled','expired')),
  valid_until timestamptz,
  updated_at timestamptz not null default now()
);
create table if not exists public.payment_records (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  provider text not null,
  provider_order_id text unique,
  provider_payment_id text unique,
  amount_paise bigint not null check (amount_paise > 0),
  currency text not null default 'INR',
  status text not null default 'created' check (status in ('created','authorized','captured','failed','refunded')),
  created_at timestamptz not null default now()
);

-- Every user-owned table is protected by owner-scoped RLS.
do $$
declare t text;
begin
  foreach t in array array['profiles','user_preferences','document_folders','documents','reminders','expense_categories','expenses','budgets','subscriptions','appliances','maintenance_records','calendar_events','notifications','user_plans','payment_records']
  loop
    execute format('alter table public.%I enable row level security',t);
    if t = 'profiles' then
      execute 'create policy "profile owner access" on public.profiles for all to authenticated using ((select auth.uid()) = id) with check ((select auth.uid()) = id)';
    elsif t = 'user_preferences' then
      execute 'create policy "preferences owner access" on public.user_preferences for all to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id)';
    else
      execute format('create policy "%s owner access" on public.%I for all to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id)',t,t);
    end if;
  end loop;
end $$;

create index if not exists documents_user_created_idx on public.documents(user_id,created_at desc);
create index if not exists reminders_user_due_idx on public.reminders(user_id,due_at);
create index if not exists expenses_user_date_idx on public.expenses(user_id,occurred_on desc);
create index if not exists subscriptions_user_next_idx on public.subscriptions(user_id,next_payment_on);
create index if not exists calendar_user_start_idx on public.calendar_events(user_id,starts_at);

-- Private document bucket; paths must begin with the authenticated user's UUID.
insert into storage.buckets (id,name,public,file_size_limit,allowed_mime_types)
values ('lifevault-documents','lifevault-documents',false,10485760,array['application/pdf','image/jpeg','image/png','image/webp'])
on conflict (id) do update set public=false,file_size_limit=10485760,allowed_mime_types=excluded.allowed_mime_types;

create policy "Users read own LifeVault files" on storage.objects for select to authenticated
using (bucket_id='lifevault-documents' and (storage.foldername(name))[1]=(select auth.uid())::text);
create policy "Users upload own LifeVault files" on storage.objects for insert to authenticated
with check (bucket_id='lifevault-documents' and (storage.foldername(name))[1]=(select auth.uid())::text);
create policy "Users update own LifeVault files" on storage.objects for update to authenticated
using (bucket_id='lifevault-documents' and (storage.foldername(name))[1]=(select auth.uid())::text)
with check (bucket_id='lifevault-documents' and (storage.foldername(name))[1]=(select auth.uid())::text);
create policy "Users delete own LifeVault files" on storage.objects for delete to authenticated
using (bucket_id='lifevault-documents' and (storage.foldername(name))[1]=(select auth.uid())::text);

-- Profiles are created on signup; no user-editable metadata is used for authorization.
create or replace function public.handle_new_user()
returns trigger language plpgsql security invoker set search_path = ''
as $$
begin
  insert into public.profiles (id,display_name) values (new.id, coalesce(new.raw_user_meta_data->>'full_name',new.email));
  insert into public.user_preferences (user_id) values (new.id);
  insert into public.user_plans (user_id) values (new.id);
  return new;
end;
$$;
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
for each row execute procedure public.handle_new_user();