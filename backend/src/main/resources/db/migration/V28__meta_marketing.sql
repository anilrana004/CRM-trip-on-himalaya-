-- =====================================================================
-- Meta Marketing API (ADR 0008) — authorized accounts, call audit, leadgen
-- idempotency, and stable external lead key.
-- =====================================================================

ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS meta_lead_id varchar(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_leads_meta_lead_id
    ON leads (meta_lead_id)
    WHERE meta_lead_id IS NOT NULL;

CREATE TABLE meta_ad_accounts (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    meta_account_id varchar(64)  NOT NULL,
    name            varchar(255) NOT NULL,
    currency        varchar(16),
    account_status  integer,
    active          boolean      NOT NULL DEFAULT true,
    last_synced_at  timestamptz,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_meta_ad_accounts_meta_id UNIQUE (meta_account_id)
);

CREATE TABLE meta_api_call_log (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    correlation_id  varchar(64)  NOT NULL,
    operation       varchar(80)  NOT NULL,
    account_id      varchar(64),
    graph_version   varchar(16)  NOT NULL,
    http_status     integer,
    success         boolean      NOT NULL,
    error_code      varchar(64),
    created_at      timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_meta_api_call_log_created ON meta_api_call_log (created_at DESC);
CREATE INDEX idx_meta_api_call_log_op ON meta_api_call_log (operation, created_at DESC);

CREATE TABLE meta_leadgen_events (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    meta_lead_id    varchar(64)  NOT NULL,
    page_id         varchar(64),
    form_id         varchar(64),
    ad_id           varchar(64),
    adgroup_id      varchar(64),
    campaign_id     varchar(64),
    lead_id         uuid REFERENCES leads(id),
    status          varchar(20)  NOT NULL,
    error_message   varchar(1000),
    created_at      timestamptz  NOT NULL DEFAULT now(),
    processed_at    timestamptz,
    CONSTRAINT uq_meta_leadgen_events_lead UNIQUE (meta_lead_id),
    CONSTRAINT ck_meta_leadgen_status CHECK (status IN ('RECEIVED', 'PROCESSED', 'DUPLICATE', 'FAILED'))
);
