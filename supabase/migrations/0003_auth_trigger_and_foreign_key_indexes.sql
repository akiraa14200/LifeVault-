-- Auth signup trigger must be able to create owner rows regardless of the auth service role.
-- Keep the function tightly scoped and prevent direct invocation by API roles.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data->>'full_name', new.email));
  insert into public.user_preferences (user_id) values (new.id);
  insert into public.user_plans (user_id) values (new.id);
  return new;
end;
$$;

revoke all on function public.handle_new_user() from public, anon, authenticated;

-- Cover foreign keys flagged by Supabase's performance advisor.
create index if not exists appliances_user_id_idx on public.appliances(user_id);
create index if not exists budgets_category_id_idx on public.budgets(category_id);
create index if not exists document_folders_user_id_idx on public.document_folders(user_id);
create index if not exists documents_folder_id_idx on public.documents(folder_id);
create index if not exists expenses_category_id_idx on public.expenses(category_id);
create index if not exists maintenance_records_appliance_id_idx on public.maintenance_records(appliance_id);
create index if not exists maintenance_records_user_id_idx on public.maintenance_records(user_id);
create index if not exists notifications_user_id_idx on public.notifications(user_id);
create index if not exists payment_records_user_id_idx on public.payment_records(user_id);
