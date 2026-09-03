-- V14: redacoes e correcao por rubrica.
--
-- Documento Mestre V4.0, secao 14. Na V3.0 isso era uma linha de tabela
-- ("Redacoes — Correcao"); a V4.0 abre porque e o servico mais caro de operar da
-- plataforma e o unico cujo custo cresce com o uso.
--
-- Fluxo: envio -> fila do corretor -> correcao por rubrica -> devolutiva ->
-- reescrita opcional.

-- Secao 14: "Corretor humano com perfil proprio. IA so sugere; nao publica nota."
-- O perfil e separado de INSTRUCTOR porque corrigir redacao e ver o texto de um
-- aluno identificado, e isso nao deve vir junto com "pode editar aula".
INSERT INTO roles (name) VALUES ('ROLE_CORRECTOR');

-- ---------------------------------------------------------------------------
-- Rubrica configuravel por banca.
--
-- Secao 14: "Configuravel por banca -- Cebraspe, FGV, FCC, Cesgranrio. Nota por
-- criterio, nao so total." Nota so total nao ensina nada: o aluno precisa saber
-- em qual criterio perdeu para saber o que treinar.
-- ---------------------------------------------------------------------------
CREATE TABLE essay_rubrics (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001'
               REFERENCES tenants (id),
    board      TEXT NOT NULL,
    name       TEXT NOT NULL,
    -- [{"code":"C1","title":"...","max_score":200}]
    criteria   JSONB NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX essay_rubrics_banca_ativa_unica
    ON essay_rubrics (tenant_id, upper(board)) WHERE active;

-- ---------------------------------------------------------------------------
-- A redacao enviada.
-- ---------------------------------------------------------------------------
CREATE TABLE essays (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001'
                 REFERENCES tenants (id),
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    prompt       TEXT NOT NULL,
    board        TEXT,
    -- Secao 14: "PDF ou imagem, ate 10 MB, com OCR para texto manuscrito."
    file_url     TEXT NOT NULL,
    ocr_text     TEXT,
    status       TEXT NOT NULL DEFAULT 'SUBMITTED',
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Secao 14: "Prazo 7 dias corridos. Contagem visivel ao aluno."
    -- Gravado como data, e nao calculado na leitura: mudar a politica de prazo
    -- no futuro nao pode reescrever o compromisso ja assumido com quem enviou.
    due_at       TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '7 days'),
    -- Secao 14, passo 5: reescrita opcional, ligada a redacao original.
    rewrite_of   UUID REFERENCES essays (id),
    CONSTRAINT essays_status_check CHECK (status IN
        ('SUBMITTED', 'IN_REVIEW', 'CORRECTED', 'REWRITE_SUBMITTED', 'CANCELLED'))
);

-- A fila do corretor: o mais perto do vencimento primeiro, para que o prazo de
-- 7 dias seja cumprido por ordem de urgencia e nao por ordem de chegada.
CREATE INDEX essays_fila_idx ON essays (due_at)
    WHERE status IN ('SUBMITTED', 'IN_REVIEW');
CREATE INDEX essays_aluno_idx ON essays (user_id, submitted_at DESC);

-- ---------------------------------------------------------------------------
-- A devolutiva.
-- ---------------------------------------------------------------------------
CREATE TABLE essay_corrections (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    essay_id       UUID NOT NULL REFERENCES essays (id) ON DELETE CASCADE,
    corrector_id   UUID NOT NULL REFERENCES users (id),
    rubric_id      UUID REFERENCES essay_rubrics (id),
    -- {"C1": 160, "C2": 120, ...} — nota por criterio, conforme a secao 14.
    scores         JSONB NOT NULL DEFAULT '{}'::jsonb,
    total_score    NUMERIC(6,2),
    -- Secao 14: "Anotacao sobre o texto, nota por criterio e comentario final
    -- em audio ou texto."
    comment        TEXT,
    audio_url      TEXT,
    annotations    JSONB NOT NULL DEFAULT '[]'::jsonb,
    -- Secao 14 e 17: a IA pre-analisa para acelerar o corretor, mas o campo e
    -- separado da nota de proposito. Nota e do humano; isto e rascunho.
    ai_draft       TEXT,
    assigned_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at   TIMESTAMPTZ
);

-- Uma correcao por redacao. Sem isso, dois corretores pegando a mesma redacao da
-- fila produzem duas notas diferentes para o mesmo texto.
CREATE UNIQUE INDEX essay_corrections_essay_unique ON essay_corrections (essay_id);
CREATE INDEX essay_corrections_corretor_idx ON essay_corrections (corrector_id, completed_at DESC);

-- ---------------------------------------------------------------------------
-- Cota.
--
-- Secao 14: "Mentoria: 4/mes. Curso avulso: 1 na compra. Avulsa: R$ 49."
-- E o porque, do proprio documento: "Correcao ilimitada destroi a margem: 4
-- correcoes mensais por 12 meses custam R$ 864 por aluno de mentoria contra um
-- ticket de R$ 3.564. A cota e o que mantem a margem da secao 04 de pe."
--
-- A competencia e o primeiro dia do mes: a cota da mentoria e mensal e nao
-- acumula, senao um aluno inativo por seis meses despeja 24 correcoes de uma vez
-- na fila e estoura o prazo de todo mundo.
-- ---------------------------------------------------------------------------
CREATE TABLE essay_quotas (
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    period      DATE NOT NULL,
    available   SMALLINT NOT NULL DEFAULT 0,
    used        SMALLINT NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, period),
    CONSTRAINT essay_quotas_nao_negativa CHECK (available >= 0 AND used >= 0),
    CONSTRAINT essay_quotas_nao_estoura CHECK (used <= available)
);

-- ---------------------------------------------------------------------------
-- Rubricas iniciais das quatro bancas que a secao 14 nomeia.
-- Os pesos espelham o padrao publico de cada uma e sao editaveis no admin.
-- ---------------------------------------------------------------------------
INSERT INTO essay_rubrics (board, name, criteria) VALUES
('CEBRASPE', 'Cebraspe — discursiva padrão', '[
  {"code":"AP","title":"Apresentação e estrutura textual","max_score":10},
  {"code":"DC","title":"Desenvolvimento do tema e coerência","max_score":10},
  {"code":"DT","title":"Domínio técnico do conteúdo","max_score":20},
  {"code":"NG","title":"Nota de gramática (desconto por erro)","max_score":0}
]'::jsonb),

('FGV', 'FGV — discursiva padrão', '[
  {"code":"AT","title":"Apresentação e estrutura","max_score":5},
  {"code":"CT","title":"Conteúdo e argumentação","max_score":15},
  {"code":"LG","title":"Linguagem e norma culta","max_score":10}
]'::jsonb),

('FCC', 'FCC — discursiva padrão', '[
  {"code":"CO","title":"Conteúdo","max_score":15},
  {"code":"ES","title":"Estrutura","max_score":5},
  {"code":"EX","title":"Expressão","max_score":10}
]'::jsonb),

('CESGRANRIO', 'Cesgranrio — discursiva padrão', '[
  {"code":"AB","title":"Abordagem do tema","max_score":10},
  {"code":"AR","title":"Articulação e coesão","max_score":10},
  {"code":"CG","title":"Correção gramatical","max_score":10}
]'::jsonb);
