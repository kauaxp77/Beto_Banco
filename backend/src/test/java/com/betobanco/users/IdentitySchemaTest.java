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
