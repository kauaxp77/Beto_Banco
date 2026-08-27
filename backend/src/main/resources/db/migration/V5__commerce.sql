-- V5: comercio e processamento — produtos, entitlements, pagamentos,
-- webhooks, outbox de e-mail e auditoria.

CREATE TABLE products (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku         TEXT NOT NULL,
    name        TEXT NOT NULL,
    description TEXT,
    price_cents BIGINT NOT NULL,
    currency    TEXT NOT NULL DEFAULT 'BRL',
    active      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT products_sku_unique UNIQUE (sku),
    CONSTRAINT products_price_check CHECK (price_cents >= 0)
);

CREATE INDEX products_active_idx ON products (active);

-- Quem tem direito a que. O entitlement e o que da acesso — a role diz o que
-- a pessoa E, o entitlement diz o que ela COMPROU.
CREATE TABLE entitlements (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id  UUID NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    source      TEXT NOT NULL,
    source_ref  TEXT,
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ,
    revoked_at  TIMESTAMPTZ,
    granted_by  UUID REFERENCES users (id),
    CONSTRAINT entitlements_source_check
        CHECK (source IN ('PAYMENT', 'MANUAL', 'MIGRATION'))
);

-- E este indice parcial que torna a concessao idempotente: conceder duas
-- vezes o mesmo produto ao mesmo aluno colide, em vez de duplicar.
CREATE UNIQUE INDEX entitlements_ativo_unico
    ON entitlements (user_id, product_id) WHERE revoked_at IS NULL;

CREATE INDEX entitlements_user_id_idx ON entitlements (user_id);

CREATE TABLE payments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider                TEXT NOT NULL,
    provider_transaction_id TEXT NOT NULL,
    product_id              UUID REFERENCES products (id),
    user_id                 UUID REFERENCES users (id),
    buyer_email             TEXT NOT NULL,
    buyer_name              TEXT,
    amount_cents            BIGINT NOT NULL,
    currency                TEXT NOT NULL DEFAULT 'BRL',
    status                  TEXT NOT NULL,
    approved_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT payments_provider_tx_unique UNIQUE (provider, provider_transaction_id),
    CONSTRAINT payments_status_check CHECK (status IN
        ('PENDING', 'APPROVED', 'CANCELLED', 'REFUNDED', 'CHARGEBACK', 'FAILED'))
);

CREATE INDEX payments_status_created_idx ON payments (status, created_at);
CREATE INDEX payments_user_id_idx ON payments (user_id);

CREATE TABLE payment_splits (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id   UUID NOT NULL REFERENCES payments (id) ON DELETE CASCADE,
    recipient    TEXT NOT NULL,
    amount_cents BIGINT NOT NULL,
    percentage   NUMERIC(5,2)
);

CREATE INDEX payment_splits_payment_id_idx ON payment_splits (payment_id);

-- A unique em (provider, event_id) E o mecanismo de idempotencia do sistema.
-- Evento repetido colide no indice; quem decide e o banco, nao a aplicacao.
CREATE TABLE webhook_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider        TEXT NOT NULL,
    event_id        TEXT NOT NULL,
    event_type      TEXT,
    payload         JSONB NOT NULL,
    signature_valid BOOLEAN NOT NULL DEFAULT true,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at    TIMESTAMPTZ,
    status          TEXT NOT NULL DEFAULT 'RECEIVED',
    attempts        INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    error_message   TEXT,
    CONSTRAINT webhook_events_provider_event_unique UNIQUE (provider, event_id),
    CONSTRAINT webhook_events_status_check CHECK (status IN
        ('RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED', 'IGNORED', 'MANUAL'))
);

CREATE INDEX webhook_events_status_next_idx ON webhook_events (status, next_attempt_at);

-- E-mail fica FORA da transacao de dominio: mensagem enviada nao tem rollback.
CREATE TABLE email_outbox (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    to_address      TEXT NOT NULL,
    template        TEXT NOT NULL,
    payload         JSONB NOT NULL,
    status          TEXT NOT NULL DEFAULT 'PENDING',
    attempts        INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ,
    error_message   TEXT,
    dedup_key       TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT email_outbox_dedup_unique UNIQUE (dedup_key),
    CONSTRAINT email_outbox_status_check CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX email_outbox_status_next_idx ON email_outbox (status, next_attempt_at);

CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES users (id),
    action        TEXT NOT NULL,
    entity_type   TEXT,
    entity_id     TEXT,
    ip            TEXT,
    user_agent    TEXT,
    result        TEXT NOT NULL DEFAULT 'SUCCESS',
    metadata      JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX audit_logs_actor_created_idx ON audit_logs (actor_user_id, created_at);
CREATE INDEX audit_logs_entity_idx ON audit_logs (entity_type, entity_id);
CREATE INDEX audit_logs_action_created_idx ON audit_logs (action, created_at);
