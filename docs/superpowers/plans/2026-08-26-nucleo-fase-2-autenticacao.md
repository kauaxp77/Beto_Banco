# Núcleo — Fase 2: Autenticação — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar autenticação própria — identidade, credenciais, JWT com refresh rotativo e autorização por role — sobre o chassi construído na Fase 1, sem trancar nenhum dos alunos existentes para fora.

**Architecture:** O módulo `users` é dono da identidade e das credenciais; o módulo `auth` é dono dos casos de uso de sessão. As senhas legadas do Supabase são bcrypt e continuam válidas, migrando para Argon2id sozinhas conforme cada aluno entra. O access token é um JWT curto e stateless; o refresh token é um valor opaco persistido apenas como hash, que rotaciona a cada uso e cuja reutilização revoga a cadeia inteira.

**Tech Stack:** Java 21 · Spring Boot 3.5.6 · Spring Security · Spring Data JPA · Flyway · PostgreSQL 17 · JJWT 0.12.x · Argon2 via Bouncy Castle · Bucket4j · JUnit 5 · Testcontainers · ArchUnit

**Spec:** `docs/superpowers/specs/2026-08-26-nucleo-pagamento-acesso-design.md` (seções 3, 5.1, 5.4, 5.6 e 6)

## Global Constraints

- **Java 21** · **Spring Boot 3.5.6** · **PostgreSQL 17** (Testcontainers em `postgres:17-alpine`)
- `spring.jpa.hibernate.ddl-auto=validate` em todos os perfis. Nenhuma tabela criada pelo Hibernate.
- Toda alteração estrutural por migration Flyway versionada. **Nenhuma migration destrutiva.**
- Prefixo de rota `/api/v1` via `server.servlet.context-path`.
- Envelope de erro: `{ "success": false, "error": { code, message, status, path, traceId, timestamp, fieldErrors } }`
- `code` é contrato estável em `SCREAMING_SNAKE_CASE`. **Nenhum `code` pode divergir da natureza do `status`.**
- Access token JWT **HS256, 15 minutos**, com `sub`, `roles`, `iat`, `exp`, `jti`.
- Refresh token: **valor opaco de 256 bits, nunca JWT**, persistido só como hash, validade 30 dias, **rotaciona a cada uso**.
- Senhas: `DelegatingPasswordEncoder`, **Argon2id** para novas, **bcrypt** aceito para legadas, **re-hash no login bem-sucedido**.
- `password_reset_tokens` tem coluna `purpose`: **`FIRST_ACCESS` (72h)** e **`RESET` (1h)**.
- Roles: `ROLE_STUDENT`, `ROLE_ADMIN`, `ROLE_INSTRUCTOR` — **os três obrigatórios**.
- Autorização: negar por padrão; liberar é que precisa ser explícito.
- **Identidade nunca vem do cliente.** Nenhum controller aceita `userId` por path, query ou body.
- Nenhuma resposta contém `password_hash`, hash de token ou campo administrativo desnecessário.
- Todo segredo por variável de ambiente. Timestamps `timestamptz` em UTC. Identificadores UUID.
- Pacote raiz `com.betobanco`. Backend em `backend/`.

## Ambiente

`JAVA_HOME` não está definido. Prefixe todo comando Maven:

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test
```

Docker precisa estar rodando. A suíte começa esta fase com **32 testes verdes**.

## Estado herdado da Fase 1

Já existem e **não devem ser recriados**: `ApiResponse.ok(T)` / `ApiResponse.error(Object)`, `PageResponse.from(Page)`, `PageRequestFactory.of(Integer, Integer, String)`, `ErrorCode` (enum com `httpStatus()`), `BusinessException(ErrorCode, String)`, `NotFoundException(String)`, `ErrorPayload`, `FieldErrorItem`, `GlobalExceptionHandler extends ResponseEntityExceptionHandler`, `TraceIdFilter.HEADER` / `.MDC_KEY`, `com.betobanco.support.PostgresTestBase`, `ModuleBoundariesTest` com três regras ArchUnit, `V1__baseline.sql`.

## Estado real do banco de produção

Confirmado por consulta direta em 2026-08-26 no projeto Supabase `bjnplubfqoltaxfboodl`:

| id | email | hash | role |
|---|---|---|---|
| `d5c9ae01-acf9-4e04-a96a-d40081a7a742` | `teste@gmail.com` | `$2a$10$…` (60 chars) | `ALUNO` |
| `998acc8c-958a-493e-b4fc-bc92db27a344` | `admin@gmail.com` | `$2a$10$…` (60 chars) | `ADMIN` |

**Os dois hashes são bcrypt puro.** Isso é o que torna a migração transparente possível — e é também a armadilha: o `DelegatingPasswordEncoder` identifica o algoritmo pelo prefixo `{id}`, então um hash copiado *sem* `{bcrypt}` faz o login lançar `IllegalArgumentException` em vez de falhar graciosamente. A migration prefixa.

---

## Estrutura de arquivos ao final desta fase

```
backend/src/main/java/com/betobanco/
├── config/
│   └── PasswordEncoderConfig.java
├── security/
│   ├── SecurityConfig.java
│   ├── JwtService.java
│   ├── JwtAuthFilter.java
│   ├── AuthenticatedUser.java
│   ├── EnvelopeAuthenticationEntryPoint.java
│   ├── EnvelopeAccessDeniedHandler.java
│   └── RateLimitFilter.java
├── users/
│   ├── entity/{User,Role,Student}.java
│   ├── repository/{UserRepository,RoleRepository,StudentRepository}.java
│   ├── api/UserAccount.java
│   ├── service/UserService.java
│   ├── dto/{StudentResponse,StudentUpdateRequest}.java
│   └── controller/StudentController.java
├── auth/
│   ├── entity/{RefreshToken,PasswordResetToken,TokenPurpose}.java
│   ├── repository/{RefreshTokenRepository,PasswordResetTokenRepository}.java
│   ├── service/{AuthService,RefreshTokenService,PasswordResetService}.java
│   ├── dto/{LoginRequest,TokenResponse,MeResponse,RegisterRequest,
│   │         ForgotPasswordRequest,ResetPasswordRequest}.java
│   └── controller/AuthController.java
└── shared/exception/ErrorCode.java            (modificado: CLIENT_ERROR)

backend/src/main/resources/db/migration/
├── V2__identity_schema.sql
├── V3__migrate_legacy_profiles.sql
└── V4__auth_tokens.sql
```

---

### Task 1: Pagar a dívida da Fase 1 — `CLIENT_ERROR`

A revisão final da Fase 1 apontou que o fallback para um 4xx sem código próprio devolve `code: MALFORMED_REQUEST` junto de um `status` que não é 400. A spec §8.2 agora proíbe isso: `code` é o contrato pelo qual o frontend decide comportamento, e um `code` que mente sobre a natureza do erro quebra essa garantia.

**Files:**
- Modify: `backend/src/main/java/com/betobanco/shared/exception/ErrorCode.java`
- Modify: `backend/src/main/java/com/betobanco/shared/exception/GlobalExceptionHandler.java` (método `codigoPara`)
- Modify: `backend/src/test/java/com/betobanco/shared/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `ErrorCode`, `GlobalExceptionHandler` (Fase 1)
- Produces: `ErrorCode.CLIENT_ERROR` com `httpStatus()` = `400`, usado apenas como fallback quando nenhum código específico casa com o status.

- [ ] **Step 1: Escrever o teste que falha**

Acrescente a `GlobalExceptionHandlerTest`, dentro da classe do controller de teste, um endpoint que devolve um 4xx sem código próprio:

```java
        @org.springframework.web.bind.annotation.GetMapping("/status-incomum")
        void statusIncomum() {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.I_AM_A_TEAPOT);
        }
```

E o teste:

```java
    @Test
    void statusSemCodigoProprioNaoMenteSobreANatureza() throws Exception {
        mockMvc.perform(get("/teste/status-incomum"))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.error.status").value(418))
                .andExpect(jsonPath("$.error.code").value("CLIENT_ERROR"));
    }
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=GlobalExceptionHandlerTest
```

Esperado: **FALHA** com `expected: CLIENT_ERROR but was: MALFORMED_REQUEST`. Se falhar com `CLIENT_ERROR` não existindo, é falha de compilação — também aceitável, mas continue até ver a asserção falhar depois de criar a constante.

- [ ] **Step 3: Acrescentar a constante**

Em `ErrorCode.java`, acrescente entre `MALFORMED_REQUEST` e `UNAUTHORIZED`:

```java
    CLIENT_ERROR(400),
```

- [ ] **Step 4: Trocar o fallback**

Em `GlobalExceptionHandler.codigoPara`, troque `ErrorCode.MALFORMED_REQUEST` por `ErrorCode.CLIENT_ERROR` no `orElse`:

```java
    private ErrorCode codigoPara(HttpStatusCode statusCode) {
        return Arrays.stream(ErrorCode.values())
                .filter(c -> c.httpStatus() == statusCode.value())
                .findFirst()
                .orElse(statusCode.is5xxServerError()
                        ? ErrorCode.INTERNAL_ERROR
                        : ErrorCode.CLIENT_ERROR);
    }
```

**Cuidado com a ordem do enum:** `CLIENT_ERROR` e `MALFORMED_REQUEST` têm o mesmo `httpStatus()` (400), e o `filter(...).findFirst()` devolve o primeiro na ordem de declaração. Como `MALFORMED_REQUEST` vem antes, um 400 genuíno continua recebendo `MALFORMED_REQUEST` — que é o desejado. **Não inverta a ordem de declaração.**

- [ ] **Step 5: Rodar e confirmar que passa**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test
```

Esperado: **33 testes verdes**. Confirme em especial que `corpoIlegivelRetorna400` continua esperando `MALFORMED_REQUEST` — se ele quebrou, a ordem do enum está errada.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/betobanco/shared/exception backend/src/test/java/com/betobanco/shared/exception
git commit -m "fix(backend): code de fallback deixa de mentir sobre a natureza do status"
```

---

### Task 2: Esquema de identidade (migration V2)

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__identity_schema.sql`
- Create: `backend/src/test/java/com/betobanco/users/IdentitySchemaTest.java`

**Interfaces:**
- Consumes: `com.betobanco.support.PostgresTestBase`
- Produces: as tabelas `users`, `roles`, `user_roles`, `students`, com as três roles já semeadas. A Task 4 mapeia entidades JPA contra exatamente estas colunas.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/users/IdentitySchemaTest.java`:

```java
package com.betobanco.users;

import com.betobanco.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IdentitySchemaTest extends PostgresTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void asQuatroTabelasDeIdentidadeExistem() {
        List<String> tabelas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tabelas).contains("users", "roles", "user_roles", "students");
    }

    @Test
    void asTresRolesObrigatoriasEstaoSemeadas() {
        List<String> roles = jdbc.queryForList("SELECT name FROM roles ORDER BY name", String.class);

        assertThat(roles).containsExactly("ROLE_ADMIN", "ROLE_INSTRUCTOR", "ROLE_STUDENT");
    }

    @Test
    void emailEhUnicoEArmazenadoEmMinusculas() {
        jdbc.update("INSERT INTO users (id, email, full_name, status) "
                + "VALUES (gen_random_uuid(), 'Alguem@Exemplo.COM', 'Alguém', 'ACTIVE')");

        String gravado = jdbc.queryForObject(
                "SELECT email FROM users WHERE full_name = 'Alguém'", String.class);
        assertThat(gravado).isEqualTo("alguem@exemplo.com");

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'users' AND indexdef ILIKE '%UNIQUE%email%'",
                Integer.class)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void studentUsaOMesmoIdentificadorDoUsuario() {
        String fk = jdbc.queryForObject(
                "SELECT ccu.column_name FROM information_schema.table_constraints tc "
                        + "JOIN information_schema.constraint_column_usage ccu "
                        + "  ON tc.constraint_name = ccu.constraint_name "
                        + "WHERE tc.table_name = 'students' AND tc.constraint_type = 'FOREIGN KEY'",
                String.class);

        assertThat(fk).isEqualTo("id");
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=IdentitySchemaTest
```

Esperado: **FALHA** — `assertThat(tabelas).contains(...)` não encontra as tabelas.

- [ ] **Step 3: Escrever a migration**

`backend/src/main/resources/db/migration/V2__identity_schema.sql`:

```sql
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
```

- [ ] **Step 4: Rodar e confirmar que passa**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=IdentitySchemaTest
```

Esperado: **PASSA**, quatro testes verdes.

- [ ] **Step 5: Rodar a suíte inteira e commitar**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test
git add backend/src/main/resources/db/migration backend/src/test/java/com/betobanco/users
git commit -m "feat(backend): esquema de identidade com users, roles e students"
```

Esperado: **37 testes verdes**.

---

### Task 3: Migração dos perfis legados (migration V3)

Esta é a tarefa mais delicada da fase. A migration precisa rodar em **dois ambientes que não se parecem**: no PostgreSQL de produção, onde `auth.users` e `public.profiles` existem com dados reais; e num contêiner vazio do Testcontainers, onde não existem. Uma migration que assume a presença das tabelas legadas quebra toda a suíte de testes.

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__migrate_legacy_profiles.sql`
- Create: `backend/src/test/java/com/betobanco/users/LegacyMigrationTest.java`

**Interfaces:**
- Consumes: tabelas da Task 2
- Produces: nada em código. Garante que, em produção, os usuários legados existem em `users` com hash prefixado `{bcrypt}` e as roles mapeadas.

- [ ] **Step 1: Escrever o teste que falha**

Este teste **não** estende `PostgresTestBase`: ele sobe um contêiner próprio, cria o esquema legado do Supabase, e roda o Flyway programaticamente. É a única forma de exercitar o caminho que só existe em produção.

`backend/src/test/java/com/betobanco/users/LegacyMigrationTest.java`:

```java
package com.betobanco.users;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercita a V3 contra um banco que TEM o esquema legado do Supabase.
 * Sobe contêiner proprio porque o do PostgresTestBase ja teve o Flyway
 * aplicado sobre um banco vazio — que e o outro caminho, coberto pelo
 * ultimo teste desta classe.
 */
class LegacyMigrationTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("legado")
                    .withUsername("legado")
                    .withPassword("legado");

    private static JdbcTemplate jdbc;

    // Hash bcrypt real de "senha123", no mesmo formato $2a$10$ encontrado na
    // instancia de producao.
    private static final String HASH_LEGADO =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @BeforeAll
    static void prepararBancoLegado() {
        POSTGRES.start();
        DriverManagerDataSource ds = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(ds);

        jdbc.execute("CREATE SCHEMA auth");
        jdbc.execute("""
                CREATE TABLE auth.users (
                    id UUID PRIMARY KEY,
                    email TEXT NOT NULL,
                    encrypted_password TEXT,
                    email_confirmed_at TIMESTAMPTZ
                )""");
        jdbc.execute("""
                CREATE TYPE user_role AS ENUM ('ALUNO', 'PROFESSOR', 'ADMIN', 'SUPER_ADMIN')""");
        jdbc.execute("""
                CREATE TABLE public.profiles (
                    id UUID PRIMARY KEY REFERENCES auth.users (id),
                    full_name TEXT NOT NULL,
                    role user_role NOT NULL DEFAULT 'ALUNO',
                    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
                )""");

        inserirLegado("d5c9ae01-acf9-4e04-a96a-d40081a7a742", "teste@gmail.com", "Aluno Legado", "ALUNO");
        inserirLegado("998acc8c-958a-493e-b4fc-bc92db27a344", "ADMIN@Gmail.com", "Admin Legado", "ADMIN");
        inserirLegado("11111111-1111-1111-1111-111111111111", "prof@gmail.com", "Prof Legado", "PROFESSOR");
        inserirLegado("22222222-2222-2222-2222-222222222222", "super@gmail.com", "Super Legado", "SUPER_ADMIN");
        // Usuario sem senha: representa quem nunca definiu credencial.
        jdbc.update("INSERT INTO auth.users (id, email, encrypted_password) VALUES (?::uuid, ?, NULL)",
                "33333333-3333-3333-3333-333333333333", "semsenha@gmail.com");
        jdbc.update("INSERT INTO public.profiles (id, full_name, role) VALUES (?::uuid, ?, ?::user_role)",
                "33333333-3333-3333-3333-333333333333", "Sem Senha", "ALUNO");

        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private static void inserirLegado(String id, String email, String nome, String role) {
        jdbc.update("INSERT INTO auth.users (id, email, encrypted_password) VALUES (?::uuid, ?, ?)",
                id, email, HASH_LEGADO);
        jdbc.update("INSERT INTO public.profiles (id, full_name, role) VALUES (?::uuid, ?, ?::user_role)",
                id, nome, role);
    }

    @AfterAll
    static void encerrar() {
        POSTGRES.stop();
    }

    @Test
    void todosOsPerfisLegadosViraramUsuarios() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(5);
    }

    @Test
    void oIdentificadorEhPreservado() {
        String email = jdbc.queryForObject(
                "SELECT email FROM users WHERE id = 'd5c9ae01-acf9-4e04-a96a-d40081a7a742'::uuid",
                String.class);

        assertThat(email).isEqualTo("teste@gmail.com");
    }

    @Test
    void oHashLegadoRecebeOPrefixoBcrypt() {
        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE email = 'teste@gmail.com'", String.class);

        assertThat(hash).isEqualTo("{bcrypt}" + HASH_LEGADO);
    }

    @Test
    void emailLegadoComMaiusculasEhNormalizado() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = 'admin@gmail.com'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void usuarioSemSenhaMigraComHashNulo() {
        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE email = 'semsenha@gmail.com'", String.class);

        assertThat(hash).isNull();
    }

    @Test
    void asRolesLegadasSaoMapeadas() {
        assertThat(roleDe("teste@gmail.com")).containsExactly("ROLE_STUDENT");
        assertThat(roleDe("admin@gmail.com")).containsExactly("ROLE_ADMIN");
        assertThat(roleDe("prof@gmail.com")).containsExactly("ROLE_INSTRUCTOR");
        assertThat(roleDe("super@gmail.com")).containsExactly("ROLE_ADMIN");
    }

    @Test
    void somenteAlunosGanhamRegistroEmStudents() {
        List<String> ids = jdbc.queryForList(
                "SELECT u.email FROM students s JOIN users u ON u.id = s.id ORDER BY u.email",
                String.class);

        assertThat(ids).containsExactly("semsenha@gmail.com", "teste@gmail.com");
    }

    @Test
    void reexecutarAMigracaoNaoDuplica() {
        // A V3 ja rodou uma vez pelo Flyway. Rodar o corpo dela de novo tem de
        // ser inofensivo: e o que acontece se alguem precisar reaplicar o script
        // manualmente em producao.
        jdbc.execute(corpoDaV3());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(5);
        // Cinco usuarios legados, uma role cada. ADMIN e SUPER_ADMIN mapeiam
        // ambos para ROLE_ADMIN, mas sao usuarios distintos: duas linhas.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_roles", Integer.class)).isEqualTo(5);
    }

    private List<String> roleDe(String email) {
        return jdbc.queryForList(
                "SELECT r.name FROM user_roles ur "
                        + "JOIN roles r ON r.id = ur.role_id "
                        + "JOIN users u ON u.id = ur.user_id WHERE u.email = ?",
                String.class, email);
    }

    private String corpoDaV3() {
        try (var in = getClass().getResourceAsStream("/db/migration/V3__migrate_legacy_profiles.sql")) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("nao foi possivel ler a V3", e);
        }
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=LegacyMigrationTest
```

Esperado: **FALHA** — `SELECT COUNT(*) FROM users` devolve 0, porque a V3 ainda não existe.

- [ ] **Step 3: Escrever a migration**

`backend/src/main/resources/db/migration/V3__migrate_legacy_profiles.sql`:

```sql
-- V3: migra os usuarios do Supabase para o esquema proprio.
--
-- Roda em dois ambientes que nao se parecem: em producao, onde auth.users e
-- public.profiles existem; e num banco vazio de teste, onde nao existem. A
-- guarda to_regclass torna o script um no-op no segundo caso, em vez de
-- quebrar toda a suite.
--
-- Nada e apagado. As tabelas legadas seguem intactas: os sub-projetos 2 e 3
-- ainda dependem de questions, attempts e site_settings.

DO $$
DECLARE
    v_migrados INTEGER;
BEGIN
    IF to_regclass('public.profiles') IS NULL OR to_regclass('auth.users') IS NULL THEN
        RAISE NOTICE 'Esquema legado ausente; V3 nao tem o que migrar.';
        RETURN;
    END IF;

    -- 1. Usuarios. O UUID e preservado para manter validas as chaves
    --    estrangeiras legadas (attempts.student_id, questions.created_by).
    --    O prefixo {bcrypt} e obrigatorio: o DelegatingPasswordEncoder
    --    identifica o algoritmo por ele, e sem o prefixo todo login lanca
    --    IllegalArgumentException em vez de simplesmente recusar a senha.
    INSERT INTO users (id, email, password_hash, full_name, status, created_at)
    SELECT p.id,
           lower(trim(au.email)),
           CASE WHEN au.encrypted_password IS NULL OR au.encrypted_password = ''
                THEN NULL
                ELSE '{bcrypt}' || au.encrypted_password
           END,
           COALESCE(NULLIF(trim(p.full_name), ''), split_part(au.email, '@', 1)),
           'ACTIVE',
           COALESCE(p.created_at, now())
    FROM public.profiles p
    JOIN auth.users au ON au.id = p.id
    ON CONFLICT (id) DO NOTHING;

    GET DIAGNOSTICS v_migrados = ROW_COUNT;

    -- 2. Roles. ALUNO -> STUDENT, PROFESSOR -> INSTRUCTOR,
    --    ADMIN e SUPER_ADMIN -> ADMIN.
    INSERT INTO user_roles (user_id, role_id)
    SELECT p.id, r.id
    FROM public.profiles p
    JOIN roles r ON r.name = CASE p.role::text
                                 WHEN 'ALUNO'       THEN 'ROLE_STUDENT'
                                 WHEN 'PROFESSOR'   THEN 'ROLE_INSTRUCTOR'
                                 WHEN 'ADMIN'       THEN 'ROLE_ADMIN'
                                 WHEN 'SUPER_ADMIN' THEN 'ROLE_ADMIN'
                                 ELSE 'ROLE_STUDENT'
                             END
    WHERE EXISTS (SELECT 1 FROM users u WHERE u.id = p.id)
    ON CONFLICT (user_id, role_id) DO NOTHING;

    -- 3. Perfil de aluno so para quem e aluno. Admin e instrutor nao estudam.
    INSERT INTO students (id)
    SELECT p.id
    FROM public.profiles p
    WHERE p.role::text = 'ALUNO'
      AND EXISTS (SELECT 1 FROM users u WHERE u.id = p.id)
    ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE 'V3: % usuario(s) legado(s) migrado(s).', v_migrados;
END $$;
```

- [ ] **Step 4: Rodar e confirmar que passa**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=LegacyMigrationTest
```

Esperado: **PASSA**, oito testes verdes.

- [ ] **Step 5: Confirmar o outro caminho — banco sem esquema legado**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=IdentitySchemaTest
```

Esperado: **PASSA**. Este é o teste que prova que a guarda `to_regclass` funciona: o `PostgresTestBase` sobe um banco vazio, e a V3 tem de ser um no-op ali. Se este teste quebrar, a guarda está errada.

- [ ] **Step 6: Rodar a suíte inteira e commitar**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test
git add backend/src/main/resources/db/migration backend/src/test/java/com/betobanco/users
git commit -m "feat(backend): migra perfis do Supabase preservando UUID e hash bcrypt"
```

Esperado: **45 testes verdes**.

---

### Task 4: Entidades e repositórios de identidade

**Files:**
- Create: `backend/src/main/java/com/betobanco/users/entity/{User,Role,Student}.java`
- Create: `backend/src/main/java/com/betobanco/users/repository/{UserRepository,RoleRepository,StudentRepository}.java`
- Create: `backend/src/test/java/com/betobanco/users/UserRepositoryTest.java`

**Interfaces:**
- Consumes: tabelas das Tasks 2 e 3
- Produces:
  - `User`: `getId(): UUID` · `getEmail(): String` · `getPasswordHash(): String` · `setPasswordHash(String)` · `getFullName(): String` · `setFullName(String)` · `getStatus(): String` · `setStatus(String)` · `isActive(): boolean` · `getRoles(): Set<Role>` · construtor `User(String email, String passwordHash, String fullName)`
  - `Role`: `getId(): UUID` · `getName(): String`
  - `Student`: `getId(): UUID` · `getPhone(): String` · `setPhone(String)` · construtor `Student(UUID id)`
  - `UserRepository.findByEmailIgnoreCase(String): Optional<User>` · `existsByEmailIgnoreCase(String): boolean`
  - `RoleRepository.findByName(String): Optional<Role>`
  - `StudentRepository extends JpaRepository<Student, UUID>`

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/users/UserRepositoryTest.java`:

```java
package com.betobanco.users;

import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class UserRepositoryTest extends PostgresTestBase {

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Test
    void persisteEBuscaPorEmailIgnorandoCaixa() {
        Role aluno = roles.findByName("ROLE_STUDENT").orElseThrow();
        User novo = new User("Maria@Exemplo.com", "{bcrypt}xxx", "Maria");
        novo.getRoles().add(aluno);
        users.saveAndFlush(novo);

        assertThat(users.findByEmailIgnoreCase("MARIA@exemplo.COM")).isPresent();
        assertThat(users.existsByEmailIgnoreCase("maria@exemplo.com")).isTrue();
    }

    @Test
    void idEhGeradoEStatusPadraoEhAtivo() {
        User novo = users.saveAndFlush(new User("joao@exemplo.com", null, "Joao"));

        assertThat(novo.getId()).isNotNull();
        assertThat(novo.getStatus()).isEqualTo("ACTIVE");
        assertThat(novo.isActive()).isTrue();
    }

    @Test
    void asRolesSaoCarregadas() {
        Role admin = roles.findByName("ROLE_ADMIN").orElseThrow();
        User novo = new User("chefe@exemplo.com", "{bcrypt}xxx", "Chefe");
        novo.getRoles().add(admin);
        users.saveAndFlush(novo);

        User lido = users.findByEmailIgnoreCase("chefe@exemplo.com").orElseThrow();
        assertThat(lido.getRoles()).extracting(Role::getName).containsExactly("ROLE_ADMIN");
    }

    @Test
    void asTresRolesObrigatoriasExistem() {
        assertThat(roles.findByName("ROLE_STUDENT")).isPresent();
        assertThat(roles.findByName("ROLE_ADMIN")).isPresent();
        assertThat(roles.findByName("ROLE_INSTRUCTOR")).isPresent();
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=UserRepositoryTest
```

Esperado: **FALHA de compilação** — nenhuma das classes existe.

- [ ] **Step 3: Criar `Role`**

`backend/src/main/java/com/betobanco/users/entity/Role.java`:

```java
package com.betobanco.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    protected Role() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
```

- [ ] **Step 4: Criar `User`**

`backend/src/main/java/com/betobanco/users/entity/User.java`:

```java
package com.betobanco.users.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    public static final String ACTIVE = "ACTIVE";
    public static final String BLOCKED = "BLOCKED";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String status = ACTIVE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected User() {
    }

    public User(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
```

**Por que `createdAt` e `updatedAt` são `insertable = false, updatable = false`:** o trigger `users_normalize_email_trigger` da V2 já mantém `updated_at`, e o `DEFAULT now()` cuida de `created_at`. Deixar o Hibernate escrever essas colunas criaria duas fontes de verdade para o mesmo dado — o banco venceria de qualquer forma, e o objeto em memória ficaria mentindo até o próximo `refresh`.

- [ ] **Step 5: Criar `Student`**

`backend/src/main/java/com/betobanco/users/entity/Student.java`:

```java
package com.betobanco.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "students")
public class Student {

    @Id
    private UUID id;

    private String phone;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected Student() {
    }

    public Student(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
```

**`Student` não tem `@GeneratedValue` de propósito:** o `id` dele *é* o id do usuário, atribuído explicitamente. Gerar um novo criaria duas identidades para a mesma pessoa e quebraria as chaves estrangeiras legadas de `attempts.student_id`.

- [ ] **Step 6: Criar os três repositórios**

`backend/src/main/java/com/betobanco/users/repository/UserRepository.java`:

```java
package com.betobanco.users.repository;

import com.betobanco.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
```

`backend/src/main/java/com/betobanco/users/repository/RoleRepository.java`:

```java
package com.betobanco.users.repository;

import com.betobanco.users.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);
}
```

`backend/src/main/java/com/betobanco/users/repository/StudentRepository.java`:

```java
package com.betobanco.users.repository;

import com.betobanco.users.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
}
```

- [ ] **Step 7: Rodar a suíte e confirmar que passa**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test
```

Esperado: **49 testes verdes**. Se falhar com `Schema-validation: missing column` ou `wrong column type`, a entidade e a migration divergem — corrija a **entidade**, nunca relaxando o `ddl-auto=validate`.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/betobanco/users backend/src/test/java/com/betobanco/users
git commit -m "feat(backend): entidades e repositorios de identidade"
```

---

### Task 5: Codificação de senhas com migração transparente

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/betobanco/config/PasswordEncoderConfig.java`
- Create: `backend/src/test/java/com/betobanco/config/PasswordEncoderConfigTest.java`

**Interfaces:**
- Produces: bean `PasswordEncoder` (`DelegatingPasswordEncoder`); constantes `PasswordEncoderConfig.ID_ATUAL` (`"argon2"`) e `PasswordEncoderConfig.PREFIXO_ATUAL` (`"{argon2}"`). A Task 9 compara o hash contra `PREFIXO_ATUAL` para decidir o re-hash.

- [ ] **Step 1: Adicionar Bouncy Castle**

Argon2 no Spring Security exige Bouncy Castle em runtime. Em `backend/pom.xml`, dentro de `<dependencies>`:

```xml
		<dependency>
			<groupId>org.bouncycastle</groupId>
			<artifactId>bcprov-jdk18on</artifactId>
			<version>1.78.1</version>
		</dependency>
```

- [ ] **Step 2: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/config/PasswordEncoderConfigTest.java`:

```java
package com.betobanco.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderConfigTest {

    private final PasswordEncoder encoder = new PasswordEncoderConfig().passwordEncoder();

    @Test
    void senhaNovaEhCodificadaEmArgon2() {
        String hash = encoder.encode("senha-forte-123");

        assertThat(hash).startsWith(PasswordEncoderConfig.PREFIXO_ATUAL);
        assertThat(encoder.matches("senha-forte-123", hash)).isTrue();
        assertThat(encoder.matches("outra-senha", hash)).isFalse();
    }

    @Test
    void hashLegadoDoSupabaseContinuaValido() {
        String legado = "{bcrypt}" + new BCryptPasswordEncoder().encode("senha-antiga");

        assertThat(encoder.matches("senha-antiga", legado)).isTrue();
        assertThat(encoder.matches("senha-errada", legado)).isFalse();
    }

    @Test
    void hashSemPrefixoEhRecusadoSemExplodir() {
        // Documenta por que a migration V3 grava "{bcrypt}$2a$10$...": sem o
        // prefixo o DelegatingPasswordEncoder nao sabe qual algoritmo usar.
        // O comportamento correto e recusar, nao lancar excecao.
        String semPrefixo = new BCryptPasswordEncoder().encode("senha-antiga");

        assertThat(encoder.matches("senha-antiga", semPrefixo)).isFalse();
    }

    @Test
    void duasCodificacoesDaMesmaSenhaDiferem() {
        assertThat(encoder.encode("igual")).isNotEqualTo(encoder.encode("igual"));
    }
}
```

- [ ] **Step 3: Rodar e confirmar que falha**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=PasswordEncoderConfigTest
```

Esperado: **FALHA de compilação** — `PasswordEncoderConfig` não existe.

- [ ] **Step 4: Implementar**

`backend/src/main/java/com/betobanco/config/PasswordEncoderConfig.java`:

```java
package com.betobanco.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * Senhas novas em Argon2id; senhas legadas do Supabase, em bcrypt, continuam
 * validas. O DelegatingPasswordEncoder escolhe o algoritmo pelo prefixo {id}
 * do hash — por isso a migration V3 grava "{bcrypt}$2a$10$..." e nao o hash cru.
 */
@Configuration
public class PasswordEncoderConfig {

    public static final String ID_ATUAL = "argon2";
    public static final String PREFIXO_ATUAL = "{" + ID_ATUAL + "}";

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = Map.of(
                ID_ATUAL, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
                "bcrypt", new BCryptPasswordEncoder());

        DelegatingPasswordEncoder delegating =
                new DelegatingPasswordEncoder(ID_ATUAL, encoders);

        // Hash sem prefixo nao e adivinhado. Este encoder de fallback so
        // participa de matches() e sempre devolve false — recusar de forma
        // limpa e melhor do que supor um algoritmo e comparar contra a
        // suposicao errada. Ele nunca codifica nada.
        delegating.setDefaultPasswordEncoderForMatches(new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                throw new UnsupportedOperationException(
                        "encode sempre usa o algoritmo atual: " + ID_ATUAL);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return false;
            }
        });

        return delegating;
    }
}
```

- [ ] **Step 5: Rodar a suíte e commitar**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test
git add backend/pom.xml backend/src/main/java/com/betobanco/config backend/src/test/java/com/betobanco/config
git commit -m "feat(backend): Argon2id para senhas novas, bcrypt aceito para as legadas"
```

Esperado: **53 testes verdes**.

---

### Task 6: Emissão e validação de JWT

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`, `application-dev.yml`, `application-test.yml`
- Create: `backend/src/main/java/com/betobanco/security/AuthenticatedUser.java`
- Create: `backend/src/main/java/com/betobanco/security/JwtService.java`
- Create: `backend/src/test/java/com/betobanco/security/JwtServiceTest.java`

**Interfaces:**
- Produces:
  - `record AuthenticatedUser(UUID id, String email, Set<String> roles)` com `hasRole(String): boolean`
  - `JwtService.gerar(UUID id, String email, Set<String> roles): String`
  - `JwtService.validar(String token): Optional<AuthenticatedUser>`
  - `JwtService.duracaoSegundos(): long`

- [ ] **Step 1: Adicionar JJWT**

Em `backend/pom.xml`:

```xml
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-api</artifactId>
			<version>0.12.6</version>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-impl</artifactId>
			<version>0.12.6</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-jackson</artifactId>
			<version>0.12.6</version>
			<scope>runtime</scope>
		</dependency>
```

Se `0.12.6` não resolver contra o Maven Central, **não improvise**: descubra a versão estável mais recente da linha `0.12.x`, use-a nas três dependências e registre no relatório qual usou. Não caia para `0.11.x`, cuja API difere da usada aqui.

- [ ] **Step 2: Configurar as propriedades**

Em `backend/src/main/resources/application.yml`, acrescente no nível raiz (irmão de `spring:` e `server:`):

```yaml
betobanco:
  auth:
    jwt-secret: ${JWT_SECRET:}
    access-token-minutes: 15
    refresh-token-days: 30
    first-access-token-hours: 72
    reset-token-hours: 1
    cookie-secure: true
    cookie-same-site: Lax
```

Em `application-dev.yml`, no nível raiz:

```yaml
betobanco:
  auth:
    jwt-secret: ${JWT_SECRET:desenvolvimento-local-troque-isto-em-producao-32b}
    cookie-secure: false
```

Em `application-test.yml`, no nível raiz:

```yaml
betobanco:
  auth:
    jwt-secret: segredo-de-teste-com-mais-de-32-bytes-para-hs256
    cookie-secure: false
```

O perfil `prod` **não** ganha padrão: herda `${JWT_SECRET:}` da raiz, e a Task 8 faz a aplicação recusar-se a subir com o segredo vazio.

- [ ] **Step 3: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/security/JwtServiceTest.java`:

```java
package com.betobanco.security;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes-para-hs256";

    private final JwtService jwt = new JwtService(SEGREDO, 15);

    @Test
    void tokenValidoDevolveOUsuarioComSuasRoles() {
        UUID id = UUID.randomUUID();
        String token = jwt.gerar(id, "aluno@exemplo.com", Set.of("ROLE_STUDENT"));

        AuthenticatedUser usuario = jwt.validar(token).orElseThrow();

        assertThat(usuario.id()).isEqualTo(id);
        assertThat(usuario.email()).isEqualTo("aluno@exemplo.com");
        assertThat(usuario.roles()).containsExactly("ROLE_STUDENT");
        assertThat(usuario.hasRole("ROLE_STUDENT")).isTrue();
        assertThat(usuario.hasRole("ROLE_ADMIN")).isFalse();
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRecusado() {
        String forjado = new JwtService("outro-segredo-totalmente-diferente-com-32b", 15)
                .gerar(UUID.randomUUID(), "invasor@exemplo.com", Set.of("ROLE_ADMIN"));

        assertThat(jwt.validar(forjado)).isEmpty();
    }

    @Test
    void tokenExpiradoEhRecusado() {
        String token = new JwtService(SEGREDO, -1)
                .gerar(UUID.randomUUID(), "aluno@exemplo.com", Set.of("ROLE_STUDENT"));

        assertThat(jwt.validar(token)).isEmpty();
    }

    @Test
    void lixoNaoDerrubaAValidacao() {
        assertThat(jwt.validar("isto-nao-e-um-jwt")).isEmpty();
        assertThat(jwt.validar("")).isEmpty();
        assertThat(jwt.validar(null)).isEmpty();
    }

    @Test
    void doisTokensDoMesmoUsuarioTemIdentificadoresDistintos() {
        UUID id = UUID.randomUUID();

        assertThat(jwt.gerar(id, "a@b.com", Set.of("ROLE_STUDENT")))
                .isNotEqualTo(jwt.gerar(id, "a@b.com", Set.of("ROLE_STUDENT")));
    }

    @Test
    void aDuracaoEhExpostaEmSegundos() {
        assertThat(jwt.duracaoSegundos()).isEqualTo(900L);
    }
}
```

O teste do token forjado é o que mais importa: prova que a validação **confere a assinatura**, e não apenas decodifica o payload. Um validador que só fizesse `parseUnsecured` passaria em todos os outros testes desta classe.

- [ ] **Step 4: Rodar e confirmar que falha**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=JwtServiceTest
```

Esperado: **FALHA de compilação**.

- [ ] **Step 5: Criar `AuthenticatedUser`**

`backend/src/main/java/com/betobanco/security/AuthenticatedUser.java`:

```java
package com.betobanco.security;

import java.util.Set;
import java.util.UUID;

/**
 * Identidade extraida do access token. E este o objeto injetado por
 * @AuthenticationPrincipal nos controllers — a identidade vem sempre do
 * token, nunca do corpo ou da URL da requisicao.
 */
public record AuthenticatedUser(UUID id, String email, Set<String> roles) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
```

- [ ] **Step 6: Criar `JwtService`**

`backend/src/main/java/com/betobanco/security/JwtService.java`:

```java
package com.betobanco.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_EMAIL = "email";

    private final SecretKey chave;
    private final Duration duracao;

    public JwtService(@Value("${betobanco.auth.jwt-secret}") String segredo,
                      @Value("${betobanco.auth.access-token-minutes}") long minutos) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.duracao = Duration.ofMinutes(minutos);
    }

    public String gerar(UUID id, String email, Set<String> roles) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(id.toString())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, List.copyOf(roles))
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(duracao)))
                .signWith(chave)
                .compact();
    }

    public Optional<AuthenticatedUser> validar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            List<?> brutas = claims.get(CLAIM_ROLES, List.class);
            Set<String> roles = brutas == null
                    ? Set.of()
                    : brutas.stream().map(String::valueOf)
                            .collect(Collectors.toCollection(LinkedHashSet::new));

            return Optional.of(new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    roles));
        } catch (Exception e) {
            // Token invalido nao e erro do servidor: nao polui o log em ERROR.
            log.debug("Token recusado: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public long duracaoSegundos() {
        return duracao.toSeconds();
    }
}
```

**Sobre a duração negativa no teste:** `Duration.ofMinutes(-1)` produz um token que já nasce expirado. Não há relógio para adiantar nem `Thread.sleep` para esperar.

- [ ] **Step 7: Rodar a suíte e commitar**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test
git add backend/pom.xml backend/src/main/java/com/betobanco/security backend/src/main/resources backend/src/test/java/com/betobanco/security
git commit -m "feat(backend): emissao e validacao de access token JWT"
```

Esperado: **59 testes verdes**.

---

### Task 7: Refresh token rotativo com detecção de reuso

Esta é a tarefa com mais regra de negócio da fase. Um refresh token roubado é indistinguível do legítimo — a única defesa prática é a rotação: cada uso invalida o token e emite outro. Se o token antigo reaparecer, alguém tem uma cópia, e a resposta certa é derrubar a cadeia inteira.

**Files:**
- Create: `backend/src/main/resources/db/migration/V4__auth_tokens.sql`
- Create: `backend/src/main/java/com/betobanco/auth/entity/{RefreshToken,PasswordResetToken,TokenPurpose}.java`
- Create: `backend/src/main/java/com/betobanco/auth/repository/{RefreshTokenRepository,PasswordResetTokenRepository}.java`
- Create: `backend/src/main/java/com/betobanco/auth/service/RefreshTokenService.java`
- Create: `backend/src/test/java/com/betobanco/auth/RefreshTokenServiceTest.java`

**Interfaces:**
- Consumes: `User`, `UserRepository` (Task 4)
- Produces:
  - `RefreshTokenService.emitir(User): String` — devolve o valor **em claro**, que só existe neste retorno
  - `RefreshTokenService.rotacionar(String valorEmClaro): Optional<Rotacao>` — `Optional.empty()` se inválido, expirado, revogado **ou reusado**
  - `record RefreshTokenService.Rotacao(User usuario, String novoValor)`
  - `RefreshTokenService.revogarTodosDe(UUID userId): void`
  - `RefreshTokenService.revogar(String valorEmClaro): void`
  - `enum TokenPurpose { FIRST_ACCESS, RESET }`

- [ ] **Step 1: Escrever a migration**

`backend/src/main/resources/db/migration/V4__auth_tokens.sql`:

```sql
-- V4: tokens de sessao e de definicao de senha.
--
-- Nenhum valor de token e guardado em claro: so o hash SHA-256. Um vazamento
-- do banco nao pode entregar sessoes ativas.

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL,
    issued_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_tokens (id),
    user_agent  TEXT,
    ip          TEXT,
    CONSTRAINT refresh_tokens_hash_unique UNIQUE (token_hash)
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
CREATE INDEX refresh_tokens_expires_at_idx ON refresh_tokens (expires_at);

-- Primeiro acesso e recuperacao de senha sao o mesmo mecanismo com prazos
-- diferentes. Uma tabela so, distinguida por purpose.
CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL,
    purpose    TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT password_reset_tokens_hash_unique UNIQUE (token_hash),
    CONSTRAINT password_reset_tokens_purpose_check
        CHECK (purpose IN ('FIRST_ACCESS', 'RESET'))
);

CREATE INDEX password_reset_tokens_user_id_idx ON password_reset_tokens (user_id);
```

- [ ] **Step 2: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/auth/RefreshTokenServiceTest.java`:

```java
package com.betobanco.auth;

import com.betobanco.auth.repository.RefreshTokenRepository;
import com.betobanco.auth.service.RefreshTokenService;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenServiceTest extends PostgresTestBase {

    @Autowired
    private RefreshTokenService tokens;

    @Autowired
    private RefreshTokenRepository repo;

    @Autowired
    private UserRepository users;

    private User novoUsuario(String email) {
        return users.saveAndFlush(new User(email, "{bcrypt}xxx", "Fulano"));
    }

    @Test
    void oValorEmClaroNaoEhGuardadoNoBanco() {
        User u = novoUsuario("claro@exemplo.com");

        String valor = tokens.emitir(u);

        assertThat(valor).isNotBlank();
        assertThat(repo.findAll()).hasSize(1);
        assertThat(repo.findAll().getFirst().getTokenHash()).isNotEqualTo(valor);
    }

    @Test
    void rotacionarDevolveNovoValorEInvalidaOAnterior() {
        User u = novoUsuario("rotaciona@exemplo.com");
        String primeiro = tokens.emitir(u);

        RefreshTokenService.Rotacao r = tokens.rotacionar(primeiro).orElseThrow();

        assertThat(r.usuario().getId()).isEqualTo(u.getId());
        assertThat(r.novoValor()).isNotEqualTo(primeiro);
        // O token usado nao serve mais.
        assertThat(tokens.rotacionar(primeiro)).isEmpty();
    }

    @Test
    void reusarUmTokenJaRotacionadoDerrubaACadeiaInteira() {
        User u = novoUsuario("roubado@exemplo.com");
        String t1 = tokens.emitir(u);
        String t2 = tokens.rotacionar(t1).orElseThrow().novoValor();
        String t3 = tokens.rotacionar(t2).orElseThrow().novoValor();

        // t1 reaparece: alguem tem uma copia.
        assertThat(tokens.rotacionar(t1)).isEmpty();

        // A cadeia inteira cai junto, inclusive o token que ainda era valido.
        assertThat(tokens.rotacionar(t3)).isEmpty();
    }

    @Test
    void tokenDesconhecidoEhRecusadoSemExplodir() {
        assertThat(tokens.rotacionar("valor-que-nunca-existiu")).isEmpty();
        assertThat(tokens.rotacionar("")).isEmpty();
        assertThat(tokens.rotacionar(null)).isEmpty();
    }

    @Test
    void tokenDeUsuarioBloqueadoEhRecusado() {
        User u = novoUsuario("bloqueado@exemplo.com");
        String valor = tokens.emitir(u);
        u.setStatus(User.BLOCKED);
        users.saveAndFlush(u);

        assertThat(tokens.rotacionar(valor)).isEmpty();
    }

    @Test
    void revogarTodosEncerraAsSessoesDoUsuario() {
        User u = novoUsuario("sai@exemplo.com");
        String a = tokens.emitir(u);
        String b = tokens.emitir(u);

        tokens.revogarTodosDe(u.getId());

        assertThat(tokens.rotacionar(a)).isEmpty();
        assertThat(tokens.rotacionar(b)).isEmpty();
    }

    @Test
    void revogarUmNaoAfetaOutraSessaoDoMesmoUsuario() {
        User u = novoUsuario("duasabas@exemplo.com");
        String aba1 = tokens.emitir(u);
        String aba2 = tokens.emitir(u);

        tokens.revogar(aba1);

        assertThat(tokens.rotacionar(aba1)).isEmpty();
        assertThat(tokens.rotacionar(aba2)).isPresent();
    }

    @Test
    void doisUsuariosNaoInterferemEntreSi() {
        User a = novoUsuario("a@exemplo.com");
        User b = novoUsuario("b@exemplo.com");
        String tokenA = tokens.emitir(a);
        String tokenB = tokens.emitir(b);

        tokens.revogarTodosDe(a.getId());

        assertThat(tokens.rotacionar(tokenA)).isEmpty();
        Optional<RefreshTokenService.Rotacao> rb = tokens.rotacionar(tokenB);
        assertThat(rb).isPresent();
        assertThat(rb.orElseThrow().usuario().getId()).isEqualTo(b.getId());
    }
}
```

O terceiro teste é o coração da tarefa. Sem a detecção de reuso, um atacante que copiasse `t1` continuaria com acesso indefinido enquanto o usuário legítimo rotaciona normalmente — e ninguém perceberia.

- [ ] **Step 3: Rodar e confirmar que falha**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=RefreshTokenServiceTest
```

Esperado: **FALHA de compilação**.

- [ ] **Step 4: Criar as entidades**

`backend/src/main/java/com/betobanco/auth/entity/TokenPurpose.java`:

```java
package com.betobanco.auth.entity;

public enum TokenPurpose {
    FIRST_ACCESS,
    RESET
}
```

`backend/src/main/java/com/betobanco/auth/entity/RefreshToken.java`:

```java
package com.betobanco.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "issued_at", insertable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "user_agent")
    private String userAgent;

    private String ip;

    protected RefreshToken() {
    }

    public RefreshToken(UUID userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void revogar() {
        if (this.revokedAt == null) {
            this.revokedAt = Instant.now();
        }
    }

    public UUID getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(UUID replacedBy) {
        this.replacedBy = replacedBy;
    }

    /** Ja foi usado para gerar outro token: reaparecer aqui e sinal de copia. */
    public boolean foiRotacionado() {
        return replacedBy != null;
    }

    public boolean estaVigente() {
        return revokedAt == null && Instant.now().isBefore(expiresAt);
    }
}
```

`backend/src/main/java/com/betobanco/auth/entity/PasswordResetToken.java`:

```java
package com.betobanco.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenPurpose purpose;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(UUID userId, String tokenHash, TokenPurpose purpose, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public TokenPurpose getPurpose() {
        return purpose;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void marcarUsado() {
        this.usedAt = Instant.now();
    }

    public boolean estaVigente() {
        return usedAt == null && Instant.now().isBefore(expiresAt);
    }
}
```

- [ ] **Step 5: Criar os repositórios**

`backend/src/main/java/com/betobanco/auth/repository/RefreshTokenRepository.java`:

```java
package com.betobanco.auth.repository;

import com.betobanco.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :agora "
            + "WHERE t.userId = :userId AND t.revokedAt IS NULL")
    int revogarVigentesDe(@Param("userId") UUID userId, @Param("agora") Instant agora);
}
```

`backend/src/main/java/com/betobanco/auth/repository/PasswordResetTokenRepository.java`:

```java
package com.betobanco.auth.repository;

import com.betobanco.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
```

- [ ] **Step 6: Criar o serviço**

`backend/src/main/java/com/betobanco/auth/service/RefreshTokenService.java`:

```java
package com.betobanco.auth.service;

import com.betobanco.auth.entity.RefreshToken;
import com.betobanco.auth.repository.RefreshTokenRepository;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int BYTES = 32; // 256 bits

    private final RefreshTokenRepository repo;
    private final UserRepository users;
    private final Duration validade;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repo,
                               UserRepository users,
                               @Value("${betobanco.auth.refresh-token-days}") long dias) {
        this.repo = repo;
        this.users = users;
        this.validade = Duration.ofDays(dias);
    }

    public record Rotacao(User usuario, String novoValor) {
    }

    @Transactional
    public String emitir(User usuario) {
        String valor = gerarValor();
        repo.saveAndFlush(new RefreshToken(
                usuario.getId(), hash(valor), Instant.now().plus(validade)));
        return valor;
    }

    /**
     * Troca um refresh token por outro. Devolve vazio quando o token e
     * desconhecido, expirado, revogado, do usuario bloqueado — ou quando ja
     * foi rotacionado antes, caso em que a cadeia inteira e derrubada.
     */
    @Transactional
    public Optional<Rotacao> rotacionar(String valorEmClaro) {
        if (valorEmClaro == null || valorEmClaro.isBlank()) {
            return Optional.empty();
        }

        Optional<RefreshToken> encontrado = repo.findByTokenHash(hash(valorEmClaro));
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken atual = encontrado.get();

        // Reuso: este token ja gerou um sucessor. Como o valor em claro so
        // deveria existir no cliente legitimo, sua reaparicao significa copia.
        // Derruba tudo do usuario, inclusive a sessao que ainda funcionava.
        if (atual.foiRotacionado()) {
            log.warn("Reuso de refresh token detectado para o usuario {}; revogando a cadeia",
                    atual.getUserId());
            repo.revogarVigentesDe(atual.getUserId(), Instant.now());
            return Optional.empty();
        }

        if (!atual.estaVigente()) {
            return Optional.empty();
        }

        Optional<User> dono = users.findById(atual.getUserId()).filter(User::isActive);
        if (dono.isEmpty()) {
            return Optional.empty();
        }

        String novoValor = gerarValor();
        RefreshToken sucessor = repo.saveAndFlush(new RefreshToken(
                atual.getUserId(), hash(novoValor), Instant.now().plus(validade)));

        atual.revogar();
        atual.setReplacedBy(sucessor.getId());
        repo.saveAndFlush(atual);

        return Optional.of(new Rotacao(dono.get(), novoValor));
    }

    @Transactional
    public void revogar(String valorEmClaro) {
        if (valorEmClaro == null || valorEmClaro.isBlank()) {
            return;
        }
        repo.findByTokenHash(hash(valorEmClaro)).ifPresent(t -> {
            t.revogar();
            repo.saveAndFlush(t);
        });
    }

    @Transactional
    public void revogarTodosDe(UUID userId) {
        repo.revogarVigentesDe(userId, Instant.now());
    }

    private String gerarValor() {
        byte[] bytes = new byte[BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 basta aqui: o valor tem 256 bits de entropia vinda de
     * SecureRandom, entao nao ha o que quebrar por forca bruta ou dicionario —
     * ao contrario de uma senha escolhida por humano, que exige Argon2.
     */
    private String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
```

- [ ] **Step 7: Rodar a suíte e commitar**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test
git add backend/src/main/resources/db/migration backend/src/main/java/com/betobanco/auth backend/src/test/java/com/betobanco/auth
git commit -m "feat(backend): refresh token rotativo com deteccao de reuso"
```

Esperado: **67 testes verdes**.

---

### Task 8: Cadeia de segurança e envelope em 401/403

Fecha o carry-over registrado na Fase 1: hoje uma requisição não autenticada recebe o formato padrão do Spring Boot, diferente do envelope que o resto da API usa.

**Files:**
- Create: `backend/src/main/java/com/betobanco/security/JwtAuthFilter.java`
- Create: `backend/src/main/java/com/betobanco/security/EnvelopeAuthenticationEntryPoint.java`
- Create: `backend/src/main/java/com/betobanco/security/EnvelopeAccessDeniedHandler.java`
- Create: `backend/src/main/java/com/betobanco/security/SecurityConfig.java`
- Create: `backend/src/test/java/com/betobanco/security/SecurityConfigTest.java`

**Interfaces:**
- Consumes: `JwtService`, `AuthenticatedUser` (Task 6); `ErrorCode`, `ErrorPayload`, `ApiResponse`, `TraceIdFilter` (Fase 1)
- Produces: cadeia de filtros configurada. A partir daqui, controllers recebem a identidade por `@AuthenticationPrincipal AuthenticatedUser`.

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/security/SecurityConfigTest.java`:

```java
package com.betobanco.security;

import com.betobanco.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityConfigTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwt;

    @Test
    void rotaProtegidaSemTokenDevolve401NoEnvelopePadrao() throws Exception {
        mockMvc.perform(get("/students/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.status").value(401))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void alunoNaoAcessaRotaAdministrativaERecebe403NoEnvelope() throws Exception {
        String token = jwt.gerar(UUID.randomUUID(), "aluno@exemplo.com", Set.of("ROLE_STUDENT"));

        mockMvc.perform(get("/admin/students").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.status").value(403));
    }

    @Test
    void tokenInvalidoNaoAutentica() throws Exception {
        mockMvc.perform(get("/students/me").header("Authorization", "Bearer lixo.nao.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void rotasPublicasSeguemAbertas() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=SecurityConfigTest
```

Esperado: **FALHA** — hoje a autoconfiguração padrão devolve HTTP Basic, então `/students/me` responde `401` mas **sem envelope**, e `/actuator/health` também exige credencial.

- [ ] **Step 3: Criar o filtro de autenticação**

`backend/src/main/java/com/betobanco/security/JwtAuthFilter.java`:

```java
package com.betobanco.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIXO)) {
            jwt.validar(header.substring(PREFIXO.length())).ifPresent(usuario -> {
                List<SimpleGrantedAuthority> autoridades = usuario.roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                // O principal e o proprio AuthenticatedUser: e ele que chega
                // aos controllers por @AuthenticationPrincipal.
                var auth = new UsernamePasswordAuthenticationToken(usuario, null, autoridades);
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
```

**O filtro nunca rejeita.** Se o token for inválido, ele apenas não autentica e segue — quem decide o que fazer com uma requisição anônima é a cadeia de autorização, e a resposta sai pelo `EnvelopeAuthenticationEntryPoint`. Um filtro que respondesse `401` sozinho produziria resposta fora do envelope.

- [ ] **Step 4: Criar os dois manipuladores de envelope**

`backend/src/main/java/com/betobanco/security/EnvelopeAuthenticationEntryPoint.java`:

```java
package com.betobanco.security;

import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.ErrorPayload;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.shared.trace.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** Faz o 401 vindo da cadeia de filtros sair no mesmo envelope do resto da API. */
@Component
public class EnvelopeAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    public EnvelopeAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        ErrorPayload payload = new ErrorPayload(
                ErrorCode.UNAUTHORIZED.name(), "Não autenticado",
                ErrorCode.UNAUTHORIZED.httpStatus(), request.getRequestURI(),
                MDC.get(TraceIdFilter.MDC_KEY), Instant.now().toString(), List.of());

        response.setStatus(ErrorCode.UNAUTHORIZED.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(payload));
    }
}
```

`backend/src/main/java/com/betobanco/security/EnvelopeAccessDeniedHandler.java`:

```java
package com.betobanco.security;

import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.ErrorPayload;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.shared.trace.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** Faz o 403 vindo da cadeia de filtros sair no mesmo envelope do resto da API. */
@Component
public class EnvelopeAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper;

    public EnvelopeAccessDeniedHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        ErrorPayload payload = new ErrorPayload(
                ErrorCode.FORBIDDEN.name(), "Acesso negado",
                ErrorCode.FORBIDDEN.httpStatus(), request.getRequestURI(),
                MDC.get(TraceIdFilter.MDC_KEY), Instant.now().toString(), List.of());

        response.setStatus(ErrorCode.FORBIDDEN.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(payload));
    }
}
```

- [ ] **Step 5: Criar a configuração**

`backend/src/main/java/com/betobanco/security/SecurityConfig.java`:

```java
package com.betobanco.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final EnvelopeAuthenticationEntryPoint entryPoint;
    private final EnvelopeAccessDeniedHandler accessDeniedHandler;
    private final String origensPermitidas;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          EnvelopeAuthenticationEntryPoint entryPoint,
                          EnvelopeAccessDeniedHandler accessDeniedHandler,
                          @Value("${betobanco.auth.jwt-secret}") String jwtSecret,
                          @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}") String origens) {
        // Falha rapido: subir sem segredo produziria tokens que qualquer um
        // consegue forjar, e o sintoma so apareceria em producao.
        if (jwtSecret == null || jwtSecret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET ausente ou com menos de 32 bytes; HS256 exige pelo menos isso.");
        }
        this.jwtAuthFilter = jwtAuthFilter;
        this.entryPoint = entryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.origensPermitidas = origens;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ATENCAO: os matchers sao relativos ao context-path
                        // /api/v1, que o servlet remove antes de chegar aqui.
                        // Escrever "/api/v1/auth/**" nao casa com nada.
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/webhooks/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                            .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origensPermitidas.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("X-Trace-Id"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

**Duas armadilhas que este código evita.** A primeira: `hasRole("ADMIN")` compara contra `ROLE_ADMIN`, porque o Spring acrescenta o prefixo sozinho — escrever `hasRole("ROLE_ADMIN")` procuraria por `ROLE_ROLE_ADMIN` e nunca casaria. A segunda está no comentário: os matchers ignoram o context-path.

CSRF fica desabilitado porque a API é stateless e autentica por header `Authorization`. Isso muda na Task 10, se o refresh token passar a viajar em cookie.

- [ ] **Step 6: Rodar a suíte e commitar**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test
git add backend/src/main/java/com/betobanco/security backend/src/test/java/com/betobanco/security
git commit -m "feat(backend): cadeia de seguranca com JWT e envelope em 401/403"
```

Esperado: **71 testes verdes**. O `SecurityConfigTest` referencia `/students/me` e `/admin/students`, que ainda não existem — ambos devem devolver `401`/`403` **antes** de chegar a qualquer controller, que é justamente o que o teste prova.

---

### Task 9: Login e `/auth/me`

**Files:**
- Create: `backend/src/main/java/com/betobanco/auth/dto/{LoginRequest,TokenResponse,MeResponse}.java`
- Create: `backend/src/main/java/com/betobanco/auth/service/AuthService.java`
- Create: `backend/src/main/java/com/betobanco/auth/controller/AuthController.java`
- Create: `backend/src/test/java/com/betobanco/auth/AuthLoginTest.java`

**Interfaces:**
- Consumes: `UserRepository`, `PasswordEncoder`, `JwtService`, `RefreshTokenService`, `PasswordEncoderConfig.PREFIXO_ATUAL`
- Produces:
  - `AuthService.autenticar(String email, String senha): TokenResponse` — lança `BusinessException(UNAUTHORIZED, ...)` quando falha
  - `record TokenResponse(String accessToken, String refreshToken, long expiresIn, String tokenType)`
  - `record MeResponse(UUID id, String email, String fullName, Set<String> roles)`
  - `POST /auth/login`, `GET /auth/me`

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/auth/AuthLoginTest.java`:

```java
package com.betobanco.auth;

import com.betobanco.config.PasswordEncoderConfig;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthLoginTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    private User criar(String email, String hash, String role) {
        Role r = roles.findByName(role).orElseThrow();
        User u = new User(email, hash, "Fulano");
        u.getRoles().add(r);
        return users.saveAndFlush(u);
    }

    private String corpo(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + senha + "\"}";
    }

    @Test
    void loginValidoDevolveAccessTokenERefreshToken() throws Exception {
        criar("ok@exemplo.com", "{bcrypt}" + new BCryptPasswordEncoder().encode("senha123"),
                "ROLE_STUDENT");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("ok@exemplo.com", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void oHashLegadoEhPromovidoParaArgon2NoPrimeiroLogin() throws Exception {
        User u = criar("legado@exemplo.com",
                "{bcrypt}" + new BCryptPasswordEncoder().encode("senha123"), "ROLE_STUDENT");
        assertThat(u.getPasswordHash()).startsWith("{bcrypt}");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("legado@exemplo.com", "senha123")))
                .andExpect(status().isOk());

        User depois = users.findByEmailIgnoreCase("legado@exemplo.com").orElseThrow();
        assertThat(depois.getPasswordHash()).startsWith(PasswordEncoderConfig.PREFIXO_ATUAL);

        // E a senha continua funcionando depois da promocao.
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("legado@exemplo.com", "senha123")))
                .andExpect(status().isOk());
    }

    @Test
    void senhaErradaDevolve401NoEnvelope() throws Exception {
        criar("errada@exemplo.com", "{bcrypt}" + new BCryptPasswordEncoder().encode("certa"),
                "ROLE_STUDENT");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("errada@exemplo.com", "outra")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void emailInexistenteDaAMesmaRespostaDeSenhaErrada() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("ninguem@exemplo.com", "qualquer")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("Credenciais inválidas"));
    }

    @Test
    void usuarioBloqueadoNaoEntra() throws Exception {
        User u = criar("bloqueado@exemplo.com",
                "{bcrypt}" + new BCryptPasswordEncoder().encode("senha123"), "ROLE_STUDENT");
        u.setStatus(User.BLOCKED);
        users.saveAndFlush(u);

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("bloqueado@exemplo.com", "senha123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioSemSenhaDefinidaNaoEntra() throws Exception {
        criar("semsenha@exemplo.com", null, "ROLE_STUDENT");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("semsenha@exemplo.com", "qualquer")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailComCaixaDiferenteEntra() throws Exception {
        criar("caixa@exemplo.com", "{bcrypt}" + new BCryptPasswordEncoder().encode("senha123"),
                "ROLE_STUDENT");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("CAIXA@Exemplo.COM", "senha123")))
                .andExpect(status().isOk());
    }

    @Test
    void corpoInvalidoDevolve422() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void meDevolveOUsuarioDoTokenSemVazarOHash() throws Exception {
        criar("eu@exemplo.com", "{bcrypt}" + new BCryptPasswordEncoder().encode("senha123"),
                "ROLE_STUDENT");

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("eu@exemplo.com", "senha123")))
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(
                login.getResponse().getContentAsString(), "$.data.accessToken");

        String corpoMe = mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("eu@exemplo.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_STUDENT"))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpoMe).doesNotContain("passwordHash", "bcrypt", "argon2");
    }

    @Test
    void meSemTokenDevolve401() throws Exception {
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }
}
```

O teste do e-mail inexistente é de segurança, não de correção: resposta e mensagem idênticas às de senha errada impedem que o endpoint funcione como enumerador de clientes.

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=AuthLoginTest
```

Esperado: **FALHA de compilação**.

- [ ] **Step 3: Criar os DTOs**

`backend/src/main/java/com/betobanco/auth/dto/LoginRequest.java`:

```java
package com.betobanco.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Informe a senha")
        String password) {
}
```

`backend/src/main/java/com/betobanco/auth/dto/TokenResponse.java`:

```java
package com.betobanco.auth.dto;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn, String tokenType) {

    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, expiresIn, "Bearer");
    }
}
```

`backend/src/main/java/com/betobanco/auth/dto/MeResponse.java`:

```java
package com.betobanco.auth.dto;

import java.util.Set;
import java.util.UUID;

public record MeResponse(UUID id, String email, String fullName, Set<String> roles) {
}
```

- [ ] **Step 4: Criar o serviço**

`backend/src/main/java/com/betobanco/auth/service/AuthService.java`:

```java
package com.betobanco.auth.service;

import com.betobanco.auth.dto.TokenResponse;
import com.betobanco.config.PasswordEncoderConfig;
import com.betobanco.security.JwtService;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Mensagem unica: distinguir "email nao existe" de "senha errada"
     *  transformaria o endpoint num enumerador de clientes. */
    private static final String CREDENCIAIS_INVALIDAS = "Credenciais inválidas";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;

    public AuthService(UserRepository users, PasswordEncoder encoder,
                       JwtService jwt, RefreshTokenService refreshTokens) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public TokenResponse autenticar(String email, String senha) {
        Optional<User> encontrado = users.findByEmailIgnoreCase(email.trim());

        if (encontrado.isEmpty() || encontrado.get().getPasswordHash() == null
                || !encoder.matches(senha, encontrado.get().getPasswordHash())
                || !encontrado.get().isActive()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, CREDENCIAIS_INVALIDAS);
        }

        User usuario = encontrado.get();
        promoverHashSeNecessario(usuario, senha);
        return emitirPar(usuario);
    }

    /** Emite o par de tokens para um usuario ja autenticado. */
    @Transactional
    public TokenResponse emitirPar(User usuario) {
        String access = jwt.gerar(usuario.getId(), usuario.getEmail(), nomesDasRoles(usuario));
        String refresh = refreshTokens.emitir(usuario);
        return TokenResponse.bearer(access, refresh, jwt.duracaoSegundos());
    }

    public Set<String> nomesDasRoles(User usuario) {
        return usuario.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    }

    /**
     * Migracao silenciosa: no primeiro login bem-sucedido de um hash legado, a
     * senha e regravada em Argon2id. A base migra sozinha, um aluno por vez,
     * sem que ninguem precise redefinir nada.
     */
    private void promoverHashSeNecessario(User usuario, String senha) {
        if (!usuario.getPasswordHash().startsWith(PasswordEncoderConfig.PREFIXO_ATUAL)) {
            usuario.setPasswordHash(encoder.encode(senha));
            users.saveAndFlush(usuario);
            log.info("Hash de senha promovido para {} no usuario {}",
                    PasswordEncoderConfig.ID_ATUAL, usuario.getId());
        }
    }
}
```

- [ ] **Step 5: Criar o controller**

`backend/src/main/java/com/betobanco/auth/controller/AuthController.java`:

```java
package com.betobanco.auth.controller;

import com.betobanco.auth.dto.LoginRequest;
import com.betobanco.auth.dto.MeResponse;
import com.betobanco.auth.dto.TokenResponse;
import com.betobanco.auth.service.AuthService;
import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService auth;
    private final UserRepository users;

    public AuthController(AuthService auth, UserRepository users) {
        this.auth = auth;
        this.users = users;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(auth.autenticar(req.email(), req.password())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        // A identidade vem do token, nunca do cliente.
        User usuario = users.findById(atual.id())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        return ResponseEntity.ok(ApiResponse.ok(new MeResponse(
                usuario.getId(), usuario.getEmail(), usuario.getFullName(),
                auth.nomesDasRoles(usuario))));
    }
}
```

- [ ] **Step 6: Rodar a suíte e commitar**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test
git add backend/src/main/java/com/betobanco/auth backend/src/test/java/com/betobanco/auth
git commit -m "feat(backend): login com promocao de hash legado e endpoint /auth/me"
```

Esperado: **81 testes verdes**.

---

### Task 10: Refresh e logout

**Files:**
- Create: `backend/src/main/java/com/betobanco/auth/dto/RefreshRequest.java`
- Modify: `backend/src/main/java/com/betobanco/auth/controller/AuthController.java`
- Create: `backend/src/test/java/com/betobanco/auth/AuthRefreshTest.java`

**Interfaces:**
- Consumes: `RefreshTokenService.rotacionar`, `RefreshTokenService.revogar`, `AuthService.emitirPar`
- Produces: `POST /auth/refresh`, `POST /auth/logout`

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/auth/AuthRefreshTest.java`:

```java
package com.betobanco.auth;

import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthRefreshTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    private void criar(String email) {
        Role r = roles.findByName("ROLE_STUDENT").orElseThrow();
        User u = new User(email, "{bcrypt}" + new BCryptPasswordEncoder().encode("senha123"), "F");
        u.getRoles().add(r);
        users.saveAndFlush(u);
    }

    private String refreshDoLogin(String email) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.data.refreshToken");
    }

    private String json(String refresh) {
        return "{\"refreshToken\":\"" + refresh + "\"}";
    }

    @Test
    void refreshValidoDevolveNovoParEInvalidaOAnterior() throws Exception {
        criar("refresh@exemplo.com");
        String primeiro = refreshDoLogin("refresh@exemplo.com");

        String corpo = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(json(primeiro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String novo = JsonPath.read(corpo, "$.data.refreshToken");
        assertThat(novo).isNotEqualTo(primeiro);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(json(primeiro)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reusarUmRefreshJaRotacionadoDerrubaTodasAsSessoes() throws Exception {
        criar("roubo@exemplo.com");
        String t1 = refreshDoLogin("roubo@exemplo.com");
        String t2 = JsonPath.read(mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(json(t1)))
                .andReturn().getResponse().getContentAsString(), "$.data.refreshToken");

        // t1 reaparece: sinal de copia.
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(json(t1)))
                .andExpect(status().isUnauthorized());

        // t2 era legitimo e cai junto: e o preco de conter um roubo.
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(json(t2)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshDesconhecidoDevolve401NoEnvelope() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(json("nunca-existiu")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void logoutEncerraApenasASessaoInformada() throws Exception {
        criar("logout@exemplo.com");
        String aba1 = refreshDoLogin("logout@exemplo.com");
        String aba2 = refreshDoLogin("logout@exemplo.com");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON).content(json(aba1)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(json(aba1)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(json(aba2)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutComTokenDesconhecidoNaoVazaSeEleExistia() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON).content(json("qualquer-coisa")))
                .andExpect(status().isNoContent());
    }
}
```

O último teste protege uma sutileza: `logout` responde `204` mesmo para token inexistente. Responder `404` diria ao atacante quais tokens existem.

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=AuthRefreshTest
```

Esperado: **FALHA** com `404` nas rotas, que ainda não existem.

- [ ] **Step 3: Criar o DTO**

`backend/src/main/java/com/betobanco/auth/dto/RefreshRequest.java`:

```java
package com.betobanco.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Informe o refresh token")
        String refreshToken) {
}
```

- [ ] **Step 4: Acrescentar os endpoints ao `AuthController`**

Adicione os imports:

```java
import com.betobanco.auth.dto.RefreshRequest;
import com.betobanco.auth.service.RefreshTokenService;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
```

Acrescente o campo e ajuste o construtor para receber `RefreshTokenService refreshTokens`, guardando-o em `this.refreshTokens`. Depois acrescente os dois métodos:

```java
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest req) {
        RefreshTokenService.Rotacao rotacao = refreshTokens.rotacionar(req.refreshToken())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED, "Sessão inválida ou expirada"));

        String access = auth.nomesDasRoles(rotacao.usuario()).isEmpty()
                ? null
                : null; // placeholder removido abaixo
        return ResponseEntity.ok(ApiResponse.ok(
                auth.emitirParComRefreshExistente(rotacao.usuario(), rotacao.novoValor())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        // Sempre 204: responder 404 revelaria quais tokens existem.
        refreshTokens.revogar(req.refreshToken());
        return ResponseEntity.noContent().build();
    }
```

**Remova as três linhas do `String access = ...` acima** — elas não fazem nada e só existem para deixar claro que o access token **não** é gerado aqui: quem gera é o `AuthService`, reaproveitando o refresh que a rotação já emitiu. Em `AuthService`, acrescente:

```java
    /**
     * Emite um novo access token para um refresh que ACABOU de ser rotacionado.
     * Diferente de emitirPar, nao cria outro refresh: isso geraria dois tokens
     * vigentes por rotacao e a cadeia deixaria de ser uma cadeia.
     */
    public TokenResponse emitirParComRefreshExistente(User usuario, String refreshJaEmitido) {
        String access = jwt.gerar(usuario.getId(), usuario.getEmail(), nomesDasRoles(usuario));
        return TokenResponse.bearer(access, refreshJaEmitido, jwt.duracaoSegundos());
    }
```

- [ ] **Step 5: Rodar a suíte e commitar**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test
git add backend/src/main/java/com/betobanco/auth backend/src/test/java/com/betobanco/auth
git commit -m "feat(backend): refresh rotativo e logout por sessao"
```

Esperado: **86 testes verdes**.

---

### Task 11: Cadastro, esqueci a senha e definição de senha

Primeiro acesso e recuperação compartilham endpoint e tabela, distinguidos por `purpose` — como a spec decidiu.

**Files:**
- Create: `backend/src/main/java/com/betobanco/auth/dto/{RegisterRequest,ForgotPasswordRequest,ResetPasswordRequest}.java`
- Create: `backend/src/main/java/com/betobanco/auth/service/PasswordResetService.java`
- Modify: `backend/src/main/java/com/betobanco/auth/controller/AuthController.java`
- Create: `backend/src/test/java/com/betobanco/auth/AuthRegistrationTest.java`

**Interfaces:**
- Produces:
  - `PasswordResetService.criarToken(User, TokenPurpose): String` — valor em claro, só neste retorno
  - `PasswordResetService.redefinir(String token, String novaSenha): void` — lança `BusinessException`
  - `POST /auth/register`, `POST /auth/forgot-password`, `POST /auth/reset-password`

- [ ] **Step 1: Escrever o teste que falha**

`backend/src/test/java/com/betobanco/auth/AuthRegistrationTest.java`:

```java
package com.betobanco.auth;

import com.betobanco.auth.entity.TokenPurpose;
import com.betobanco.auth.service.PasswordResetService;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.StudentRepository;
import com.betobanco.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthRegistrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private StudentRepository students;

    @Autowired
    private PasswordResetService resets;

    private String registro(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"senha-forte-123\","
                + "\"fullName\":\"Novo Aluno\"}";
    }

    @Test
    void registroCriaUsuarioAlunoSemNenhumEntitlement() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("novo@exemplo.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("novo@exemplo.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_STUDENT"));

        User criado = users.findByEmailIgnoreCase("novo@exemplo.com").orElseThrow();
        // Cadastro publico e captura de lead: cria a conta, nao da acesso a
        // conteudo. Quem da acesso e o entitlement, na Fase 3.
        assertThat(students.findById(criado.getId())).isPresent();
    }

    @Test
    void emailJaCadastradoDevolve409() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registro("repetido@exemplo.com"))).andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("REPETIDO@Exemplo.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void senhaCurtaDevolve422() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"curta@exemplo.com\",\"password\":\"123\","
                                + "\"fullName\":\"Alguem\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("password"));
    }

    @Test
    void forgotPasswordRespondeIgualParaEmailQueExisteOuNao() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registro("existe@exemplo.com"))).andExpect(status().isCreated());

        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"existe@exemplo.com\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao-existe@exemplo.com\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void tokenDePrimeiroAcessoDefineASenhaEPermiteLogin() throws Exception {
        Role r = roles.findByName("ROLE_STUDENT").orElseThrow();
        User u = new User("primeiro@exemplo.com", null, "Primeiro Acesso");
        u.getRoles().add(r);
        users.saveAndFlush(u);

        String token = resets.criarToken(u, TokenPurpose.FIRST_ACCESS);

        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"minha-senha-123\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"primeiro@exemplo.com\","
                                + "\"password\":\"minha-senha-123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void tokenDeDefinicaoDeSenhaNaoServeDuasVezes() throws Exception {
        Role r = roles.findByName("ROLE_STUDENT").orElseThrow();
        User u = users.saveAndFlush(new User("umavez@exemplo.com", null, "Uma Vez"));
        u.getRoles().add(r);
        users.saveAndFlush(u);

        String token = resets.criarToken(u, TokenPurpose.RESET);

        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"password\":\"primeira-vez-123\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"password\":\"segunda-vez-123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLIENT_ERROR"));
    }

    @Test
    void tokenDesconhecidoNaoRedefineNada() throws Exception {
        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"nunca-existiu\",\"password\":\"qualquer-123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redefinirSenhaEncerraAsSessoesAbertas() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registro("sessoes@exemplo.com"))).andExpect(status().isCreated());

        String refresh = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"sessoes@exemplo.com\","
                                        + "\"password\":\"senha-forte-123\"}"))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.refreshToken");

        User u = users.findByEmailIgnoreCase("sessoes@exemplo.com").orElseThrow();
        String token = resets.criarToken(u, TokenPurpose.RESET);

        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"password\":\"nova-senha-123\"}"))
                .andExpect(status().isNoContent());

        // Quem redefine a senha normalmente esta reagindo a um acesso indevido:
        // manter sessoes antigas vivas anularia o proposito.
        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=AuthRegistrationTest
```

Esperado: **FALHA de compilação**.

- [ ] **Step 3: Criar os DTOs**

`backend/src/main/java/com/betobanco/auth/dto/RegisterRequest.java`:

```java
package com.betobanco.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Informe a senha")
        @Size(min = 8, message = "A senha precisa ter ao menos 8 caracteres")
        String password,

        @NotBlank(message = "Informe o nome completo")
        String fullName) {
}
```

`backend/src/main/java/com/betobanco/auth/dto/ForgotPasswordRequest.java`:

```java
package com.betobanco.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        String email) {
}
```

`backend/src/main/java/com/betobanco/auth/dto/ResetPasswordRequest.java`:

```java
package com.betobanco.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Token ausente")
        String token,

        @NotBlank(message = "Informe a senha")
        @Size(min = 8, message = "A senha precisa ter ao menos 8 caracteres")
        String password) {
}
```

- [ ] **Step 4: Criar o `PasswordResetService`**

`backend/src/main/java/com/betobanco/auth/service/PasswordResetService.java`:

```java
package com.betobanco.auth.service;

import com.betobanco.auth.entity.PasswordResetToken;
import com.betobanco.auth.entity.TokenPurpose;
import com.betobanco.auth.repository.PasswordResetTokenRepository;
import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    private static final int BYTES = 32;

    private final PasswordResetTokenRepository repo;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final RefreshTokenService refreshTokens;
    private final Duration validadePrimeiroAcesso;
    private final Duration validadeReset;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(PasswordResetTokenRepository repo,
                                UserRepository users,
                                PasswordEncoder encoder,
                                RefreshTokenService refreshTokens,
                                @Value("${betobanco.auth.first-access-token-hours}") long horasPrimeiro,
                                @Value("${betobanco.auth.reset-token-hours}") long horasReset) {
        this.repo = repo;
        this.users = users;
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
        this.validadePrimeiroAcesso = Duration.ofHours(horasPrimeiro);
        this.validadeReset = Duration.ofHours(horasReset);
    }

    @Transactional
    public String criarToken(User usuario, TokenPurpose purpose) {
        Duration validade = purpose == TokenPurpose.FIRST_ACCESS
                ? validadePrimeiroAcesso
                : validadeReset;

        byte[] bytes = new byte[BYTES];
        random.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repo.saveAndFlush(new PasswordResetToken(
                usuario.getId(), hash(valor), purpose, Instant.now().plus(validade)));

        return valor;
    }

    @Transactional
    public void redefinir(String valorEmClaro, String novaSenha) {
        PasswordResetToken token = repo.findByTokenHash(hash(valorEmClaro))
                .filter(PasswordResetToken::estaVigente)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CLIENT_ERROR, "Link inválido ou expirado"));

        User usuario = users.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CLIENT_ERROR, "Link inválido ou expirado"));

        usuario.setPasswordHash(encoder.encode(novaSenha));
        users.saveAndFlush(usuario);

        token.marcarUsado();
        repo.saveAndFlush(token);

        // Quem redefine a senha normalmente esta reagindo a um acesso indevido.
        // Deixar sessoes antigas vivas anularia o proposito da redefinicao.
        refreshTokens.revogarTodosDe(usuario.getId());
    }

    private String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
```

- [ ] **Step 5: Criar o registro no `UserService`**

`backend/src/main/java/com/betobanco/users/service/UserService.java`:

```java
package com.betobanco.users.service;

import com.betobanco.shared.exception.BusinessException;
import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.users.entity.Role;
import com.betobanco.users.entity.Student;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.RoleRepository;
import com.betobanco.users.repository.StudentRepository;
import com.betobanco.users.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final StudentRepository students;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, RoleRepository roles,
                       StudentRepository students, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.students = students;
        this.encoder = encoder;
    }

    /**
     * Cadastro publico: cria a conta como aluno, SEM nenhum entitlement. Quem
     * libera conteudo e o entitlement, concedido pelo pagamento na Fase 3.
     */
    @Transactional
    public User registrar(String email, String senha, String nomeCompleto) {
        String normalizado = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalizado)) {
            throw new BusinessException(ErrorCode.CONFLICT, "E-mail já cadastrado");
        }

        Role aluno = roles.findByName("ROLE_STUDENT")
                .orElseThrow(() -> new IllegalStateException("ROLE_STUDENT ausente"));

        User usuario = new User(normalizado, encoder.encode(senha), nomeCompleto.trim());
        usuario.getRoles().add(aluno);
        users.saveAndFlush(usuario);

        students.saveAndFlush(new Student(usuario.getId()));
        return usuario;
    }
}
```

- [ ] **Step 6: Acrescentar os três endpoints ao `AuthController`**

Imports adicionais:

```java
import com.betobanco.auth.dto.ForgotPasswordRequest;
import com.betobanco.auth.dto.RegisterRequest;
import com.betobanco.auth.dto.ResetPasswordRequest;
import com.betobanco.auth.entity.TokenPurpose;
import com.betobanco.auth.service.PasswordResetService;
import com.betobanco.users.service.UserService;
import org.springframework.http.HttpStatus;
```

Acrescente `UserService userService` e `PasswordResetService resets` ao construtor, e os métodos:

```java
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MeResponse>> register(
            @Valid @RequestBody RegisterRequest req) {
        User criado = userService.registrar(req.email(), req.password(), req.fullName());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(new MeResponse(
                criado.getId(), criado.getEmail(), criado.getFullName(),
                auth.nomesDasRoles(criado))));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        // Resposta identica exista o e-mail ou nao: qualquer diferenca
        // transformaria o endpoint num enumerador de clientes.
        users.findByEmailIgnoreCase(req.email().trim())
                .ifPresent(u -> resets.criarToken(u, TokenPurpose.RESET));

        // O envio do e-mail entra na Fase 3, junto com a outbox.
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        resets.redefinir(req.token(), req.password());
        return ResponseEntity.noContent().build();
    }
```

- [ ] **Step 7: Rodar a suíte e commitar**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test
git add backend/src/main/java/com/betobanco backend/src/test/java/com/betobanco
git commit -m "feat(backend): cadastro, recuperacao e definicao de senha"
```

Esperado: **94 testes verdes**.

---

### Task 12: Rate limiting, perfil do aluno e a regra de identidade

Fecha a fase: protege os endpoints de autenticação contra força bruta, entrega o primeiro recurso do aluno autenticado, e estende a regra ArchUnit para cobrir `@AuthenticationPrincipal`.

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/betobanco/security/RateLimitFilter.java`
- Create: `backend/src/main/java/com/betobanco/users/dto/{StudentResponse,StudentUpdateRequest}.java`
- Create: `backend/src/main/java/com/betobanco/users/controller/StudentController.java`
- Create: `backend/src/test/java/com/betobanco/security/RateLimitFilterTest.java`
- Create: `backend/src/test/java/com/betobanco/users/StudentControllerTest.java`

**Interfaces:**
- Produces: `GET /students/me`, `PUT /students/me`; filtro de rate limit em `/auth/login`, `/auth/forgot-password` e `/auth/reset-password`

- [ ] **Step 1: Adicionar Bucket4j**

Em `backend/pom.xml`:

```xml
		<dependency>
			<groupId>com.bucket4j</groupId>
			<artifactId>bucket4j_jdk17-core</artifactId>
			<version>8.14.0</version>
		</dependency>
```

Se esse `artifactId` ou versão não resolver, **não improvise**: a biblioteca renomeou artefatos entre a linha 8.0 e a 8.10. Descubra o artefato correto publicado para Java 17+, use-o, e registre no relatório qual usou. Não substitua por implementação própria sem registrar o motivo.

- [ ] **Step 2: Escrever os testes que falham**

`backend/src/test/java/com/betobanco/security/RateLimitFilterTest.java`:

```java
package com.betobanco.security;

import com.betobanco.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RateLimitFilterTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    private static final String CORPO =
            "{\"email\":\"forca@bruta.com\",\"password\":\"tentativa\"}";

    @Test
    void tentativasDemaisDeLoginDevolvem429NoEnvelope() throws Exception {
        for (int i = 0; i < RateLimitFilter.LIMITE; i++) {
            mockMvc.perform(post("/auth/login").with(r -> { r.setRemoteAddr("10.0.0.1"); return r; })
                    .contentType(MediaType.APPLICATION_JSON).content(CORPO));
        }

        mockMvc.perform(post("/auth/login").with(r -> { r.setRemoteAddr("10.0.0.1"); return r; })
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.error.status").value(429));
    }

    @Test
    void outroIpNaoEhAfetadoPeloLimiteDoPrimeiro() throws Exception {
        for (int i = 0; i < RateLimitFilter.LIMITE + 2; i++) {
            mockMvc.perform(post("/auth/login").with(r -> { r.setRemoteAddr("10.0.0.2"); return r; })
                    .contentType(MediaType.APPLICATION_JSON).content(CORPO));
        }

        mockMvc.perform(post("/auth/login").with(r -> { r.setRemoteAddr("10.0.0.3"); return r; })
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotasQueNaoSaoDeAutenticacaoNaoSaoLimitadas() throws Exception {
        for (int i = 0; i < RateLimitFilter.LIMITE + 5; i++) {
            mockMvc.perform(get("/actuator/health")
                    .with(r -> { r.setRemoteAddr("10.0.0.4"); return r; }));
        }

        mockMvc.perform(get("/actuator/health").with(r -> { r.setRemoteAddr("10.0.0.4"); return r; }))
                .andExpect(status().isOk());
    }
}
```

`backend/src/test/java/com/betobanco/users/StudentControllerTest.java`:

```java
package com.betobanco.users;

import com.betobanco.security.JwtService;
import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.entity.Student;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.StudentRepository;
import com.betobanco.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class StudentControllerTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private StudentRepository students;

    @Autowired
    private JwtService jwt;

    private String tokenDeUmAlunoNovo(String email) {
        User u = users.saveAndFlush(new User(email, "{bcrypt}x", "Aluno"));
        students.saveAndFlush(new Student(u.getId()));
        return jwt.gerar(u.getId(), u.getEmail(), Set.of("ROLE_STUDENT"));
    }

    @Test
    void meDevolveOPerfilDoProprioAluno() throws Exception {
        String token = tokenDeUmAlunoNovo("perfil@exemplo.com");

        mockMvc.perform(get("/students/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("perfil@exemplo.com"))
                .andExpect(jsonPath("$.data.fullName").value("Aluno"));
    }

    @Test
    void atualizaNomeETelefoneDoProprioAluno() throws Exception {
        String token = tokenDeUmAlunoNovo("atualiza@exemplo.com");

        mockMvc.perform(put("/students/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Nome Novo\",\"phone\":\"11999998888\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Nome Novo"))
                .andExpect(jsonPath("$.data.phone").value("11999998888"));
    }

    @Test
    void umTokenNaoAlcancaOPerfilDeOutroAluno() throws Exception {
        tokenDeUmAlunoNovo("vitima@exemplo.com");
        String tokenDoOutro = tokenDeUmAlunoNovo("outro@exemplo.com");

        // Nao existe rota que aceite id de aluno: /students/me e o unico
        // caminho, e o id vem do token. Este teste documenta a ausencia.
        mockMvc.perform(get("/students/me").header("Authorization", "Bearer " + tokenDoOutro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("outro@exemplo.com"));
    }

    @Test
    void tokenDeUsuarioInexistenteDevolve404() throws Exception {
        String orfao = jwt.gerar(UUID.randomUUID(), "fantasma@exemplo.com", Set.of("ROLE_STUDENT"));

        mockMvc.perform(get("/students/me").header("Authorization", "Bearer " + orfao))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void semTokenDevolve401() throws Exception {
        mockMvc.perform(get("/students/me")).andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 3: Rodar e confirmar que falham**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=RateLimitFilterTest+StudentControllerTest
```

Esperado: **FALHA de compilação**.

- [ ] **Step 4: Criar o filtro de rate limit**

`backend/src/main/java/com/betobanco/security/RateLimitFilter.java`:

```java
package com.betobanco.security;

import com.betobanco.shared.exception.ErrorCode;
import com.betobanco.shared.exception.ErrorPayload;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.shared.trace.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita tentativas nos endpoints de autenticacao, por IP.
 *
 * LIMITACAO CONHECIDA: o contador vive em memoria, entao o limite efetivo
 * multiplica pelo numero de instancias. Uma implantacao replicada exige mover
 * isto para Redis. Registrado na secao 6.6 da spec.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    public static final int LIMITE = 10;
    private static final Duration JANELA = Duration.ofMinutes(1);

    private static final Set<String> PROTEGIDOS =
            Set.of("/auth/login", "/auth/forgot-password", "/auth/reset-password");

    private final Map<String, Bucket> baldes = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public RateLimitFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!PROTEGIDOS.contains(request.getServletPath())) {
            chain.doFilter(request, response);
            return;
        }

        Bucket balde = baldes.computeIfAbsent(
                request.getServletPath() + "|" + request.getRemoteAddr(),
                k -> Bucket.builder()
                        .addLimit(Bandwidth.builder().capacity(LIMITE)
                                .refillGreedy(LIMITE, JANELA).build())
                        .build());

        if (balde.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }

        ErrorPayload payload = new ErrorPayload(
                ErrorCode.RATE_LIMIT_EXCEEDED.name(), "Limite de requisições excedido",
                ErrorCode.RATE_LIMIT_EXCEEDED.httpStatus(), request.getRequestURI(),
                MDC.get(TraceIdFilter.MDC_KEY), Instant.now().toString(), List.of());

        response.setStatus(ErrorCode.RATE_LIMIT_EXCEEDED.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(payload));
    }
}
```

Se a API do Bucket4j na versão que você resolveu diferir de `Bandwidth.builder()...refillGreedy(...)`, adapte para a forma equivalente daquela versão e registre no relatório. O comportamento exigido é: `LIMITE` requisições por janela de um minuto, por IP e por rota.

- [ ] **Step 5: Criar os DTOs e o controller do aluno**

`backend/src/main/java/com/betobanco/users/dto/StudentResponse.java`:

```java
package com.betobanco.users.dto;

import java.util.UUID;

public record StudentResponse(UUID id, String email, String fullName, String phone) {
}
```

`backend/src/main/java/com/betobanco/users/dto/StudentUpdateRequest.java`:

```java
package com.betobanco.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentUpdateRequest(
        @NotBlank(message = "Informe o nome completo")
        @Size(max = 120, message = "Nome muito longo")
        String fullName,

        @Pattern(regexp = "^$|^[0-9]{10,13}$",
                message = "Telefone deve conter de 10 a 13 dígitos")
        String phone) {
}
```

`backend/src/main/java/com/betobanco/users/controller/StudentController.java`:

```java
package com.betobanco.users.controller;

import com.betobanco.security.AuthenticatedUser;
import com.betobanco.shared.exception.NotFoundException;
import com.betobanco.shared.response.ApiResponse;
import com.betobanco.users.dto.StudentResponse;
import com.betobanco.users.dto.StudentUpdateRequest;
import com.betobanco.users.entity.Student;
import com.betobanco.users.entity.User;
import com.betobanco.users.repository.StudentRepository;
import com.betobanco.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
@Tag(name = "Students")
public class StudentController {

    private final UserRepository users;
    private final StudentRepository students;

    public StudentController(UserRepository users, StudentRepository students) {
        this.users = users;
        this.students = students;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<StudentResponse>> me(
            @AuthenticationPrincipal AuthenticatedUser atual) {
        return ResponseEntity.ok(ApiResponse.ok(montar(atual)));
    }

    @PutMapping("/me")
    @Transactional
    public ResponseEntity<ApiResponse<StudentResponse>> atualizar(
            @AuthenticationPrincipal AuthenticatedUser atual,
            @Valid @RequestBody StudentUpdateRequest req) {

        User usuario = usuarioDe(atual);
        usuario.setFullName(req.fullName().trim());
        users.saveAndFlush(usuario);

        Student perfil = students.findById(atual.id())
                .orElseGet(() -> new Student(atual.id()));
        perfil.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone());
        students.saveAndFlush(perfil);

        return ResponseEntity.ok(ApiResponse.ok(new StudentResponse(
                usuario.getId(), usuario.getEmail(), usuario.getFullName(), perfil.getPhone())));
    }

    private User usuarioDe(AuthenticatedUser atual) {
        // Sempre pelo id do token. Nao existe rota que aceite id de aluno.
        return users.findById(atual.id())
                .orElseThrow(() -> new NotFoundException("Aluno não encontrado"));
    }

    private StudentResponse montar(AuthenticatedUser atual) {
        User usuario = usuarioDe(atual);
        String telefone = students.findById(atual.id()).map(Student::getPhone).orElse(null);
        return new StudentResponse(
                usuario.getId(), usuario.getEmail(), usuario.getFullName(), telefone);
    }
}
```

- [ ] **Step 6: Provar que a regra ArchUnit continua valendo**

A regra `nenhumControllerAceitaUserIdVindoDoCliente` foi escrita na Fase 1 e nunca viu um controller de verdade. Agora existem dois. Rode:

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -q test -Dtest=ModuleBoundariesTest
```

Esperado: **três verdes**. Agora crie temporariamente, em `StudentController`, um método que viole a regra:

```java
    @GetMapping("/violacao/{userId}")
    public ResponseEntity<ApiResponse<StudentResponse>> violacao(
            @org.springframework.web.bind.annotation.PathVariable java.util.UUID userId) {
        return null;
    }
```

Rode de novo e **confirme que `nenhumControllerAceitaUserIdVindoDoCliente` falha**. Depois **apague o método** e confirme o verde. Cole as duas saídas no relatório — é a primeira vez que a regra é exercitada contra um controller real do projeto.

- [ ] **Step 7: Rodar a suíte e commitar**

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw -B verify
git add backend/pom.xml backend/src/main/java/com/betobanco backend/src/test/java/com/betobanco
git commit -m "feat(backend): rate limit em autenticacao e perfil do aluno"
```

Esperado: **`BUILD SUCCESS` com 102 testes verdes**.

---

## Critério de conclusão da Fase 2

1. `./mvnw -B verify` termina em `BUILD SUCCESS` com todos os testes verdes
2. A migration V3 é no-op num banco vazio e migra os perfis num banco com o esquema do Supabase — os dois caminhos cobertos por teste
3. `POST /auth/login` com `admin@gmail.com` e a senha atual do Supabase devolve um par de tokens, **sem que ninguém tenha redefinido nada**
4. O hash desse usuário no banco passa de `{bcrypt}` para `{argon2}` depois do primeiro login
5. `GET /students/me` sem token devolve `401` **no envelope padrão**; um aluno em `/admin/**` devolve `403` no mesmo envelope
6. Reusar um refresh token já rotacionado derruba todas as sessões do usuário
7. As três regras ArchUnit passam, e a de `userId` foi vista reprovando um controller real

## Verificação manual contra o banco de produção

Depois que a suíte estiver verde, o teste que realmente importa é este — e ele não é automatizável, porque depende da senha real:

```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" \
  DATABASE_URL="jdbc:postgresql://db.bjnplubfqoltaxfboodl.supabase.co:5432/postgres" \
  DATABASE_USER="postgres" DATABASE_PASSWORD="<senha do banco>" \
  JWT_SECRET="<segredo com 32+ bytes>" \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Com a aplicação de pé, `POST http://localhost:8080/api/v1/auth/login` com `admin@gmail.com` e a senha atual. **Se devolver um token, a migração transparente funcionou.**

## Pendências registradas para a Fase 3

- O envio de e-mail em `forgot-password` e no primeiro acesso ainda não existe: o token é criado e descartado. A outbox de e-mail é da Fase 3, e sem ela nenhum aluno novo consegue definir a senha sozinho.
- Rate limit em memória: o limite efetivo multiplica pelo número de instâncias. Migrar para Redis quando houver replicação.
- `POST /auth/register` cria conta sem entitlement, o que é o correto — mas até a Fase 3 existir, ninguém tem acesso a conteúdo nenhum, porque não há conteúdo.

## Divergência deliberada da spec, registrada

A **seção 6.2 da spec** determina que o refresh token viaje em cookie
`HttpOnly; Secure; SameSite`, para que um XSS não consiga exfiltrá-lo. Este
plano entrega o refresh token **no corpo JSON** da resposta.

A divergência é consciente e tem prazo. O cookie só faz sentido quando existe um
navegador do outro lado, e o frontend é a Fase 4 — decidir agora o `SameSite` e
o domínio significaria decidir sem saber onde a API será servida, que é
exatamente a questão em aberto da seção 6.2. Além disso, o cookie exige um token
anti-CSRF no `/auth/refresh`, e escrever essa proteção antes de existir cliente
produziria código sem consumidor.

**O que isso custa se ficar como está:** um XSS no frontend rouba a sessão longa,
não só os 15 minutos do access token. Por isso a mudança para cookie é
**pré-requisito da Fase 4**, não item opcional — e a máquina que ela precisa
(rotação, detecção de reuso, revogação) já está pronta aqui. O que muda é só o
transporte.
