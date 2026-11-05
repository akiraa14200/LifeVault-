# Deployment

1. Import this repository into Vercel.
2. Set the environment variables from `.env.example` in Vercel Project Settings.
3. Apply the Supabase migration and verify RLS/storage policies.
4. Set `NEXT_PUBLIC_APP_URL` to the production URL and configure matching Supabase Auth redirect URLs.
5. Deploy only after auth, CRUD, upload, scheduled notifications and security tests are complete.

No production deployment has been performed by this repository bootstrap.