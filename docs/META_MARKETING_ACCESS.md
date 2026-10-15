# Meta Marketing API — ops verification & Standard Access

> Companion to ADR 0008. Use this checklist before re-requesting Marketing API
> **Standard Access**. Do not generate fake Ads API traffic.

## 1. Server configuration (Railway)

Boot logs a non-sensitive summary: `[meta-config] accessToken=set|missing …`
(values are never logged). Ads endpoints return **503** if the access token is
missing; the process still starts so DB/health checks work.

| Variable | Required? | Purpose |
|---|---|---|
| `META_ACCESS_TOKEN` | **Yes** for Ads/Campaigns/insights/lead fetch | Business **system user** token with ads + leads permissions |
| `META_APP_SECRET` | **Yes** for webhook POST | `X-Hub-Signature-256` verification |
| `META_WEBHOOK_VERIFY_TOKEN` | **Yes** for webhook GET | Hub challenge for `/api/webhook/meta` |
| `META_APP_ID` | Optional | Meta app id for ops/docs; **not used by Graph client today** |
| `META_GRAPH_VERSION` | Optional (default `v22.0`) | Graph API version |
| `META_AD_ACCOUNT_IDS` | Optional | Allowlist (`act_…`, comma-separated); empty = discover via token |
| `META_GRAPH_BASE_URL` | Optional | Override Graph host (tests) |
| `META_CONNECT_TIMEOUT` / `META_READ_TIMEOUT` / `META_MAX_RETRIES` | Optional | HTTP client tuning |

Never put Meta secrets on Vercel. Frontend only needs `API_PROXY_TARGET`.

## 2. Functional verification (real business use)

Sign in as MANAGER / ADMIN / CEO on the CRM:

1. **Ad Channels** → **Run Meta diagnostic** (or `GET /api/marketing/meta/diagnostics`).
   Expect `status=OK` and `accessToken=set`. No secrets are returned.
2. **Ad Channels** → authorized accounts load.
3. **Campaigns** → list campaigns; change insights preset; refresh.
4. Confirm `GET /api/marketing/meta/usage?days=15` shows successful
   `diagnostics.*` / `adaccounts.list` / `campaigns.list` / `insights.list` rows.
5. (Optional) Submit a test Lead Ad → webhook creates a CRM lead; duplicate
   delivery does not create a second lead.

Diagnostic statuses: `CONFIG_MISSING`, `AUTH_FAILURE`, `PERMISSION_FAILURE`,
`ACCOUNT_INACCESSIBLE`, `RATE_LIMITED`, `UNAVAILABLE`, `OK`.

## 3. Webhook (only after public HTTPS)

Callback: `https://<railway-domain>/api/webhook/meta`

1. Meta App → Webhooks → verify with `META_WEBHOOK_VERIFY_TOKEN`.
2. Subscribe object `page`, field `leadgen`.
3. Subscribe the Facebook Page that owns the forms.
4. Do **not** claim subscription is active until Meta shows the Page subscribed.

## 4. Usage evidence hygiene

Table `meta_api_call_log` stores only:

- timestamp, operation, account id, HTTP status, success, error code, graph version, correlation id

It must **never** store access tokens, app secrets, or lead field values.
CRM UI exposes a sanitized summary at `/api/marketing/meta/usage`.

## 5. Before re-applying for Standard Access

1. Confirm Meta still lists Development access and required permissions for your app.
2. Confirm ≥15 days of **genuine** Ads API calls from this integrated app
   (CRM UI / authorized sync — not scripts whose only purpose is volume).
3. Privacy policy URL, data deletion instructions, and reviewer notes are
   accurate for Trip On Himalaya CRM (not the public marketing site alone).
4. Reviewer path: login → Ad Channels → Campaigns → (optional) Lead Ads form.
5. Re-read Meta’s current eligibility criteria; do not assume approval after
   calendar time alone.

## 6. What we will not do

- Fake usage loops
- Hardcoding personal explorer tokens
- Calling Graph from the browser with privileged tokens
- Claiming webhooks are live before Railway HTTPS + Page subscription
