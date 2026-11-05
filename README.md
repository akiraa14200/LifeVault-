# LifeVault

LifeVault is a personal life organiser for documents, reminders, expenses, subscriptions and home maintenance.

## Stack
- Next.js App Router + TypeScript
- Tailwind CSS + Lucide
- Supabase Auth, Postgres and private Storage
- Vercel deployment target

## Current status
The first foundation commit includes a responsive dashboard preview, app shell, Supabase browser/server client helpers, environment template and an initial owner-scoped database/storage migration. The dashboard currently uses clearly labelled sample/empty-state data; CRUD, authentication screens, scheduled notifications and payments are not yet connected.

## Local setup
1. Install Node.js 20.9+.
2. Run `npm install`.
3. Copy `.env.example` to `.env.local` and add your Supabase project URL and anon/publishable key.
4. Apply `supabase/migrations/0001_initial_schema.sql` in the Supabase SQL Editor.
5. Run `npm run dev`.

Never put the Supabase service-role key in a `NEXT_PUBLIC_` variable or commit real secrets.

## Roadmap
1. Auth, onboarding and protected routes.
2. Real document vault with signed URLs and upload validation.
3. Reminders and server-scheduled notification delivery.
4. Expenses, budgets, subscriptions and home-care CRUD.
5. Unified calendar, settings, export/delete and plan entitlements.
6. Automated tests, accessibility and production deployment.

See [SETUP.md](./SETUP.md), [DATABASE.md](./DATABASE.md), [SECURITY.md](./SECURITY.md), [FEATURES.md](./FEATURES.md), and [DEPLOYMENT.md](./DEPLOYMENT.md).