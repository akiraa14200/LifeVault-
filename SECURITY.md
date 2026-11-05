# Security

- Never expose service-role or payment secrets to browser code.
- RLS is enabled on every public user-data table.
- Storage is private and constrained by owner UUID, MIME allowlist and size limit.
- Use server-side auth checks for protected routes and privileged actions.
- Generate short-lived signed document URLs only after checking ownership.
- Validate all inputs server-side; do not trust client-provided user IDs or plan names.
- Payment entitlements must only change after verified provider signatures/webhooks.
- Account deletion must remove owned records and stored objects under an explicit, confirmed workflow.

This initial foundation does not yet implement all application-layer checks, rate limits, scheduled jobs, or payment verification.