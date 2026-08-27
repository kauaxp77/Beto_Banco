-- V6: nucleo do curso — cursos, modulos, aulas e progresso do aluno.
-- O acesso continua sendo decidido pelo entitlement: o vinculo
-- course_products diz qual compra libera qual curso (combos incluidos).

CREATE TABLE courses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       TEXT NOT NULL,
    slug        TEXT NOT NULL,
    description TEXT,
    cover_url   TEXT,
    published   BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT courses_slug_unique UNIQUE (slug)
);

-- N:N de proposito: um combo (produto) libera varios cursos, e o mesmo
-- curso pode ser vendido em mais de um produto.
CREATE TABLE course_products (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id  UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT course_products_unique UNIQUE (course_id, product_id)
);

CREATE INDEX course_products_product_idx ON course_products (product_id);

CREATE TABLE course_modules (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    title     TEXT NOT NULL,
    position  INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX course_modules_course_idx ON course_modules (course_id, position);

CREATE TABLE lessons (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id        UUID NOT NULL REFERENCES course_modules (id) ON DELETE CASCADE,
    title            TEXT NOT NULL,
    description      TEXT,
    video_url        TEXT,
    duration_seconds INTEGER,
    position         INTEGER NOT NULL DEFAULT 0,
    published        BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX lessons_module_idx ON lessons (module_id, position);

-- A unique e o mecanismo de idempotencia: concluir a mesma aula duas vezes
-- colide no indice em vez de duplicar — mesmo padrao dos entitlements.
CREATE TABLE lesson_progress (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    lesson_id    UUID NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT lesson_progress_unique UNIQUE (user_id, lesson_id)
);

CREATE INDEX lesson_progress_user_idx ON lesson_progress (user_id);
