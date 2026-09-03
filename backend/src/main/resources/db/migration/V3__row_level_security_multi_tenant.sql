-- =============================================================================
-- Secao 27 -- White Label / isolamento
--
-- "tenant_id em todas as tabelas com row-level security no PostgreSQL -- nao
--  banco por cliente."
-- Secao 27, consequencia hoje: "Se tenant_id nao entrar na Fase 1, a Fase 5 vira
-- reescrita do backend inteiro."
--
-- O tenant corrente vem de app.tenant_id, definido pelo TenantFilter no inicio de
-- cada requisicao (SET LOCAL, portanto amarrado a transacao e a conexao do pool
-- nunca vaza o valor para a requisicao seguinte).
-- =============================================================================

CREATE OR REPLACE FUNCTION tenant_corrente()
RETURNS uuid
LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('app.tenant_id', true), '')::uuid $$;

-- A politica so restringe quando ha tenant definido. Sem app.tenant_id (jobs de
-- manutencao, migracao, reconciliacao) a leitura e ampla -- por isso o papel usado
-- por esses jobs nunca deve ser o mesmo do servidor web.
DO $$
DECLARE
    t text;
    tabelas text[] := ARRAY[
        'usuario', 'carreira', 'orgao', 'cargo', 'concurso', 'curso',
        'matricula', 'pedido', 'cupom', 'lead', 'simulado', 'questao',
        'redacao', 'rubrica', 'post', 'evento_analytics'
    ];
BEGIN
    FOREACH t IN ARRAY tabelas LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format($p$
            CREATE POLICY isolamento_tenant ON %I
            USING (tenant_id = tenant_corrente() OR tenant_corrente() IS NULL)
            WITH CHECK (tenant_id = tenant_corrente() OR tenant_corrente() IS NULL)
        $p$, t);
    END LOOP;
END $$;

-- -----------------------------------------------------------------------------
-- Secao 11 -- fila de revisao de ficha de concurso
-- "Ficha sem verificacao ha mais de 60 dias entra em fila de revisao no admin."
-- -----------------------------------------------------------------------------
CREATE VIEW vw_concurso_revisao_pendente AS
    SELECT id, tenant_id, nome, slug, verificado_em,
           COALESCE(EXTRACT(DAY FROM now() - verificado_em)::int, 9999) AS dias_sem_verificar
      FROM concurso
     WHERE verificado_em IS NULL
        OR verificado_em < now() - INTERVAL '60 days';

-- -----------------------------------------------------------------------------
-- Secao 12 -- reconciliacao diaria as 03h compara pedidos locais com o gateway.
-- A visao lista o que a reconciliacao precisa olhar: pago sem matricula viva, e
-- matricula viva sem pedido aprovado. Os dois lados do erro que custa dinheiro.
-- -----------------------------------------------------------------------------
CREATE VIEW vw_divergencia_acesso AS
    SELECT p.id AS pedido_id, p.email, p.status::text AS pedido_status,
           'PAGO_SEM_ACESSO'::text AS divergencia
      FROM pedido p
     WHERE p.status = 'APROVADO'
       AND p.excluido_em IS NULL
       AND NOT EXISTS (
             SELECT 1 FROM matricula m
              WHERE m.pedido_id = p.id AND m.status = 'ATIVA' AND m.excluido_em IS NULL)
    UNION ALL
    SELECT m.pedido_id, u.email, COALESCE(p.status::text, 'SEM_PEDIDO'),
           'ACESSO_SEM_PAGAMENTO'
      FROM matricula m
      JOIN usuario u ON u.id = m.usuario_id
      LEFT JOIN pedido p ON p.id = m.pedido_id
     WHERE m.status = 'ATIVA'
       AND m.excluido_em IS NULL
       AND (p.id IS NULL OR p.status <> 'APROVADO');

COMMENT ON VIEW vw_divergencia_acesso IS
  'Secao 30, risco CRITICO-FINANCEIRO: aluno paga e nao recebe acesso. A reconciliacao das 03h le esta visao e alerta quando ela nao esta vazia.';
