package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.evaluation.EvaluationCase;
import com.aishare.knowledgerag.evaluation.EvaluationCaseResult;
import com.aishare.knowledgerag.evaluation.EvaluationRepository;
import com.aishare.knowledgerag.evaluation.EvaluationRunSummary;
import com.aishare.knowledgerag.security.PermissionLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JdbcEvaluationRepository implements EvaluationRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcEvaluationRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<EvaluationCase> findCases() {
        return jdbcTemplate.query("""
                        SELECT id, case_key, question,
                               expected_external_document_ids,
                               should_answer, required_permission_level, tags
                        FROM evaluation_case
                        ORDER BY case_key
                        """,
                (resultSet, rowNumber) -> {
                    String requiredLevel = resultSet.getString("required_permission_level");
                    return new EvaluationCase(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getString("case_key"),
                            resultSet.getString("question"),
                            readStringList(resultSet.getString(
                                    "expected_external_document_ids")),
                            resultSet.getBoolean("should_answer"),
                            requiredLevel == null
                                    ? null
                                    : PermissionLevel.valueOf(requiredLevel),
                            readStringList(resultSet.getString("tags"))
                    );
                }
        );
    }

    @Override
    public Map<String, UUID> resolveDocumentIds(
            UUID tenantId,
            Collection<String> externalDocumentIds
    ) {
        if (externalDocumentIds.isEmpty()) {
            return Map.of();
        }
        return jdbcTemplate.query("""
                        SELECT external_document_id, id
                        FROM knowledge_document
                        WHERE tenant_id = :tenantId
                          AND external_document_id IN (:externalDocumentIds)
                          AND status = 'ACTIVE'
                        """,
                new MapSqlParameterSource()
                        .addValue("tenantId", tenantId)
                        .addValue("externalDocumentIds", externalDocumentIds),
                (resultSet, rowNumber) -> Map.entry(
                        resultSet.getString("external_document_id"),
                        resultSet.getObject("id", UUID.class)
                )
        ).stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                Map.Entry::getValue
        ));
    }

    @Override
    public void startRun(
            UUID runId,
            Map<String, Object> configuration,
            Instant startedAt
    ) {
        jdbcTemplate.update("""
                        INSERT INTO evaluation_run (
                            id, status, configuration, summary, started_at
                        ) VALUES (
                            :id, 'RUNNING', CAST(:configuration AS jsonb),
                            '{}'::jsonb, :startedAt
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", runId)
                        .addValue("configuration", toJson(configuration))
                        .addValue("startedAt", startedAt)
        );
    }

    @Override
    public void saveResult(EvaluationCaseResult result) {
        jdbcTemplate.update("""
                        INSERT INTO evaluation_result (
                            id, run_id, case_id, answer, retrieved_chunks,
                            metrics, latency_ms, error_message
                        ) VALUES (
                            :id, :runId, :caseId, NULL,
                            CAST(:retrievedChunks AS jsonb), CAST(:metrics AS jsonb),
                            :latencyMs, :errorMessage
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", result.id())
                        .addValue("runId", result.runId())
                        .addValue("caseId", result.caseId())
                        .addValue("retrievedChunks", toJson(result.retrievedChunks()))
                        .addValue("metrics", toJson(result.metrics()))
                        .addValue("latencyMs", result.latencyMs())
                        .addValue("errorMessage", result.errorMessage())
        );
    }

    @Override
    public void completeRun(
            UUID runId,
            String status,
            EvaluationRunSummary summary,
            Instant completedAt
    ) {
        jdbcTemplate.update("""
                        UPDATE evaluation_run
                        SET status = :status,
                            summary = CAST(:summary AS jsonb),
                            completed_at = :completedAt
                        WHERE id = :id
                        """,
                new MapSqlParameterSource()
                        .addValue("id", runId)
                        .addValue("status", status)
                        .addValue("summary", toJson(summary))
                        .addValue("completedAt", completedAt)
        );
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("评测用例 JSON 解析失败", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("评测结果 JSON 序列化失败", exception);
        }
    }
}
