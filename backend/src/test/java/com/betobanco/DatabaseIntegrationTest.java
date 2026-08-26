package com.betobanco;

import com.betobanco.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseIntegrationTest extends PostgresTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void bancoEhPostgres17() {
        String versao = jdbc.queryForObject("SHOW server_version", String.class);
        assertThat(versao).startsWith("17.");
    }

    @Test
    void flywayExecutouAMigrationBaseline() {
        Integer aplicadas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(aplicadas).isGreaterThanOrEqualTo(1);
    }
}
