package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.evaluation.EvaluationRunSummary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JdbcEvaluationRepositoryTest {

    @Test
    void bindsRunTimestampsAsJdbcTimestamps() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcEvaluationRepository repository = new JdbcEvaluationRepository(
                jdbcTemplate,
                JsonMapper.builder().findAndAddModules().build()
        );
        UUID runId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-09-23T01:00:00Z");
        Instant completedAt = Instant.parse("2026-09-23T01:01:00Z");
        ArgumentCaptor<SqlParameterSource> parameters =
                ArgumentCaptor.forClass(SqlParameterSource.class);

        repository.startRun(runId, Map.of("type", "RETRIEVAL"), startedAt);
        repository.completeRun(
                runId,
                "COMPLETED",
                new EvaluationRunSummary(1, 1, 1, 1, 1, 1, 1),
                completedAt
        );

        verify(jdbcTemplate, org.mockito.Mockito.times(2))
                .update(anyString(), parameters.capture());
        assertThat(parameters.getAllValues().get(0).getValue("startedAt"))
                .isEqualTo(Timestamp.from(startedAt));
        assertThat(parameters.getAllValues().get(1).getValue("completedAt"))
                .isEqualTo(Timestamp.from(completedAt));
    }
}
