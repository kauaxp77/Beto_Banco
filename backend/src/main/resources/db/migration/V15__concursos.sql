-- V15: sistema de concursos — carreira, orgao, cargo e a ficha do concurso.
--
-- Documento Mestre V4.0, secoes 07 e 11.
--
-- Secao 07 fixa a arquitetura de conteudo em tres niveis (Carreira -> Orgao ->
-- Cargo) e traz uma regra que decide o modelo: "Um concurso pode pertencer a
-- mais de uma carreira -- regra obrigatoria mantida, e e ela que exige a tabela
-- de juncao". Sem a juncao, um concurso do Banco do Brasil so caberia em
-- "Bancaria", e o cargo de TI dele sumiria de quem estuda para Administrativa.
--
-- Secao 11 e sobre confianca: "Salario e vaga incorretos geram reclamacao e
-- perda de confianca. Toda ficha exibe 'verificado em DD/MM/AAAA' e link para a
-- fonte oficial. Ficha sem verificacao ha mais de 60 dias entra em fila de
-- revisao no admin." Por isso verified_at e source_url nao sao opcionais no
-- sentido pratico: sem eles a ficha nao pode ir ao ar.

-- ---------------------------------------------------------------------------
-- Nivel 1 — carreira
-- ---------------------------------------------------------------------------
CREATE TABLE careers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001'
                REFERENCES tenants (id),
    name        TEXT NOT NULL,
    slug        TEXT NOT NULL,
    description TEXT,
    position    INTEGER NOT NULL DEFAULT 0,
    -- A secao 07 escalona as carreiras por fase; as de fase futura ficam
    -- cadastradas e inativas, para nao aparecerem no catalogo antes da hora.
    active      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT careers_tenant_slug_unique UNIQUE (tenant_id, slug)
);

-- ---------------------------------------------------------------------------
-- Nivel 2 — orgao
-- ---------------------------------------------------------------------------
CREATE TABLE agencies (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001'
               REFERENCES tenants (id),
    name       TEXT NOT NULL,
    acronym    TEXT NOT NULL,
    sphere     TEXT NOT NULL,
    state      CHAR(2),
    site_url   TEXT,
    logo_url   TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT agencies_sphere_check
        CHECK (sphere IN ('FEDERAL', 'STATE', 'MUNICIPAL', 'DISTRICT'))
);

CREATE UNIQUE INDEX agencies_tenant_acronym_unique ON agencies (tenant_id, upper(acronym));

-- ---------------------------------------------------------------------------
-- Nivel 3 — cargo
-- ---------------------------------------------------------------------------
CREATE TABLE positions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id          UUID NOT NULL REFERENCES agencies (id) ON DELETE CASCADE,
    name               TEXT NOT NULL,
    education_level    TEXT NOT NULL,
    -- Dinheiro em centavos, inteiro (secao 18). Salario e o campo que a secao 11
    -- destaca como fonte de reclamacao quando sai errado.
    salary_cents       BIGINT,
    weekly_hours       SMALLINT,
    benefits           TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT positions_education_check CHECK (education_level IN
        ('FUNDAMENTAL', 'MEDIO', 'TECNICO', 'SUPERIOR', 'POS')),
    CONSTRAINT positions_salary_check CHECK (salary_cents IS NULL OR salary_cents >= 0)
);

CREATE INDEX positions_agency_idx ON positions (agency_id);

-- ---------------------------------------------------------------------------
-- A ficha do concurso
-- ---------------------------------------------------------------------------
CREATE TABLE contests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001'
                        REFERENCES tenants (id),
    agency_id           UUID NOT NULL REFERENCES agencies (id),
    name                TEXT NOT NULL,
    slug                TEXT NOT NULL,
    -- Banca examinadora. Texto livre porque bancas novas aparecem e travar em
    -- lista fecharia a porta para o concurso que ninguem previu.
    board               TEXT,
    status              TEXT NOT NULL DEFAULT 'EXPECTED',

    vacancies           INTEGER,
    reserve_list        INTEGER,
    salary_cents        BIGINT,
    education_level     TEXT,
    weekly_hours        SMALLINT,
    benefits            TEXT,

    registration_start  DATE,
    registration_end    DATE,
    registration_fee_cents BIGINT,
    exam_date           DATE,

    -- Secao 11: PDF oficial e link para a fonte. A ficha nunca e a autoridade;
    -- ela aponta para quem e.
    official_pdf_url    TEXT,
    source_url          TEXT,

    -- Secao 11: "Data da ultima verificacao" — origem: sistema.
    verified_at         TIMESTAMPTZ,
    verified_by         UUID REFERENCES users (id),

    published_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT contests_tenant_slug_unique UNIQUE (tenant_id, slug),
    CONSTRAINT contests_status_check CHECK (status IN (
        'EXPECTED', 'AUTHORIZED', 'NOTICE_PUBLISHED', 'REGISTRATION_OPEN',
        'REGISTRATION_CLOSED', 'EXAM_TAKEN', 'CLOSED')),
    CONSTRAINT contests_salary_check CHECK (salary_cents IS NULL OR salary_cents >= 0),
    CONSTRAINT contests_fee_check
        CHECK (registration_fee_cents IS NULL OR registration_fee_cents >= 0),
    -- Inscricao que termina antes de comecar e erro de digitacao, e uma ficha
    -- com esse erro manda o aluno perder o prazo.
    CONSTRAINT contests_registration_period_check
        CHECK (registration_start IS NULL OR registration_end IS NULL
               OR registration_end >= registration_start),
    -- Secao 11 -- a regra que protege a confianca: publicar exige ter conferido
    -- e ter de onde. O banco recusa a ficha publicada sem verificacao e sem
    -- fonte, em vez de depender de alguem lembrar disso na tela do admin.
    CONSTRAINT contests_publicado_exige_verificacao
        CHECK (published_at IS NULL OR (verified_at IS NOT NULL AND source_url IS NOT NULL))
);

CREATE INDEX contests_agency_idx ON contests (agency_id);
CREATE INDEX contests_status_idx ON contests (tenant_id, status);
CREATE INDEX contests_publicados_idx ON contests (tenant_id, registration_end DESC)
    WHERE published_at IS NOT NULL;
-- NULLS FIRST: ficha nunca verificada e a mais urgente da fila de revisao.
CREATE INDEX contests_revisao_idx ON contests (verified_at NULLS FIRST);

-- ---------------------------------------------------------------------------
-- A juncao que a secao 07 exige
-- ---------------------------------------------------------------------------
CREATE TABLE contest_careers (
    contest_id UUID NOT NULL REFERENCES contests (id) ON DELETE CASCADE,
    career_id  UUID NOT NULL REFERENCES careers (id) ON DELETE CASCADE,
    PRIMARY KEY (contest_id, career_id)
);

CREATE INDEX contest_careers_career_idx ON contest_careers (career_id);

-- Cargos oferecidos no concurso. Um concurso abre varios cargos, e o mesmo
-- cargo reaparece em edicoes diferentes do concurso.
CREATE TABLE contest_positions (
    contest_id  UUID NOT NULL REFERENCES contests (id) ON DELETE CASCADE,
    position_id UUID NOT NULL REFERENCES positions (id) ON DELETE CASCADE,
    vacancies   INTEGER,
    PRIMARY KEY (contest_id, position_id)
);

-- ---------------------------------------------------------------------------
-- Secao 11 — fila de revisao no admin.
--
-- "Ficha sem verificacao ha mais de 60 dias entra em fila de revisao no admin."
-- Como visao, e nao como coluna calculada: o criterio e uma janela de tempo que
-- se move sozinha, e materializa-lo exigiria um job so para envelhecer linhas.
-- ---------------------------------------------------------------------------
CREATE VIEW vw_contests_para_revisar AS
    SELECT c.id,
           c.tenant_id,
           c.name,
           c.slug,
           c.status,
           c.verified_at,
           c.published_at IS NOT NULL AS publicado,
           COALESCE(EXTRACT(DAY FROM now() - c.verified_at)::int, 9999) AS dias_sem_verificar
      FROM contests c
     WHERE c.verified_at IS NULL
        OR c.verified_at < now() - INTERVAL '60 days';

COMMENT ON VIEW vw_contests_para_revisar IS
    'Secao 11 -- fichas sem verificacao ha mais de 60 dias. Salario e vaga '
    'errados geram reclamacao e perda de confianca; esta fila e o que impede a '
    'ficha de envelhecer sem ninguem olhar.';

-- ---------------------------------------------------------------------------
-- Carreiras da secao 07. As tres do MVP entram ativas; Tribunais e Policial
-- ficam cadastradas e inativas, com a fase registrada na descricao.
-- ---------------------------------------------------------------------------
INSERT INTO careers (name, slug, description, position, active) VALUES
('Bancária',       'bancaria',       'Bancos públicos e de fomento. Ticket alto, editais previsíveis.',        1, true),
('Educacional',    'educacional',    'Magistério e áreas de apoio educacional. Base de alunos já existente.',  2, true),
('Administrativa', 'administrativa', 'Cargos administrativos das três esferas. Maior volume de vagas.',        3, true),
('Tribunais',      'tribunais',      'Tribunais e Ministério Público. Depende do banco de questões — Fase 3.', 4, false),
('Policial',       'policial',       'Carreiras policiais, com TAF. Exige conteúdo específico — Fase 4.',      5, false);

-- Bancos que a plataforma já cobre, com as siglas usadas nas imagens do site.
INSERT INTO agencies (name, acronym, sphere, state, site_url) VALUES
('Banco do Brasil',                                      'BB',       'FEDERAL', NULL, 'https://www.bb.com.br'),
('Caixa Econômica Federal',                              'CAIXA',    'FEDERAL', NULL, 'https://www.caixa.gov.br'),
('Banco Central do Brasil',                              'BACEN',    'FEDERAL', NULL, 'https://www.bcb.gov.br'),
('Banco Nacional de Desenvolvimento Econômico e Social', 'BNDES',    'FEDERAL', NULL, 'https://www.bndes.gov.br'),
('Banco do Nordeste do Brasil',                          'BNB',      'FEDERAL', NULL, 'https://www.bnb.gov.br'),
('Banco da Amazônia',                                    'BASA',     'FEDERAL', NULL, 'https://www.bancoamazonia.com.br'),
('Banco de Brasília',                                    'BRB',      'DISTRICT', 'DF', 'https://www.brb.com.br'),
('Banco do Estado do Rio Grande do Sul',                 'BANRISUL', 'STATE',   'RS', 'https://www.banrisul.com.br'),
('Banco do Estado do Pará',                              'BANPARA',  'STATE',   'PA', 'https://www.banpara.b.br'),
('Banco do Estado do Espírito Santo',                    'BANESTES', 'STATE',   'ES', 'https://www.banestes.com.br'),
('Banco do Estado de Sergipe',                           'BANESE',   'STATE',   'SE', 'https://www.banese.com.br'),
('Banco de Desenvolvimento de Minas Gerais',             'BDMG',     'STATE',   'MG', 'https://www.bdmg.mg.gov.br'),
('Banco de Desenvolvimento do Espírito Santo',           'BANDES',   'STATE',   'ES', 'https://www.bandes.com.br'),
('Banco Regional de Desenvolvimento do Extremo Sul',     'BRDE',     'STATE',   'RS', 'https://www.brde.com.br');
