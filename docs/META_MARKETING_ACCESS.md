# Meta Marketing API — ops verification & Standard Access

> Companion to ADR 0008. Use this checklist before re-requesting Marketing API
> **Standard Access**. Do not generate fake Ads API traffic.

## 1. Server configuration (Railway)

| Variable | Purpose |
|---|---|
| `META_APP_ID` | Meta app id |
| `META_APP_SECRET` | Webhook signature (`X-Hub-Signature-256`) |
| `META_ACCESS_TOKEN` | Business **system user** token with ads + leads permissions |
| `META_GRAPH_VERSION` | Default `v22.0` (raise only after checking Meta changelog) |
| `META_AD_ACCOUNT_IDS` | Optional allowlist (`act_…`, comma-separated) |
| `META_WEBHOOK_VERIFY_TOKEN` | Hub challenge for `/api/webhook/meta` |

Never put these on Vercel. Frontend only needs `API_PROXY_TARGET`.

## 2. Functional verification (real business use)

Sign in as MANAGER / ADMIN / CEO on the CRM:

1. **Ad Channels** → authorized accounts load.
2. **Campaigns** → list campaigns; change insights preset; refresh.
3. Confirm `GET /api/marketing/meta/usage?days=15` shows successful
   `adaccounts.list` / `campaigns.list` / `insights.list` rows.
4. (Optional) Submit a test Lead Ad → webhook creates a CRM lead; duplicate
   delivery does not create a second lead.

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
