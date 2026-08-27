-- V10: questoes nas aulas — o motor de simulado integrado a area de membros.
-- Uma aula com questoes vira "aula de questoes"; responder conta como
-- progresso e alimenta o historico de tentativas do aluno.

CREATE TABLE quiz_questions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id     UUID NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    statement     TEXT NOT NULL,
    options       JSONB NOT NULL,
    correct_index INTEGER NOT NULL,
    explanation   TEXT,
    position      INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT quiz_questions_statement_check CHECK (length(trim(statement)) > 0),
    CONSTRAINT quiz_questions_correct_check CHECK (correct_index >= 0)
);

CREATE INDEX quiz_questions_lesson_idx ON quiz_questions (lesson_id, position);

-- Cada entrega e um registro imutavel: o aluno refaz quantas vezes quiser
-- e o historico mostra a evolucao (padrao do nicho de concursos).
CREATE TABLE quiz_attempts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    lesson_id     UUID NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    correct_count INTEGER NOT NULL,
    total_count   INTEGER NOT NULL,
    answers       JSONB NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX quiz_attempts_user_lesson_idx ON quiz_attempts (user_id, lesson_id, created_at DESC);
