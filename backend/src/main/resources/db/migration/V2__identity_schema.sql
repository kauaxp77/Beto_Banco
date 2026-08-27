-- V2: esquema de identidade do Nucleo.
-- Nao altera nem remove nenhuma tabela legada.

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         TEXT NOT NULL,
    password_hash TEXT,
    full_name     TEXT NOT NULL,
    status        TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'BLOCKED')),
    CONSTRAINT users_email_lowercase_check CHECK (email = lower(email))
);

-- Email e guardado sempre em minusculas: normalizar na escrita evita que
-- "Fulano@x.com" e "fulano@x.com" virem duas contas para a mesma pessoa.
CREATE OR REPLACE FUNCTION users_normalize_email() RETURNS TRIGGER AS $$
BEGIN
    NEW.email := lower(trim(NEW.email));
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_normalize_email_trigger
    BEFORE INSERT OR UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION users_normalize_email();

CREATE UNIQUE INDEX users_email_unique ON users (email);

CREATE TABLE roles (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    CONSTRAINT roles_name_unique UNIQUE (name)
);

INSERT INTO roles (name) VALUES ('ROLE_STUDENT'), ('ROLE_ADMIN'), ('ROLE_INSTRUCTOR');

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX user_roles_role_id_idx ON user_roles (role_id);

-- O PK de students E o id do usuario. Isso mantem validas as chaves
-- estrangeiras legadas de attempts.student_id e questions.created_by,
-- que apontam para os mesmos UUIDs vindos do Supabase.
CREATE TABLE students (
    id         UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    phone      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
