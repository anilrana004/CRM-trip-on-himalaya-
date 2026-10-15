# ADR 0008 — Meta Marketing API integration in the modular monolith

- **Status:** Accepted
- **Date:** 2026-10-15
- **Relates to:** `docs/INTEGRATIONS.md` #10, `docs/PHASE_5_DELTA.md` Module 4,
  `docs/ARCHITECTURE.md` §2 (`marketing/` package)

## Context

Trip On Himalaya CRM must manage authorized Meta ad accounts, retrieve campaign
performance, and ingest Facebook/Instagram Lead Ads into the existing `leads`
pipeline. Manual Graph Explorer checks already proved the business assets work;
the repository has **no** Meta client yet (`marketing/` is a placeholder).

Constraints that shape the design:

1. Frontend is on Vercel; backend on Railway (AWS later). Tokens and app secrets
   must never ship to the browser.
2. Meta rejected Standard Access for insufficient Ads API usage. We need a
   genuine operational integration and an audit trail of successful calls — not
   synthetic traffic loops.
3. Existing patterns already cover HMAC webhooks (`/api/webhook/lead`, Interakt),
   JWT RBAC, Flyway, and package-by-feature modules. Meta must reuse those, not
   invent a second stack.
4. Compromised explorer tokens are burned. Credentials live only in Railway
   (or future AWS secrets).

## Decision

### Placement

All Meta Graph / Marketing API I/O lives in `com.securetravels.crm.marketing`
behind a single `MetaGraphClient` (HTTP) and thin services. Controllers expose
CRM-shaped DTOs only. No SDK dependency in v1 — Spring `RestClient` with
timeouts, pagination, and normalized errors (same spirit as Interakt adapters).

### Auth model (server)

- **Mode A (default for ops):** long-lived **Business system-user** token in
  `META_ACCESS_TOKEN`, plus `META_APP_ID` / `META_APP_SECRET` for webhook
  signature validation.
- **Mode B (later):** OAuth user-token exchange — deferred until system-user
  proves insufficient.
- Optional allowlist `META_AD_ACCOUNT_IDS` (comma-separated `act_…` or numeric
  ids). When set, CRM endpoints refuse any account not on the list (prevents
  IDOR via client-supplied account ids). When empty, accounts returned by
  `me/adaccounts` (or `/<business>/owned_ad_accounts`) are the allowlist for
  that process boot / cache window.

CRM JWT still gates every operator endpoint. Roles: `MANAGER`, `ADMIN`, `CEO`
(matches existing Marketing nav).

### Graph version and base URL

- Configurable: `app.meta.graph-version` ← `META_GRAPH_VERSION` (default
  **`v22.0`** as a stable Marketing API baseline; operators may raise to the
  newest supported version in Meta’s changelog without code changes).
- Base: `https://graph.facebook.com/{version}` via `META_GRAPH_BASE_URL`
  override only for tests.

### Data flow

```
CRM user (JWT)
  → Next.js /api proxy (Vercel)
  → Spring `/api/marketing/meta/**`
  → MetaGraphClient
  → Graph API
  → normalized DTOs (+ optional local sync rows)
  → CRM UI
```

Inbound leads:

```
Meta leadgen webhook
  → public HTTPS Railway `/api/webhook/meta`
  → verify hub.challenge (GET) + X-Hub-Signature-256 (POST)
  → persist event idempotency key
  → fetch lead detail with Page token / system user
  → LeadService.createAutomated (source FACEBOOK_ADS | INSTAGRAM)
  → unique meta_lead_id prevents double insert
```

### Persistence (minimal v1)

Flyway next migration:

- `meta_ad_accounts` — authorized accounts mirrored for allowlisting / UI.
- `meta_api_call_log` — operation, account id, HTTP status, success, correlation
  id, graph version, timestamps. **No tokens, no lead field values.**
- `meta_leadgen_events` — Meta lead id + raw event id for webhook idempotency.
- `leads.meta_lead_id` (nullable unique) — stable external key alongside existing
  mobile duplicate soft-handling.

Campaign/insights responses are **not** fully mirrored in v1 (read-through to
Graph with pagination). Optional later cache if freshness requirements demand it.

### Webhooks

- Path: `/api/webhook/meta` (GET verify, POST events) — public, signature-gated.
- Verify token: `META_WEBHOOK_VERIFY_TOKEN`.
- Signing: app secret HMAC (`META_APP_SECRET`), Meta’s `X-Hub-Signature-256`.
- Acknowledge quickly; process lead fetch in the request transaction first; if
  volume grows, reuse existing messaging INLINE/BROKER patterns (no new broker
  for Meta alone).

### Reliability

- Connect/read timeouts from config.
- Retry only transient 5xx / 429 with bounded backoff; never retry 401/403/190.
- Rate-limit CRM endpoints with existing filter patterns where useful.
- Structured errors mapped to CRM `BadRequest` / `Forbidden` / `ServiceUnavailable`.

### Explicitly out of scope (this ADR)

- Google Ads, Meta Conversions API, full Marketing ROI dashboards.
- Browser-side Graph calls.
- Fake usage generators for Standard Access review.
- Kafka / new microservices.

### Portability

Business logic and Flyway schema stay in the monolith. Railway vs AWS differs
only by env vars, public URL for webhooks, and process manager — not by package
structure.

## Consequences

- `docs/INTEGRATIONS.md` #10 moves from Not Started → In Progress under this ADR.
- Standard Access resubmission is an **ops** step after real CRM-driven Ads API
  calls appear in `meta_api_call_log` and Meta’s app dashboard.
- Operators must configure Railway secrets before production features work;
  missing config fails closed with clear 503 messages, not empty “success”.
