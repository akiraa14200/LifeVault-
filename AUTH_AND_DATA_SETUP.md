# Authentication and connected data setup

## What this milestone adds
- Email/password sign-up and sign-in using Supabase Auth.
- Email confirmation through the PKCE callback route.
- Password reset email and authenticated password update.
- Server-validated sessions with `auth.getUser()` and cookie refresh middleware.
- Protected dashboard route.
- Database-backed reminder creation/completion and expense/income creation/listing.
- Every write derives the user ID from the verified server session; the browser cannot choose an owner ID.

## Supabase configuration
1. Add `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY` to local/Vercel environment variables. A Supabase publishable key can be used as the client key too.
2. Run migrations in order: `0001_initial_schema.sql`, then `0002_authenticated_data_access.sql`.
3. In Supabase Authentication → URL Configuration, set the Site URL to your app URL.
4. Add redirect URLs for `http://localhost:3000/auth/callback` and your production `https://YOUR-DOMAIN/auth/callback`.
5. Set the same production URL in `NEXT_PUBLIC_APP_URL`.

## Test the flow
1. Open `/signup`, create an account and confirm the email.
2. Return through the email link; it should reach `/dashboard`.
3. Add a reminder and mark it complete.
4. Add an expense and confirm it appears after refresh.
5. Sign out; opening `/dashboard` should return to `/login`.
6. Create a second account and verify it cannot see the first account's rows.

## Important
The dashboard at `/dashboard` is the connected milestone. The original visual dashboard at `/` remains the earlier preview for now. Scheduled reminder delivery, document upload/preview, subscriptions, maintenance, calendar, export/deletion and billing are not yet implemented. Do not deploy this as a finished vault until those features and the security tests are completed.