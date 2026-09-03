-- V12: LGPD — documentos versionados, aceite provado, consentimento por
-- finalidade e direitos do titular.
--
-- Documento Mestre V4.0, secao 22. A plataforma coleta nome, e-mail, WhatsApp,
-- CPF, IP e historico de estudo: isso e tratamento de dado pessoal sob a Lei
-- 13.709/2018. A secao 30 classifica incidente com dado pessoal como risco ALTO,
-- com sancao da ANPD de ate 2% do faturamento, e manda implementar a secao 22
-- "na Fase 1, nao depois".

-- ---------------------------------------------------------------------------
-- "Politica de privacidade e termos de uso versionados, com aceite registrado
--  (data, hora, IP, versao)."
--
-- O texto e versionado porque o aceite precisa apontar para o que a pessoa
-- realmente leu. Guardar so "aceitou os termos", sem saber qual texto estava no
-- ar naquele dia, nao serve como prova de nada.
-- ---------------------------------------------------------------------------
CREATE TABLE legal_documents (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type           TEXT NOT NULL,
    version        TEXT NOT NULL,
    body           TEXT NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT legal_documents_type_check
        CHECK (type IN ('TERMS_OF_USE', 'PRIVACY_POLICY', 'COOKIE_POLICY')),
    CONSTRAINT legal_documents_type_version_unique UNIQUE (type, version)
);

CREATE INDEX legal_documents_vigente_idx
    ON legal_documents (type, effective_from DESC);

CREATE TABLE legal_acceptances (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users (id) ON DELETE SET NULL,
    document_id UUID NOT NULL REFERENCES legal_documents (id),
    ip          TEXT,
    user_agent  TEXT,
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ON DELETE SET NULL, e nao CASCADE: o aceite e a prova de que o consentimento
-- existiu. Apagar a conta nao pode apagar o registro de que ela concordou —
-- some justamente a evidencia que a LGPD manda guardar.
CREATE INDEX legal_acceptances_user_idx ON legal_acceptances (user_id, accepted_at DESC);

-- ---------------------------------------------------------------------------
-- "Consentimento explicito e separado para contato por WhatsApp, com registro
--  de data, hora, IP e texto aceito. Caixa pre-marcada nao e consentimento
--  valido." (secoes 16 e 22)
--
-- Uma linha por decisao, nunca um UPDATE: revogar precisa deixar rastro de que
-- houve consentimento antes. Vale sempre o registro mais recente da finalidade.
-- ---------------------------------------------------------------------------
CREATE TABLE consents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose       TEXT NOT NULL,
    granted       BOOLEAN NOT NULL,
    accepted_text TEXT NOT NULL,
    ip            TEXT,
    recorded_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT consents_purpose_check CHECK (purpose IN (
        'MARKETING_WHATSAPP', 'MARKETING_EMAIL', 'COOKIE_ANALYTICS', 'COOKIE_MARKETING'))
);

CREATE INDEX consents_vigente_idx ON consents (user_id, purpose, recorded_at DESC);

-- ---------------------------------------------------------------------------
-- "Portal do titular no perfil: exportar meus dados (JSON), corrigir, revogar
--  consentimento, excluir conta."
--
-- A tabela registra o pedido mesmo quando o atendimento e imediato: o titular
-- tem direito de saber o que pediu e quando, e o encarregado precisa conseguir
-- demonstrar o prazo de atendimento.
-- ---------------------------------------------------------------------------
CREATE TABLE data_subject_requests (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES users (id) ON DELETE SET NULL,
    user_email   TEXT NOT NULL,
    type         TEXT NOT NULL,
    status       TEXT NOT NULL DEFAULT 'COMPLETED',
    detail       TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT data_subject_requests_type_check
        CHECK (type IN ('EXPORT', 'RECTIFICATION', 'CONSENT_WITHDRAWAL', 'DELETION')),
    CONSTRAINT data_subject_requests_status_check
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'REJECTED'))
);

CREATE INDEX data_subject_requests_user_idx
    ON data_subject_requests (user_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- Marca de anonimizacao na conta.
--
-- Secao 22: "Exclusao anonimiza o cadastro mas preserva o registro fiscal do
-- pedido — obrigacao legal se sobrepoe." A conta continua existindo como
-- chave estrangeira dos pagamentos; o que sai e o dado pessoal.
-- ---------------------------------------------------------------------------
ALTER TABLE users
    ADD COLUMN anonymized_at TIMESTAMPTZ;

COMMENT ON COLUMN users.anonymized_at IS
    'Secao 22 -- quando o titular exerceu o direito de exclusao. Preenchida, o '
    'cadastro esta anonimizado e a linha so existe para sustentar o registro fiscal.';

CREATE INDEX users_anonimizados_idx ON users (anonymized_at) WHERE anonymized_at IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Textos iniciais. O conteudo definitivo entra pelo admin apos revisao
-- juridica; estas linhas existem para que o primeiro cadastro ja tenha a que
-- apontar, em vez de gravar um aceite orfao.
-- ---------------------------------------------------------------------------
INSERT INTO legal_documents (type, version, body) VALUES
('TERMS_OF_USE', '0.1.0-rascunho',
 'RASCUNHO — pendente de revisao juridica antes de ir ao ar. Precisa conter, no minimo: '
 'prazo de acesso (a secao 03 fixa 12 meses, nunca vitalicio); direito de arrependimento '
 'em 7 dias corridos com devolucao integral (CDC art. 49); politica de garantia entre 8 e '
 '30 dias; limite de dispositivos simultaneos; e vedacao ao compartilhamento de conta.'),

('PRIVACY_POLICY', '0.1.0-rascunho',
 'RASCUNHO — pendente de revisao juridica. Precisa listar cada dado coletado com sua base '
 'legal e retencao, conforme a tabela da secao 22: nome, e-mail e CPF por execucao de '
 'contrato, retidos 5 anos apos o fim do acesso (prazo fiscal); WhatsApp para marketing por '
 'consentimento, ate a revogacao; historico de estudo por execucao de contrato, enquanto '
 'durar a conta; IP e log de acesso por obrigacao legal (Marco Civil art. 15), 6 meses; '
 'redacao enviada por execucao de contrato, 2 anos; cookie de analytics por consentimento, '
 'ate a revogacao. Precisa nomear o encarregado (DPO) com canal de contato publicado no '
 'rodape e listar os operadores que tratam dado em nosso nome.'),

('COOKIE_POLICY', '0.1.0-rascunho',
 'RASCUNHO — pendente de revisao juridica. Cookies estritamente necessarios dispensam '
 'consentimento. Analytics e marketing so disparam apos aceite explicito, e a recusa '
 'precisa ser tao facil quanto o aceite (secao 22).');
