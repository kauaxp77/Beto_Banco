-- V18: captacao de leads com CRM, e os recursos de engajamento do aluno.
--
-- Documento Mestre Premium V3.0, secoes 11 (Leads + CRM), 8 (recuperacao de
-- vendas) e 5 (recursos obrigatorios da area do aluno).

-- ===========================================================================
-- Parte 1 — Leads e CRM (V3.0, secao 11)
-- ===========================================================================

-- O material que troca conteudo por contato. Existe como tabela, e nao como
-- texto livre no formulario, porque a secao 11 mede a captacao por material
-- ("PDF, Mapa Mental, Cronograma, Questoes"): com string solta, "cronograma",
-- "Cronograma" e "cronogama" viram tres materiais diferentes no relatorio.
CREATE TABLE lead_magnets (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001'
               REFERENCES tenants (id),
    slug       TEXT NOT NULL,
    title      TEXT NOT NULL,
    kind       TEXT NOT NULL,
    -- Entregue so depois do cadastro; a URL nao aparece na listagem publica.
    file_url   TEXT NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT lead_magnets_tenant_slug_unique UNIQUE (tenant_id, slug),
    CONSTRAINT lead_magnets_kind_check
        CHECK (kind IN ('PDF', 'MAPA_MENTAL', 'CRONOGRAMA', 'QUESTOES'))
);

-- A PESSOA, uma por e-mail. Quem baixa tres materiais e um lead com tres
-- eventos, nao tres leads: com uma linha por captacao, o CRM contaria a mesma
-- pessoa tres vezes e a equipe ligaria tres vezes para ela.
CREATE TABLE leads (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001'
                  REFERENCES tenants (id),
    name          TEXT NOT NULL,
    email         TEXT NOT NULL,
    whatsapp      TEXT,
    status        TEXT NOT NULL DEFAULT 'NEW',
    -- Dono no CRM. ON DELETE SET NULL: quando o vendedor sai da empresa o lead
    -- fica sem dono, e nao desaparece junto com ele.
    owner_id      UUID REFERENCES users (id) ON DELETE SET NULL,
    notes         TEXT,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT leads_status_check
        CHECK (status IN ('NEW', 'CONTACTED', 'NEGOTIATING', 'WON', 'LOST'))
);

-- Unicidade sobre lower(email): a normalizacao acontece na aplicacao, mas um
-- INSERT feito por script de importacao nao passa por ela.
CREATE UNIQUE INDEX leads_tenant_email_unique ON leads (tenant_id, lower(email));

-- Fila de trabalho do CRM: quem chegou por ultimo e ainda nao foi contatado.
CREATE INDEX leads_fila_idx ON leads (tenant_id, status, last_seen_at DESC);

-- O historico, append-only. O motivo pelo qual o lead voltou decide quem a
-- equipe liga primeiro — um cartao recusado hoje vale mais que um PDF baixado
-- no mes passado, e sobrescrever perderia exatamente essa diferenca.
CREATE TABLE lead_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id      UUID NOT NULL REFERENCES leads (id) ON DELETE CASCADE,
    source       TEXT NOT NULL,
    magnet_id    UUID REFERENCES lead_magnets (id) ON DELETE SET NULL,
    product_id   UUID REFERENCES products (id) ON DELETE SET NULL,
    amount_cents BIGINT,
    reason       TEXT,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT lead_events_source_check CHECK (source IN (
        'MATERIAL', 'PAGAMENTO_RECUSADO', 'PAGAMENTO_CANCELADO', 'MANUAL')),
    CONSTRAINT lead_events_amount_check CHECK (amount_cents IS NULL OR amount_cents >= 0)
);

CREATE INDEX lead_events_lead_idx ON lead_events (lead_id, occurred_at DESC);

-- Isolamento por tenant nas raizes de propriedade, no mesmo modo permissivo da
-- V13: a politica so restringe quando app.tenant_id esta definido na sessao.
DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['leads', 'lead_magnets'] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format($p$
            CREATE POLICY isolamento_tenant ON %I
            USING (tenant_id = tenant_corrente() OR tenant_corrente() IS NULL)
            WITH CHECK (tenant_id = tenant_corrente() OR tenant_corrente() IS NULL)
        $p$, t);
    END LOOP;
END $$;

-- Os quatro materiais que a secao 11 nomeia. Entram inativos: a URL abaixo e
-- um lugar reservado, e material publicado com link quebrado queima o contato
-- que acabou de ser dado.
INSERT INTO lead_magnets (slug, title, kind, file_url, active) VALUES
('guia-carreira-bancaria', 'Guia da Carreira Bancária',              'PDF',         'https://exemplo.invalid/pendente.pdf', false),
('mapa-mental-conhecimentos-bancarios', 'Mapa Mental de Conhecimentos Bancários', 'MAPA_MENTAL', 'https://exemplo.invalid/pendente.pdf', false),
('cronograma-90-dias', 'Cronograma de 90 Dias',                      'CRONOGRAMA',  'https://exemplo.invalid/pendente.pdf', false),
('caderno-de-questoes-bb', 'Caderno de Questões — Banco do Brasil',  'QUESTOES',    'https://exemplo.invalid/pendente.pdf', false);

-- ===========================================================================
-- Parte 2 — Engajamento do aluno (V3.0, secao 5)
-- ===========================================================================

-- "Continue assistindo" e "Historico".
--
-- Tabela separada de lesson_progress de proposito. Aquela guarda conclusao, e
-- so recebe linha quando a aula termina; o progresso de reproducao existe
-- desde o primeiro segundo. Reaproveita-la exigiria tornar completed_at
-- anulavel, e toda contagem de "aulas concluidas" que hoje conta linhas
-- passaria a contar tambem aula comecada — o progresso do aluno subiria
-- sozinho sem ele concluir nada.
-- Sem duracao propria: ela ja vive em lessons.duration_seconds. Repeti-la aqui
-- criaria duas versoes do mesmo numero, que divergem no dia em que a aula for
-- reeditada — e a barra de progresso passaria a mentir para quem ja assistiu.
CREATE TABLE lesson_playback (
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    lesson_id        UUID NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    position_seconds INTEGER NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, lesson_id),
    CONSTRAINT lesson_playback_position_check CHECK (position_seconds >= 0)
);

-- A ordem do "continue assistindo" e do historico: o mais recente primeiro.
CREATE INDEX lesson_playback_recentes_idx ON lesson_playback (user_id, updated_at DESC);

-- "Favoritos". Na aula, e nao no curso: o aluno marca o ponto ao qual quer
-- voltar na revisao, e "curso favorito" nao diz onde estava a duvida.
CREATE TABLE lesson_favorites (
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    lesson_id  UUID NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, lesson_id)
);

CREATE INDEX lesson_favorites_user_idx ON lesson_favorites (user_id, created_at DESC);
