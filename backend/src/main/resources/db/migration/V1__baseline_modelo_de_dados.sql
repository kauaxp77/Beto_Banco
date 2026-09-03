-- =============================================================================
-- Documento Mestre da Plataforma V4.0 -- Secao 18 (Modelo de dados)
--
-- Regras de modelagem aplicadas em todo o arquivo:
--   * Dinheiro em centavos, sempre BIGINT. Nunca float/numeric para valor.
--   * Datas em UTC com timestamptz. Conversao para America/Sao_Paulo so na exibicao.
--   * tenant_id desde o primeiro dia nas tabelas de catalogo e usuario (secao 27).
--   * Exclusao logica (excluido_em) em usuario, pedido e matricula.
--   * Migracoes versionadas com Flyway, dentro do repositorio.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- -----------------------------------------------------------------------------
-- Dominios de valor
--
-- TEXT + CHECK em vez de CREATE TYPE ... AS ENUM. O PostgreSQL nao converte
-- varchar para tipo enum em parametro de bind, o que obrigaria cast manual em
-- cada consulta do Hibernate; e acrescentar um valor a um enum nativo e DDL que
-- nao roda dentro de transacao. A checagem continua no banco, com a mesma forca.
-- -----------------------------------------------------------------------------



-- =============================================================================
-- IDENTIDADE E ACESSO
-- =============================================================================

CREATE TABLE tenant (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome          TEXT NOT NULL,
    dominio       TEXT UNIQUE,
    tema_json     JSONB NOT NULL DEFAULT '{}'::jsonb,
    ativo         BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tenant raiz. Todo registro da Fase 1 nasce nele; a Fase 5 apenas adiciona linhas.
INSERT INTO tenant (id, nome, dominio, ativo)
VALUES ('00000000-0000-0000-0000-000000000001', 'Plataforma', NULL, TRUE);

CREATE TABLE usuario (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL REFERENCES tenant(id),
    nome                 TEXT NOT NULL,
    email                TEXT NOT NULL,
    senha_hash           TEXT NOT NULL,
    cpf                  TEXT,
    whatsapp             TEXT,
    data_nascimento      DATE,
    email_verificado_em  TIMESTAMPTZ,
    ultimo_acesso_em     TIMESTAMPTZ,
    falhas_login         SMALLINT NOT NULL DEFAULT 0,
    bloqueado_ate        TIMESTAMPTZ,
    mfa_secret           TEXT,
    mfa_ativo            BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em            TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em        TIMESTAMPTZ NOT NULL DEFAULT now(),
    excluido_em          TIMESTAMPTZ
);
-- Email unico por tenant e apenas entre contas vivas (exclusao logica libera o email).
CREATE UNIQUE INDEX ux_usuario_tenant_email ON usuario (tenant_id, lower(email)) WHERE excluido_em IS NULL;
CREATE INDEX ix_usuario_tenant ON usuario (tenant_id) WHERE excluido_em IS NULL;

CREATE TABLE perfil (
    id      SMALLINT PRIMARY KEY,
    codigo  TEXT NOT NULL UNIQUE CHECK (codigo IN ('ALUNO', 'PROFESSOR', 'CORRETOR', 'SUPORTE', 'ADMIN', 'SUPER_ADMIN')),
    nome    TEXT NOT NULL
);
INSERT INTO perfil (id, codigo, nome) VALUES
    (1, 'ALUNO',       'Aluno'),
    (2, 'PROFESSOR',   'Professor'),
    (3, 'CORRETOR',    'Corretor de redacao'),
    (4, 'SUPORTE',     'Suporte'),
    (5, 'ADMIN',       'Administrador'),
    (6, 'SUPER_ADMIN', 'Super administrador');

CREATE TABLE usuario_perfil (
    usuario_id  UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    perfil_id   SMALLINT NOT NULL REFERENCES perfil(id),
    concedido_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, perfil_id)
);

-- Secao 20: refresh de 30 dias com rotacao; reuso invalida a familia inteira.
CREATE TABLE sessao (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id          UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    familia_id          UUID NOT NULL,
    refresh_token_hash  TEXT NOT NULL UNIQUE,
    dispositivo         TEXT,
    user_agent          TEXT,
    ip                  TEXT,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_em           TIMESTAMPTZ NOT NULL,
    revogado_em         TIMESTAMPTZ,
    motivo_revogacao    TEXT
);
CREATE INDEX ix_sessao_usuario_ativa ON sessao (usuario_id) WHERE revogado_em IS NULL;
CREATE INDEX ix_sessao_familia ON sessao (familia_id);

-- Secao 21: token de recuperacao de senha, uso unico, 30 min.
CREATE TABLE token_recuperacao (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,
    expira_em   TIMESTAMPTZ NOT NULL,
    usado_em    TIMESTAMPTZ,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =============================================================================
-- CATALOGO
-- =============================================================================

CREATE TABLE carreira (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL REFERENCES tenant(id),
    nome       TEXT NOT NULL,
    slug       TEXT NOT NULL,
    descricao  TEXT,
    ordem      SMALLINT NOT NULL DEFAULT 0,
    ativo      BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_carreira_tenant_slug ON carreira (tenant_id, slug);

CREATE TABLE orgao (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    nome      TEXT NOT NULL,
    sigla     TEXT NOT NULL,
    esfera    TEXT NOT NULL CHECK (esfera IN ('FEDERAL', 'ESTADUAL', 'MUNICIPAL', 'DISTRITAL')),
    uf        CHAR(2),
    site_url  TEXT,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_orgao_tenant_sigla ON orgao (tenant_id, upper(sigla));

CREATE TABLE cargo (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenant(id),
    orgao_id          UUID NOT NULL REFERENCES orgao(id),
    nome              TEXT NOT NULL,
    escolaridade      TEXT NOT NULL CHECK (escolaridade IN ('FUNDAMENTAL', 'MEDIO', 'TECNICO', 'SUPERIOR', 'POS')),
    salario_centavos  BIGINT NOT NULL DEFAULT 0 CHECK (salario_centavos >= 0),
    carga_horaria_sem SMALLINT,
    criado_em         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_cargo_orgao ON cargo (orgao_id);

CREATE TABLE concurso (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenant(id),
    orgao_id          UUID NOT NULL REFERENCES orgao(id),
    nome              TEXT NOT NULL,
    slug              TEXT NOT NULL,
    banca             TEXT,
    status            TEXT NOT NULL DEFAULT 'PREVISTO' CHECK (status IN ('PREVISTO', 'AUTORIZADO', 'EDITAL_PUBLICADO', 'INSCRICOES_ABERTAS', 'INSCRICOES_ENCERRADAS', 'PROVA_APLICADA', 'ENCERRADO')),
    vagas             INTEGER,
    cadastro_reserva  INTEGER,
    salario_centavos  BIGINT CHECK (salario_centavos IS NULL OR salario_centavos >= 0),
    escolaridade      TEXT,
    beneficios        TEXT,
    inscricao_inicio  DATE,
    inscricao_fim     DATE,
    taxa_centavos     BIGINT CHECK (taxa_centavos IS NULL OR taxa_centavos >= 0),
    data_prova        DATE,
    pdf_url           TEXT,
    fonte_url         TEXT,
    -- Secao 11: ficha sem verificacao ha mais de 60 dias entra em fila de revisao.
    verificado_em     TIMESTAMPTZ,
    verificado_por    UUID REFERENCES usuario(id),
    criado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_concurso_tenant_slug ON concurso (tenant_id, slug);
CREATE INDEX ix_concurso_status ON concurso (tenant_id, status);
CREATE INDEX ix_concurso_revisao ON concurso (verificado_em NULLS FIRST);

-- Secao 07: "um concurso pode pertencer a mais de uma carreira" -- a regra que exige a juncao.
CREATE TABLE concurso_carreira (
    concurso_id UUID NOT NULL REFERENCES concurso(id) ON DELETE CASCADE,
    carreira_id UUID NOT NULL REFERENCES carreira(id) ON DELETE CASCADE,
    PRIMARY KEY (concurso_id, carreira_id)
);
CREATE INDEX ix_concurso_carreira_carreira ON concurso_carreira (carreira_id);

CREATE TABLE curso (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL REFERENCES tenant(id),
    carreira_id      UUID REFERENCES carreira(id),
    cargo_id         UUID REFERENCES cargo(id),
    titulo           TEXT NOT NULL,
    slug             TEXT NOT NULL,
    subtitulo        TEXT,
    descricao        TEXT,
    capa_storage_key TEXT,
    preco_centavos   BIGINT NOT NULL CHECK (preco_centavos >= 0),
    -- Secao 03: acesso nunca vitalicio. 12 meses = 365 dias a contar da aprovacao.
    dias_acesso      INTEGER NOT NULL DEFAULT 365 CHECK (dias_acesso > 0),
    cota_redacao_compra SMALLINT NOT NULL DEFAULT 1,
    publicado_em     TIMESTAMPTZ,
    criado_em        TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_curso_tenant_slug ON curso (tenant_id, slug);
CREATE INDEX ix_curso_publicado ON curso (tenant_id, publicado_em) WHERE publicado_em IS NOT NULL;

CREATE TABLE modulo (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curso_id  UUID NOT NULL REFERENCES curso(id) ON DELETE CASCADE,
    titulo    TEXT NOT NULL,
    ordem     SMALLINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_modulo_curso ON modulo (curso_id, ordem);

CREATE TABLE aula (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    modulo_id       UUID NOT NULL REFERENCES modulo(id) ON DELETE CASCADE,
    professor_id    UUID REFERENCES usuario(id),
    titulo          TEXT NOT NULL,
    descricao       TEXT,
    -- Secao 10: Panda Video como player oficial. Sem YouTube.
    panda_video_id  TEXT,
    vimeo_video_id  TEXT,
    duracao_seg     INTEGER NOT NULL DEFAULT 0 CHECK (duracao_seg >= 0),
    ordem           SMALLINT NOT NULL DEFAULT 0,
    publicado_em    TIMESTAMPTZ,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_aula_modulo ON aula (modulo_id, ordem);

CREATE TABLE material (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curso_id       UUID NOT NULL REFERENCES curso(id) ON DELETE CASCADE,
    aula_id        UUID REFERENCES aula(id) ON DELETE CASCADE,
    titulo         TEXT NOT NULL,
    tipo           TEXT NOT NULL DEFAULT 'PDF' CHECK (tipo IN ('PDF', 'MAPA_MENTAL', 'CRONOGRAMA', 'PLANILHA', 'AUDIO', 'OUTRO')),
    -- Secao 10: arquivo do aluno sai por URL assinada do R2, validade 5 min. Nunca URL publica.
    storage_key    TEXT NOT NULL,
    tamanho_bytes  BIGINT NOT NULL DEFAULT 0,
    content_type   TEXT,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_material_curso ON material (curso_id);

-- =============================================================================
-- CONSUMO
-- =============================================================================

CREATE TABLE matricula (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenant(id),
    usuario_id  UUID NOT NULL REFERENCES usuario(id),
    curso_id    UUID NOT NULL REFERENCES curso(id),
    pedido_id   UUID,
    inicia_em   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_em   TIMESTAMPTZ NOT NULL,
    status      TEXT NOT NULL DEFAULT 'ATIVA' CHECK (status IN ('ATIVA', 'EXPIRADA', 'REVOGADA', 'BLOQUEADA')),
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now(),
    excluido_em TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_matricula_viva ON matricula (usuario_id, curso_id) WHERE excluido_em IS NULL AND status = 'ATIVA';
CREATE INDEX ix_matricula_expira ON matricula (expira_em) WHERE status = 'ATIVA';

CREATE TABLE progresso_aula (
    usuario_id      UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    aula_id         UUID NOT NULL REFERENCES aula(id) ON DELETE CASCADE,
    segundos_vistos INTEGER NOT NULL DEFAULT 0 CHECK (segundos_vistos >= 0),
    concluido_em    TIMESTAMPTZ,
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, aula_id)
);

CREATE TABLE favorito (
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    aula_id    UUID NOT NULL REFERENCES aula(id) ON DELETE CASCADE,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, aula_id)
);

-- Secao 09: sequencia de estudo, meta semanal e conquistas por marco real.
CREATE TABLE meta_estudo (
    usuario_id       UUID PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    horas_semana     SMALLINT NOT NULL DEFAULT 0 CHECK (horas_semana BETWEEN 0 AND 80),
    notificar        BOOLEAN NOT NULL DEFAULT TRUE,
    atualizado_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE dia_estudo (
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    dia        DATE NOT NULL,
    segundos   INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (usuario_id, dia)
);

CREATE TABLE conquista (
    id        SMALLINT PRIMARY KEY,
    codigo    TEXT NOT NULL UNIQUE,
    titulo    TEXT NOT NULL,
    descricao TEXT NOT NULL
);
INSERT INTO conquista (id, codigo, titulo, descricao) VALUES
    (1, 'PRIMEIRA_AULA',    'Primeira aula',    'Concluiu a primeira aula da plataforma.'),
    (2, 'PRIMEIRO_SIMULADO','Primeiro simulado','Enviou o primeiro simulado.'),
    (3, 'DEZ_REDACOES',     '10 redacoes',      'Enviou dez redacoes para correcao.'),
    (4, 'MODULO_CONCLUIDO', 'Modulo concluido', 'Concluiu todas as aulas de um modulo.');

CREATE TABLE usuario_conquista (
    usuario_id   UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    conquista_id SMALLINT NOT NULL REFERENCES conquista(id),
    obtida_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, conquista_id)
);

-- Secao 10: limite de 2 dispositivos simultaneos; alerta acima de 4 IPs em 24h.
CREATE TABLE acesso_ip (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    ip         TEXT NOT NULL,
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_acesso_ip_janela ON acesso_ip (usuario_id, criado_em DESC);

-- =============================================================================
-- COMERCIAL
-- =============================================================================

CREATE TABLE cupom (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL REFERENCES tenant(id),
    codigo         TEXT NOT NULL,
    -- Secao 03: teto de 30% e validade obrigatoria. Sem cupom eterno.
    percentual     SMALLINT NOT NULL CHECK (percentual > 0 AND percentual <= 30),
    usos_max       INTEGER NOT NULL DEFAULT 1 CHECK (usos_max > 0),
    usos           INTEGER NOT NULL DEFAULT 0 CHECK (usos >= 0),
    valido_ate     TIMESTAMPTZ NOT NULL,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Trava otimista: dois checkouts simultaneos nao podem gastar o mesmo uso.
    versao         BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_cupom_tenant_codigo ON cupom (tenant_id, upper(codigo));

CREATE TABLE pedido (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL REFERENCES tenant(id),
    usuario_id        UUID REFERENCES usuario(id),
    email             TEXT NOT NULL,
    nome              TEXT,
    whatsapp          TEXT,
    cpf               TEXT,
    valor_centavos    BIGINT NOT NULL CHECK (valor_centavos >= 0),
    desconto_centavos BIGINT NOT NULL DEFAULT 0 CHECK (desconto_centavos >= 0),
    cupom_id          UUID REFERENCES cupom(id),
    status            TEXT NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'APROVADO', 'RECUSADO', 'CANCELADO', 'ESTORNADO', 'CHARGEBACK', 'EXPIRADO')),
    -- Secao 19: Idempotency-Key obrigatorio em todo POST que cria pedido ou pagamento.
    idempotency_key   TEXT,
    checkout_url      TEXT,
    -- Secao 12: PENDENTE expira em 72h.
    expira_em         TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '72 hours'),
    criado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    excluido_em       TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_pedido_idempotency ON pedido (tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX ix_pedido_status ON pedido (tenant_id, status, criado_em DESC);
CREATE INDEX ix_pedido_email ON pedido (lower(email));

ALTER TABLE matricula ADD CONSTRAINT fk_matricula_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id);

CREATE TABLE pedido_item (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id      UUID NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
    curso_id       UUID NOT NULL REFERENCES curso(id),
    valor_centavos BIGINT NOT NULL CHECK (valor_centavos >= 0)
);
CREATE INDEX ix_pedido_item_pedido ON pedido_item (pedido_id);

CREATE TABLE pagamento (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id      UUID NOT NULL REFERENCES pedido(id),
    gateway        TEXT NOT NULL DEFAULT 'INFINITYPAY',
    gateway_id     TEXT,
    metodo         TEXT,
    parcelas       SMALLINT NOT NULL DEFAULT 1,
    valor_centavos BIGINT NOT NULL CHECK (valor_centavos >= 0),
    status         TEXT NOT NULL CHECK (status IN ('PENDENTE', 'APROVADO', 'RECUSADO', 'CANCELADO', 'ESTORNADO', 'CHARGEBACK', 'EXPIRADO')),
    pago_em        TIMESTAMPTZ,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_pagamento_gateway_id ON pagamento (gateway, gateway_id) WHERE gateway_id IS NOT NULL;

-- Secao 12 -- contrato do webhook. Idempotencia por evento_id com indice unico.
CREATE TABLE webhook_evento (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gateway        TEXT NOT NULL DEFAULT 'INFINITYPAY',
    evento_id      TEXT NOT NULL,
    tipo           TEXT,
    payload_json   JSONB NOT NULL,
    assinatura_ok  BOOLEAN NOT NULL DEFAULT FALSE,
    -- Eventos podem chegar fora de ordem: comparar ocorrido_em antes de aplicar.
    ocorrido_em    TIMESTAMPTZ,
    recebido_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
    processado_em  TIMESTAMPTZ,
    status         TEXT NOT NULL DEFAULT 'RECEBIDO' CHECK (status IN ('RECEBIDO', 'PROCESSANDO', 'PROCESSADO', 'FALHA', 'FILA_MORTA', 'IGNORADO')),
    tentativas     SMALLINT NOT NULL DEFAULT 0,
    proxima_em     TIMESTAMPTZ,
    erro           TEXT
);
CREATE UNIQUE INDEX ux_webhook_evento ON webhook_evento (gateway, evento_id);
CREATE INDEX ix_webhook_fila ON webhook_evento (status, proxima_em) WHERE status IN ('RECEBIDO', 'FALHA');

CREATE TABLE reembolso (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id      UUID NOT NULL REFERENCES pedido(id),
    solicitado_por UUID REFERENCES usuario(id),
    -- Secao 12: CDC art. 49 -- 7 dias corridos, integral, por autoatendimento.
    motivo         TEXT NOT NULL,
    base_legal     TEXT NOT NULL CHECK (base_legal IN ('CDC_ART_49', 'GARANTIA_COMERCIAL', 'FALHA_PLATAFORMA', 'OUTRO')),
    valor_centavos BIGINT NOT NULL CHECK (valor_centavos >= 0),
    percentual_consumido NUMERIC(5,2),
    aprovado_em    TIMESTAMPTZ,
    recusado_em    TIMESTAMPTZ,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE lead (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL REFERENCES tenant(id),
    nome             TEXT NOT NULL,
    email            TEXT NOT NULL,
    whatsapp         TEXT,
    origem           TEXT,
    isca             TEXT,
    estagio          TEXT NOT NULL DEFAULT 'NOVO' CHECK (estagio IN ('NOVO', 'CONTATADO', 'QUALIFICADO', 'NEGOCIANDO', 'GANHO', 'PERDIDO')),
    motivo_perda     TEXT,
    -- Secao 16 e 22: consentimento explicito e separado, com data, hora, IP e texto aceito.
    consentimento_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    criado_em        TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_lead_motivo_perda CHECK (estagio <> 'PERDIDO' OR motivo_perda IS NOT NULL)
);
CREATE UNIQUE INDEX ux_lead_tenant_email ON lead (tenant_id, lower(email));

-- =============================================================================
-- AVALIACAO
-- =============================================================================

CREATE TABLE simulado (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL REFERENCES tenant(id),
    curso_id     UUID REFERENCES curso(id),
    titulo       TEXT NOT NULL,
    descricao    TEXT,
    duracao_min  SMALLINT NOT NULL CHECK (duracao_min > 0),
    gratuito     BOOLEAN NOT NULL DEFAULT FALSE,
    publicado_em TIMESTAMPTZ,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE questao (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL REFERENCES tenant(id),
    carreira_id   UUID REFERENCES carreira(id),
    disciplina    TEXT NOT NULL,
    assunto       TEXT,
    banca         TEXT,
    ano           SMALLINT,
    orgao_id      UUID REFERENCES orgao(id),
    dificuldade   TEXT NOT NULL DEFAULT 'MEDIO' CHECK (dificuldade IN ('FACIL', 'MEDIO', 'DIFICIL', 'MUITO_DIFICIL')),
    enunciado     TEXT NOT NULL,
    -- Secao 13: enunciado de prova oficial e dominio publico; comentario e obra propria.
    comentario    TEXT,
    fonte         TEXT,
    anulada       BOOLEAN NOT NULL DEFAULT FALSE,
    desatualizada BOOLEAN NOT NULL DEFAULT FALSE,
    gabarito_alterado BOOLEAN NOT NULL DEFAULT FALSE,
    criado_por    UUID REFERENCES usuario(id),
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_questao_filtro ON questao (tenant_id, disciplina, banca, ano);

CREATE TABLE alternativa (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    questao_id UUID NOT NULL REFERENCES questao(id) ON DELETE CASCADE,
    texto      TEXT NOT NULL,
    correta    BOOLEAN NOT NULL DEFAULT FALSE,
    ordem      SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_alternativa_questao ON alternativa (questao_id, ordem);

CREATE TABLE simulado_questao (
    simulado_id UUID NOT NULL REFERENCES simulado(id) ON DELETE CASCADE,
    questao_id  UUID NOT NULL REFERENCES questao(id),
    -- Secao 13: peso por disciplina, conforme edital.
    peso        NUMERIC(5,2) NOT NULL DEFAULT 1.00 CHECK (peso > 0),
    ordem       SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (simulado_id, questao_id)
);

CREATE TABLE tentativa (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    simulado_id  UUID NOT NULL REFERENCES simulado(id),
    usuario_id   UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    iniciada_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    prazo_em     TIMESTAMPTZ NOT NULL,
    enviada_em   TIMESTAMPTZ,
    nota         NUMERIC(6,2),
    apelido      TEXT,
    -- Secao 13: antifraude -- troca de aba contada no servidor.
    trocas_aba   SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_tentativa_ranking ON tentativa (simulado_id, nota DESC NULLS LAST) WHERE enviada_em IS NOT NULL;
CREATE UNIQUE INDEX ux_tentativa_aberta ON tentativa (simulado_id, usuario_id) WHERE enviada_em IS NULL;

CREATE TABLE resposta (
    tentativa_id   UUID NOT NULL REFERENCES tentativa(id) ON DELETE CASCADE,
    questao_id     UUID NOT NULL REFERENCES questao(id),
    alternativa_id UUID REFERENCES alternativa(id),
    segundos       INTEGER NOT NULL DEFAULT 0,
    respondida_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tentativa_id, questao_id)
);

CREATE TABLE redacao (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenant(id),
    usuario_id  UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    tema        TEXT NOT NULL,
    banca       TEXT,
    storage_key TEXT NOT NULL,
    texto_ocr   TEXT,
    status      TEXT NOT NULL DEFAULT 'ENVIADA' CHECK (status IN ('ENVIADA', 'EM_CORRECAO', 'CORRIGIDA', 'REESCRITA_ENVIADA', 'CANCELADA')),
    enviada_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Secao 14: prazo de 7 dias corridos, visivel ao aluno.
    prazo_em    TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '7 days'),
    reescrita_de UUID REFERENCES redacao(id)
);
CREATE INDEX ix_redacao_fila ON redacao (status, prazo_em) WHERE status IN ('ENVIADA', 'EM_CORRECAO');

CREATE TABLE rubrica (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    banca     TEXT NOT NULL,
    nome      TEXT NOT NULL,
    -- [{ "codigo": "C1", "titulo": "...", "nota_max": 200 }]
    criterios_json JSONB NOT NULL,
    ativo     BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_rubrica_tenant_banca ON rubrica (tenant_id, upper(banca)) WHERE ativo;

CREATE TABLE correcao (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    redacao_id    UUID NOT NULL REFERENCES redacao(id) ON DELETE CASCADE,
    corretor_id   UUID NOT NULL REFERENCES usuario(id),
    rubrica_id    UUID REFERENCES rubrica(id),
    notas_json    JSONB NOT NULL DEFAULT '{}'::jsonb,
    nota_total    NUMERIC(6,2),
    comentario    TEXT,
    audio_storage_key TEXT,
    -- Secao 17: IA sugere, nunca publica nota.
    pre_analise_ia TEXT,
    atribuida_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    concluida_em  TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_correcao_redacao ON correcao (redacao_id);

-- Secao 14: a cota e o que mantem a margem da secao 04 de pe.
CREATE TABLE cota_redacao (
    usuario_id     UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    competencia    DATE NOT NULL,
    disponiveis    SMALLINT NOT NULL DEFAULT 0 CHECK (disponiveis >= 0),
    usadas         SMALLINT NOT NULL DEFAULT 0 CHECK (usadas >= 0),
    PRIMARY KEY (usuario_id, competencia)
);

-- =============================================================================
-- CONTEUDO E AUDITORIA
-- =============================================================================

CREATE TABLE post (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL REFERENCES tenant(id),
    autor_id       UUID NOT NULL REFERENCES usuario(id),
    -- Secao 15: nenhum post vai ao ar sem aprovacao de uma pessoa.
    revisado_por   UUID REFERENCES usuario(id),
    revisado_em    TIMESTAMPTZ,
    gerado_por_ia  BOOLEAN NOT NULL DEFAULT FALSE,
    titulo         TEXT NOT NULL,
    slug           TEXT NOT NULL,
    resumo         TEXT,
    corpo          TEXT NOT NULL,
    capa_storage_key TEXT,
    -- E-E-A-T: link para o ato oficial e data da ultima atualizacao.
    fonte_url      TEXT,
    fonte_nome     TEXT,
    publicado_em   TIMESTAMPTZ,
    atualizado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_post_revisao_humana CHECK (publicado_em IS NULL OR revisado_por IS NOT NULL)
);
CREATE UNIQUE INDEX ux_post_tenant_slug ON post (tenant_id, slug);

CREATE TABLE log_auditoria (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   UUID REFERENCES tenant(id),
    usuario_id  UUID REFERENCES usuario(id),
    acao        TEXT NOT NULL,
    entidade    TEXT NOT NULL,
    entidade_id TEXT,
    dados_json  JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip          TEXT,
    user_agent  TEXT,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_log_auditoria_busca ON log_auditoria (entidade, entidade_id, criado_em DESC);
CREATE INDEX ix_log_auditoria_usuario ON log_auditoria (usuario_id, criado_em DESC);

-- =============================================================================
-- LGPD -- Secao 22
-- =============================================================================

CREATE TABLE documento_legal (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenant(id),
    tipo        TEXT NOT NULL CHECK (tipo IN ('TERMOS_DE_USO', 'POLITICA_PRIVACIDADE', 'POLITICA_COOKIES')),
    versao      TEXT NOT NULL,
    corpo       TEXT NOT NULL,
    vigente_de  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_documento_legal ON documento_legal (tenant_id, tipo, versao);

CREATE TABLE aceite_legal (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id    UUID REFERENCES usuario(id) ON DELETE SET NULL,
    lead_id       UUID REFERENCES lead(id) ON DELETE SET NULL,
    documento_id  UUID NOT NULL REFERENCES documento_legal(id),
    -- Prova de aceite: data, hora, IP e versao do texto aceito.
    ip            TEXT,
    user_agent    TEXT,
    aceito_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_aceite_usuario ON aceite_legal (usuario_id);

CREATE TABLE consentimento (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID REFERENCES usuario(id) ON DELETE CASCADE,
    lead_id     UUID REFERENCES lead(id) ON DELETE CASCADE,
    finalidade  TEXT NOT NULL CHECK (finalidade IN ('MARKETING_WHATSAPP', 'MARKETING_EMAIL', 'COOKIE_ANALYTICS', 'COOKIE_MARKETING')),
    concedido   BOOLEAN NOT NULL,
    texto_aceito TEXT NOT NULL,
    ip          TEXT,
    registrado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_consentimento_usuario ON consentimento (usuario_id, finalidade, registrado_em DESC);

-- Portal do titular: exportar, corrigir, revogar, excluir.
CREATE TABLE solicitacao_titular (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id   UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    tipo         TEXT NOT NULL CHECK (tipo IN ('EXPORTACAO', 'CORRECAO', 'REVOGACAO', 'EXCLUSAO')),
    status       TEXT NOT NULL DEFAULT 'ABERTA' CHECK (status IN ('ABERTA', 'EM_ANDAMENTO', 'CONCLUIDA', 'RECUSADA')),
    detalhe      TEXT,
    resultado_storage_key TEXT,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
    concluido_em TIMESTAMPTZ
);

-- =============================================================================
-- ANALYTICS -- Secao 25 (nenhum evento antes do consentimento de cookie)
-- =============================================================================

CREATE TABLE evento_analytics (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     UUID NOT NULL REFERENCES tenant(id),
    usuario_id    UUID REFERENCES usuario(id) ON DELETE SET NULL,
    anonimo_id    TEXT,
    nome          TEXT NOT NULL,
    propriedades  JSONB NOT NULL DEFAULT '{}'::jsonb,
    utm_json      JSONB NOT NULL DEFAULT '{}'::jsonb,
    ocorrido_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_evento_analytics ON evento_analytics (tenant_id, nome, ocorrido_em DESC);

-- =============================================================================
-- IA -- Secao 17 (cache por hash da entrada; teto de gasto por aluno/mes)
-- =============================================================================

CREATE TABLE ia_cache (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    funcao       TEXT NOT NULL,
    entrada_hash TEXT NOT NULL,
    modelo       TEXT NOT NULL,
    resposta_json JSONB NOT NULL,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_ia_cache ON ia_cache (funcao, entrada_hash);

CREATE TABLE ia_consumo (
    usuario_id      UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    competencia     DATE NOT NULL,
    tokens_entrada  BIGINT NOT NULL DEFAULT 0,
    tokens_saida    BIGINT NOT NULL DEFAULT 0,
    custo_centavos  BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (usuario_id, competencia)
);
