# Database

The initial migration defines profiles, preferences, document folders/files, reminders, expense categories/transactions/budgets, subscriptions, appliances, maintenance records, calendar events, notifications, plans and payment records.

All user-owned tables have RLS enabled and owner-scoped policies. Document objects live in a private bucket and use the user's UUID as the first path segment. Access should be via short-lived signed URLs after server-side authorization.

The first migration is a foundation, not a complete notification scheduler or payment entitlement implementation. Validate it in a disposable Supabase project before production.