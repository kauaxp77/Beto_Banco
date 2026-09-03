-- V11: ordem de chegada dos webhooks e reconciliacao diaria.
--
-- Documento Mestre V4.0, secao 12 (contrato do webhook). As garantias de
-- assinatura, idempotencia e retry ja existiam desde a V5; faltavam as duas
-- ultimas do contrato:
--
--   "Ordem: eventos podem chegar fora de ordem. Comparar ocorrido_em e ignorar
--    evento mais antigo que o estado atual."
--   "Reconciliacao diaria: job as 03h compara pedidos locais com a API do
--    gateway e reporta divergencias."
--
-- Sem occurred_at nao ha como distinguir "cancelamento posterior" de
-- "cancelamento que ficou preso na fila do provedor e chegou depois da
-- aprovacao" — e os dois levam a decisoes opostas sobre o acesso do aluno.

ALTER TABLE webhook_events
    ADD COLUMN occurred_at TIMESTAMPTZ;

COMMENT ON COLUMN webhook_events.occurred_at IS
    'Momento do evento no provedor. E ele que ordena, nao received_at: a ordem '
    'de chegada aqui depende da fila e das retentativas do gateway.';

-- A fila passa a ser drenada por occurred_at. NULLS FIRST porque evento sem
-- data declarada e quase sempre de um provedor mais simples, sem reordenacao —
-- tratar primeiro custa nada e evita que ele fique para tras indefinidamente.
--
-- webhook_events_status_next_idx (V5) continua: ele responde "quem esta vencido";
-- este responde "em que ordem tratar os vencidos". Sao perguntas diferentes.
CREATE INDEX webhook_events_ordem_idx
    ON webhook_events (occurred_at NULLS FIRST, received_at)
    WHERE status IN ('RECEIVED', 'FAILED');

-- ---------------------------------------------------------------------------
-- Secao 30, risco CRITICO-FINANCEIRO: "aluno paga e nao recebe acesso:
-- reembolso, chargeback e dano de reputacao".
--
-- A visao mostra os dois lados do erro que custa dinheiro. O job das 03h le
-- daqui: enquanto ela estiver vazia, pagamento e acesso estao casados.
-- ---------------------------------------------------------------------------
CREATE VIEW vw_divergencia_acesso AS
    -- Pagou e nao tem acesso. E o lado que a reconciliacao corrige sozinha:
    -- conceder o que ja foi pago nao tem risco. user_id e product_id saem aqui
    -- para que o job conceda direto, sem uma segunda consulta por linha.
    SELECT p.id            AS payment_id,
           p.user_id       AS user_id,
           p.product_id    AS product_id,
           p.buyer_email   AS email,
           p.status        AS payment_status,
           'PAGO_SEM_ACESSO'::text AS divergencia
      FROM payments p
     WHERE p.status = 'APPROVED'
       -- Sem usuario ou sem produto nao ha o que conceder: sao pagamentos que
       -- precisam de triagem humana, e apareceriam aqui como ruido diario.
       AND p.user_id IS NOT NULL
       AND p.product_id IS NOT NULL
       AND NOT EXISTS (
             SELECT 1
               FROM entitlements e
              WHERE e.source = 'PAYMENT'
                AND e.source_ref = p.id::text
                AND e.revoked_at IS NULL)

    UNION ALL

    -- Tem acesso concedido por pagamento que nao esta aprovado. Este lado nunca
    -- e corrigido automaticamente: revogar por engano o acesso de um aluno
    -- legitimo custa mais do que a auditoria manual de um punhado de linhas.
    SELECT p.id,
           e.user_id,
           e.product_id,
           u.email,
           COALESCE(p.status, 'SEM_PAGAMENTO'),
           'ACESSO_SEM_PAGAMENTO'
      FROM entitlements e
      JOIN users u ON u.id = e.user_id
      LEFT JOIN payments p ON p.id::text = e.source_ref
     WHERE e.source = 'PAYMENT'
       AND e.revoked_at IS NULL
       AND (p.id IS NULL OR p.status <> 'APPROVED');

COMMENT ON VIEW vw_divergencia_acesso IS
    'Secao 12/30 -- divergencia entre pagamento e acesso. Vazia = tudo casado.';
