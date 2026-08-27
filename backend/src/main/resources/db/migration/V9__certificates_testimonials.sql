-- V9: certificados de conclusao e depoimentos de alunos.

-- Emitido quando o aluno completa 100% do curso. O codigo e publico:
-- qualquer pessoa (um recrutador, por exemplo) valida em /certificado/<code>.
CREATE TABLE certificates (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    code      TEXT NOT NULL,
    hours     INTEGER NOT NULL DEFAULT 0,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT certificates_code_unique UNIQUE (code),
    -- Um certificado por aluno por curso; reemitir devolve o existente.
    CONSTRAINT certificates_user_course_unique UNIQUE (user_id, course_id)
);

CREATE INDEX certificates_user_idx ON certificates (user_id);

-- Depoimento nasce pendente: prova social so vai ao ar depois de aprovada.
CREATE TABLE testimonials (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id  UUID REFERENCES courses (id) ON DELETE SET NULL,
    body       TEXT NOT NULL,
    status     TEXT NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT testimonials_status_check CHECK (status IN ('PENDING', 'APPROVED', 'HIDDEN')),
    CONSTRAINT testimonials_body_check CHECK (length(trim(body)) > 0)
);

CREATE INDEX testimonials_status_idx ON testimonials (status, created_at DESC);
CREATE INDEX testimonials_user_idx ON testimonials (user_id);
