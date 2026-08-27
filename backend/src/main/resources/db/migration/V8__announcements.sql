-- V8: anuncios do professor — comunicacao com a turma na area do aluno.
-- course_id nulo = anuncio geral, visivel para todo aluno logado.

CREATE TABLE announcements (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id  UUID REFERENCES courses (id) ON DELETE CASCADE,
    title      TEXT NOT NULL,
    body       TEXT NOT NULL,
    created_by UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT announcements_title_check CHECK (length(trim(title)) > 0),
    CONSTRAINT announcements_body_check CHECK (length(trim(body)) > 0)
);

CREATE INDEX announcements_course_created_idx ON announcements (course_id, created_at DESC);
CREATE INDEX announcements_created_idx ON announcements (created_at DESC);
