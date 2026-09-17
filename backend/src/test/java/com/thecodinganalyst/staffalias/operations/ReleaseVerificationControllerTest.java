package com.thecodinganalyst.staffalias.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ReleaseVerificationControllerTest {

    @Test
    void returnsUpWhenDatabaseReadinessQuerySucceeds() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        ReleaseVerificationController controller = new ReleaseVerificationController(jdbcTemplate);

        assertThat(controller.databaseReadiness()).isEqualTo(Map.of("status", "UP"));
    }

    @Test
    void rejectsNullOrUnexpectedReadinessResults() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ReleaseVerificationController controller = new ReleaseVerificationController(jdbcTemplate);

        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(null);
        assertThatThrownBy(controller::databaseReadiness).isInstanceOf(IllegalStateException.class);

        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(2);
        assertThatThrownBy(controller::databaseReadiness).isInstanceOf(IllegalStateException.class);
    }
}
