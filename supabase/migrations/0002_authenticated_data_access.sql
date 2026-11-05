-- Explicitly grant authenticated users table privileges; RLS remains the row-level boundary.
grant select, insert, update, delete on
  public.profiles,
  public.user_preferences,
  public.document_folders,
  public.documents,
  public.reminders,
  public.expense_categories,
  public.expenses,
  public.budgets,
  public.subscriptions,
  public.appliances,
  public.maintenance_records,
  public.calendar_events,
  public.notifications,
  public.user_plans,
  public.payment_records
to authenticated;

-- The application never needs anonymous access to private LifeVault records.
revoke all on
  public.profiles,
  public.user_preferences,
  public.document_folders,
  public.documents,
  public.reminders,
  public.expense_categories,
  public.expenses,
  public.budgets,
  public.subscriptions,
  public.appliances,
  public.maintenance_records,
  public.calendar_events,
  public.notifications,
  public.user_plans,
  public.payment_records
from anon;

-- Keep timestamp maintenance consistent for mutable records.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists reminders_set_updated_at on public.reminders;
create trigger reminders_set_updated_at before update on public.reminders
for each row execute function public.set_updated_at();

drop trigger if exists subscriptions_set_updated_at on public.subscriptions;
create trigger subscriptions_set_updated_at before update on public.subscriptions
for each row execute function public.set_updated_at();

drop trigger if exists documents_set_updated_at on public.documents;
create trigger documents_set_updated_at before update on public.documents
for each row execute function public.set_updated_at();

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at before update on public.profiles
for each row execute function public.set_updated_at();

drop trigger if exists preferences_set_updated_at on public.user_preferences;
create trigger preferences_set_updated_at before update on public.user_preferences
for each row execute function public.set_updated_at();