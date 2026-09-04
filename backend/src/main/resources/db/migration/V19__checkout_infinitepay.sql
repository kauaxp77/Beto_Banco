-- V19: o pedido de checkout, que e o que liga o webhook da InfinitePay ao
-- comprador e ao produto.
--
-- Documento Mestre Premium V3.0, secao 8 (Sistema de Pagamentos, "Prioridade
-- Zero"): "Aluno escolhe curso -> Checkout -> InfinityPay -> Webhook -> Criacao
-- de usuario -> Liberacao automatica".
--
-- POR QUE ESTA TABELA PRECISA EXISTIR
--
-- O webhook do Checkout Integrado entrega invoice_slug, order_nsu,
-- transaction_nsu, amount, paid_amount, installments, capture_method e
-- receipt_url. Ele NAO entrega e-mail do comprador nem qualquer referencia ao
-- produto. Sem um pedido gravado do nosso lado, o evento de pagamento aprovado
-- chega sem dizer quem pagou nem pelo que — e nao ha como criar o aluno nem
-- liberar acesso.
--
-- O `order_nsu` enviado na criacao do link e o id desta linha. Ele volta no
-- webhook, e e por ele que o pagamento reencontra o comprador.

CREATE TABLE checkout_orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001'
                    REFERENCES tenants (id),
    product_id      UUID NOT NULL REFERENCES products (id) ON DELETE RESTRICT,

    -- Dados do comprador no momento do checkout. Ficam aqui, e nao em `users`,
    -- porque nesta etapa ainda nao existe conta: ela nasce quando o pagamento
    -- e aprovado (decisao D4 — o aluno recebe link de definicao de senha).
    buyer_email     TEXT NOT NULL,
    buyer_name      TEXT,
    buyer_phone     TEXT,

    -- Secao 18: dinheiro em centavos, inteiro. Copiado do produto no momento
    -- da criacao: se o preco mudar amanha, o que foi cobrado hoje continua
    -- sendo o que esta escrito aqui.
    amount_cents    BIGINT NOT NULL,

    status          TEXT NOT NULL DEFAULT 'CREATED',

    -- Devolvidos pela InfinitePay. A URL vem na criacao do link; o slug e o
    -- transaction_nsu so aparecem no webhook, entao nascem nulos.
    checkout_url    TEXT,
    invoice_slug    TEXT,
    transaction_nsu TEXT,

    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT checkout_orders_status_check
        CHECK (status IN ('CREATED', 'PAID', 'CANCELLED')),
    CONSTRAINT checkout_orders_amount_check CHECK (amount_cents > 0),
    -- Pedido pago sem transacao registrada nao e conciliavel: nao daria para
    -- confirmar o pagamento na API nem achar o comprovante depois.
    CONSTRAINT checkout_orders_pago_tem_transacao
        CHECK (status <> 'PAID' OR (transaction_nsu IS NOT NULL AND paid_at IS NOT NULL))
);

-- O webhook chega pelo slug quando o order_nsu vem ausente ou irreconhecivel.
CREATE INDEX checkout_orders_slug_idx ON checkout_orders (invoice_slug)
    WHERE invoice_slug IS NOT NULL;

-- Pedidos abertos, do mais recente ao mais antigo: e a tela de acompanhamento
-- do admin e a base do relatorio de carrinho abandonado.
CREATE INDEX checkout_orders_abertos_idx ON checkout_orders (tenant_id, status, created_at DESC);

-- Mesmo modo permissivo da V13: a politica so restringe quando app.tenant_id
-- estiver definido na sessao.
ALTER TABLE checkout_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE checkout_orders FORCE ROW LEVEL SECURITY;
CREATE POLICY isolamento_tenant ON checkout_orders
    USING (tenant_id = tenant_corrente() OR tenant_corrente() IS NULL)
    WITH CHECK (tenant_id = tenant_corrente() OR tenant_corrente() IS NULL);

COMMENT ON TABLE checkout_orders IS
    'Secao 8 -- pedido criado antes de mandar o comprador para a InfinitePay. '
    'O id vai como order_nsu e volta no webhook: e o unico elo entre o '
    'pagamento aprovado e quem comprou o que.';
