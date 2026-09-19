package com.thecodinganalyst.staffalias;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void applicationUsesPostgresAndRunsAllFlywayMigrationsFromCleanDatabase() {
        String databaseProduct = jdbcTemplate.queryForObject("select version()", String.class);
        List<String> successfulVersions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true and version is not null order by installed_rank",
                String.class);
        Integer failedMigrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = false", Integer.class);

        assertThat(databaseProduct).containsIgnoringCase("PostgreSQL");
        assertThat(successfulVersions).containsExactly("1", "2", "3", "4", "5");
        assertThat(failedMigrationCount).isZero();

        assertThat(tableExists("tenant")).isTrue();
        assertThat(tableExists("person")).isTrue();
        assertThat(tableExists("employment")).isTrue();
        assertThat(tableExists("employment_identifier")).isTrue();
        assertThat(tableExists("application_user")).isTrue();
        assertThat(tableExists("account_activation_token")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists (select 1 from information_schema.tables where table_schema = 'public' and table_name = ?)",
                Boolean.class, tableName);
        return Boolean.TRUE.equals(exists);
    }
}
