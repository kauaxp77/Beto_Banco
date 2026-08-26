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
                // O schema "public" ja tem a tabela profiles antes do Flyway
                // rodar pela primeira vez, exatamente como aconteceria em
                // producao contra o banco legado do Supabase. Sem baseline,
                // o Flyway recusa migrar um schema nao-vazio sem historico.
                .baselineOnMigrate(true)
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
