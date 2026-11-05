# Feature status

| Area | Status |
|---|---|
| Responsive LifeVault visual dashboard preview | Implemented at / |
| Email/password sign-up and sign-in | Implemented; requires Supabase config |
| Email confirmation callback (PKCE) | Implemented; requires Auth redirect configuration |
| Password reset and update | Implemented; requires Auth email configuration |
| Protected session middleware | Implemented with server-validated getUser |
| Connected dashboard | Implemented at /dashboard |
| Reminder create/list/complete | Connected to Supabase |
| Expense and income create/list | Connected to Supabase |
| Monthly expense summary | Calculated from all current-month expense rows |
| Owner-scoped database RLS | Initial migrations committed; not yet applied to a project |
| Private document bucket | Migration committed; upload UI/API not yet implemented |
| Document upload/preview CRUD | Planned |
| Server-scheduled reminder delivery | Planned; no browser-only timer workaround |
| Subscription tracker | Planned |
| Home maintenance CRUD | Planned |
| Unified calendar | Planned |
| Export/account deletion | Planned |
| Premium billing | Planned; no payment integration |
| Automated tests and production build verification | Pending |
| Production deployment | Not performed |