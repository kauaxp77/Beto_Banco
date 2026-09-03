-- V13: fundacao multi-tenant.
--
-- Documento Mestre V4.0, secao 27: "tenant_id em todas as tabelas com
-- row-level security no PostgreSQL -- nao banco por cliente." E, na
-- consequencia declarada: "Multi-tenancy e decisao de modelo de dados, nao de
-- fase. Se tenant_id nao entrar na Fase 1, a Fase 5 vira reescrita do backend
-- inteiro."
--
-- POR QUE ESTA MIGRACAO NAO MUDA COMPORTAMENTO NENHUM HOJE
--
-- Ela roda contra um banco de producao com alunos, cursos e pagamentos reais.
-- Ligar isolamento de verdade agora exigiria que toda consulta da aplicacao
-- passasse a declarar o tenant corrente -- e qualquer caminho esquecido
-- devolveria zero linhas, o que na pratica seria a plataforma inteira vazia.
--
-- Entao a migracao faz so a metade estrutural, que e a metade cara de fazer
-- depois:
--   1. Cria o tenant raiz e amarra todo registro existente a ele.
--   2. Acrescenta a coluna com DEFAULT, o que preenche as linhas antigas sem
--      travar a tabela para escrita.
--   3. Liga RLS com politica permissiva enquanto nao houver tenant declarado.
--
-- A politica so restringe quando app.tenant_id existe na sessao. Como nenhuma
-- parte da aplicacao define esse parametro ainda, tudo continua exatamente como
-- estava. A Fase 5 nao precisa alterar nenhuma tabela: passa a definir o
-- parametro, e o isolamento entra em vigor sozinho.

CREATE TABLE tenants (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       TEXT NOT NULL,
    -- Secao 27: "Dominio proprio do cliente via CNAME, com SSL automatico."
    domain     TEXT,
    theme      JSONB NOT NULL DEFAULT '{}'::jsonb,
    active     BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT tenants_domain_unique UNIQUE (domain)
);

-- Tenant raiz, com id fixo: a aplicacao precisa conseguir referencia-lo por
-- constante, sem uma consulta a mais em toda requisicao.
INSERT INTO tenants (id, name, domain, active)
VALUES ('00000000-0000-0000-0000-000000000001', 'Beto Banco', NULL, true);

-- ---------------------------------------------------------------------------
-- A coluna nas raizes de propriedade.
--
-- So nas raizes, e nao em toda tabela: lesson_progress pertence a uma lesson,
-- que pertence a um course, que tem dono. Repetir a coluna nas folhas seria
-- desnormalizacao que seis meses depois diverge -- a folha aponta para um
-- tenant e a raiz para outro, e nenhuma consulta avisa.
--
-- DEFAULT no ALTER preenche as linhas existentes sem reescrever a tabela
-- (PostgreSQL 11+), o que importa quando ela ja tem os alunos de producao.
-- ---------------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN tenant_id UUID NOT NULL
    DEFAULT '00000000-0000-0000-0000-000000000001'
    REFERENCES tenants (id);

ALTER TABLE products
    ADD COLUMN tenant_id UUID NOT NULL
    DEFAULT '00000000-0000-0000-0000-000000000001'
    REFERENCES tenants (id);

ALTER TABLE courses
    ADD COLUMN tenant_id UUID NOT NULL
    DEFAULT '00000000-0000-0000-0000-000000000001'
    REFERENCES tenants (id);

ALTER TABLE payments
    ADD COLUMN tenant_id UUID NOT NULL
    DEFAULT '00000000-0000-0000-0000-000000000001'
    REFERENCES tenants (id);

-- Cada tenant tem os proprios termos: a razao social do contrato e outra.
ALTER TABLE legal_documents
    ADD COLUMN tenant_id UUID NOT NULL
    DEFAULT '00000000-0000-0000-0000-000000000001'
    REFERENCES tenants (id);

CREATE INDEX users_tenant_idx     ON users (tenant_id);
CREATE INDEX products_tenant_idx  ON products (tenant_id);
CREATE INDEX courses_tenant_idx   ON courses (tenant_id);
CREATE INDEX payments_tenant_idx  ON payments (tenant_id);

-- O e-mail passa a ser unico POR TENANT, nao globalmente. Dois cursinhos
-- diferentes podem ter o mesmo aluno, e recusar o cadastro do segundo seria
-- vazar a existencia do primeiro para quem tentasse cadastrar.
--
-- Sem lower(): a V2 ja garante minusculas por trigger e por CHECK, entao
-- lower() aqui so criaria um indice funcional que o planejador usa menos.
DROP INDEX IF EXISTS users_email_unique;
CREATE UNIQUE INDEX users_tenant_email_unique ON users (tenant_id, email);

-- Mesma logica para a versao de um documento legal.
ALTER TABLE legal_documents DROP CONSTRAINT IF EXISTS legal_documents_type_version_unique;
CREATE UNIQUE INDEX legal_documents_tenant_type_version_unique
    ON legal_documents (tenant_id, type, version);

-- ---------------------------------------------------------------------------
-- Row-level security, em modo permissivo.
--
-- A politica libera quando app.tenant_id nao esta definido. Isso e proposital e
-- e o que torna esta migracao segura hoje: nenhum caminho da aplicacao define o
-- parametro, entao nada muda. No dia em que a Fase 5 comecar a defini-lo, o
-- isolamento passa a valer sem tocar em uma tabela sequer.
--
-- FORCE ROW LEVEL SECURITY para que a politica valha inclusive para o dono das
-- tabelas -- sem isso, o papel que a aplicacao usa costuma ser exatamente o que
-- escapa da regra.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION tenant_corrente()
RETURNS uuid
LANGUAGE sql STABLE AS
$$ SELECT nullif(current_setting('app.tenant_id', true), '')::uuid $$;

COMMENT ON FUNCTION tenant_corrente() IS
    'Tenant da sessao, vindo de app.tenant_id. NULL significa "sem isolamento", '
    'que e o estado atual: a Fase 5 passa a definir o parametro com SET LOCAL.';

DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['users', 'products', 'courses', 'payments', 'legal_documents'] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format($p$
            CREATE POLICY isolamento_tenant ON %I
            USING (tenant_id = tenant_corrente() OR tenant_corrente() IS NULL)
            WITH CHECK (tenant_id = tenant_corrente() OR tenant_corrente() IS NULL)
        $p$, t);
    END LOOP;
END $$;
