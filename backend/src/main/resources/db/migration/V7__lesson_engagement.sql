-- V7: engajamento na aula — comentarios com moderacao, materiais
-- complementares e avaliacao util/nao util.

-- Comentario nasce visivel (padrao Nutror: discussao em tempo real);
-- moderar e ocultar, nunca apagar — o historico fica para auditoria.
CREATE TABLE lesson_comments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id  UUID NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    parent_id  UUID REFERENCES lesson_comments (id) ON DELETE CASCADE,
    body       TEXT NOT NULL,
    status     TEXT NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT lesson_comments_status_check CHECK (status IN ('VISIBLE', 'HIDDEN')),
    CONSTRAINT lesson_comments_body_check CHECK (length(trim(body)) > 0)
);

CREATE INDEX lesson_comments_lesson_idx ON lesson_comments (lesson_id, created_at);
CREATE INDEX lesson_comments_status_idx ON lesson_comments (status, created_at);

CREATE TABLE lesson_materials (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    title     TEXT NOT NULL,
    url       TEXT NOT NULL,
    position  INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX lesson_materials_lesson_idx ON lesson_materials (lesson_id, position);

-- Um voto por aluno por aula; votar de novo troca o voto (upsert na app).
CREATE TABLE lesson_ratings (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id  UUID NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    helpful    BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT lesson_ratings_unique UNIQUE (user_id, lesson_id)
);

CREATE INDEX lesson_ratings_lesson_idx ON lesson_ratings (lesson_id);
