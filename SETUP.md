# Setup

## Requirements
Node.js 20.9 or newer and an npm-compatible package manager.

## Supabase
Create a Supabase project. In Project Settings → API, copy the project URL and anon/publishable key into `.env.local`. Apply the SQL migration in `supabase/migrations` using the SQL Editor. Enable the auth providers you intend to offer in Authentication → Providers.

The migration creates a private `lifevault-documents` bucket with a 10 MiB cap and PDF/JPEG/PNG/WebP MIME allowlist. It also enables owner-scoped RLS.

## Run locally
```bash
npm install
npm run dev
```

The dashboard is currently a frontend preview and does not yet write records to Supabase. Do not use it as a production vault until auth and CRUD are completed.