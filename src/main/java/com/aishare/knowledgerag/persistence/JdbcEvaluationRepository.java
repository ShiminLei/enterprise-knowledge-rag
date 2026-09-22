package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.evaluation.EvaluationCase;
import com.aishare.knowledgerag.evaluation.EvaluationCaseResult;
import com.aishare.knowledgerag.evaluation.EvaluationRepository;
import com.aishare.knowledgerag.evaluation.EvaluationRunSummary;
import com.aishare.knowledgerag.evaluation.EvaluationRunRecord;
import com.aishare.knowledgerag.evaluation.EvaluationRetrievedChunk;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JdbcEvaluationRepository implements EvaluationRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> OBJECT_MAP =
            new TypeReference<>() {
            };
    private static final TypeReference<List<EvaluationRetrievedChunk>> RETRIEVED_CHUNKS =
            new TypeReference<>() {
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
                               expected_external_document_ids, expected_keywords,
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
                            readStringList(resultSet.getString("expected_keywords")),
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
                            :id, :runId, :caseId, :answer,
                            CAST(:retrievedChunks AS jsonb), CAST(:metrics AS jsonb),
                            :latencyMs, :errorMessage
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", result.id())
                        .addValue("runId", result.runId())
                        .addValue("caseId", result.caseId())
                        .addValue("answer", result.answer())
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

    @Override
    public List<EvaluationRunRecord> findRuns(UUID tenantId, int limit) {
        return jdbcTemplate.query("""
                        SELECT id, status, configuration, summary,
                               started_at, completed_at
                        FROM evaluation_run
                        WHERE configuration ->> 'tenantId' = :tenantId
                        ORDER BY started_at DESC
                        LIMIT :limit
                        """,
                new MapSqlParameterSource()
                        .addValue("tenantId", tenantId.toString())
                        .addValue("limit", limit),
                this::mapRun
        );
    }

    @Override
    public Optional<EvaluationRunRecord> findRun(UUID tenantId, UUID runId) {
        return jdbcTemplate.query("""
                        SELECT id, status, configuration, summary,
                               started_at, completed_at
                        FROM evaluation_run
                        WHERE id = :runId
                          AND configuration ->> 'tenantId' = :tenantId
                        """,
                new MapSqlParameterSource()
                        .addValue("runId", runId)
                        .addValue("tenantId", tenantId.toString()),
                this::mapRun
        ).stream().findFirst();
    }

    @Override
    public List<EvaluationCaseResult> findResults(UUID runId) {
        return jdbcTemplate.query("""
                        SELECT result.id, result.run_id, result.case_id,
                               evaluation_case.case_key, evaluation_case.question,
                               result.answer, result.retrieved_chunks, result.metrics,
                               result.latency_ms, result.error_message
                        FROM evaluation_result result
                        JOIN evaluation_case ON evaluation_case.id = result.case_id
                        WHERE result.run_id = :runId
                        ORDER BY evaluation_case.case_key
                        """,
                new MapSqlParameterSource("runId", runId),
                (resultSet, rowNumber) -> {
                    Map<String, Object> metrics = readMap(
                            resultSet.getString("metrics"));
                    return new EvaluationCaseResult(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getObject("run_id", UUID.class),
                            resultSet.getObject("case_id", UUID.class),
                            resultSet.getString("case_key"),
                            resultSet.getString("question"),
                            resultSet.getString("answer"),
                            Boolean.TRUE.equals(metrics.get("passed")),
                            readChunks(resultSet.getString("retrieved_chunks")),
                            metrics,
                            resultSet.getLong("latency_ms"),
                            resultSet.getString("error_message")
                    );
                }
        );
    }

    private EvaluationRunRecord mapRun(
            java.sql.ResultSet resultSet,
            int rowNumber
    ) throws java.sql.SQLException {
        java.sql.Timestamp completedAt = resultSet.getTimestamp("completed_at");
        return new EvaluationRunRecord(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("status"),
                readMap(resultSet.getString("configuration")),
                readSummary(resultSet.getString("summary")),
                resultSet.getTimestamp("started_at").toInstant(),
                completedAt == null ? null : completedAt.toInstant()
        );
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("评测用例 JSON 解析失败", exception);
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("评测 JSON 解析失败", exception);
        }
    }

    private List<EvaluationRetrievedChunk> readChunks(String json) {
        try {
            return objectMapper.readValue(json, RETRIEVED_CHUNKS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("评测召回结果解析失败", exception);
        }
    }

    private EvaluationRunSummary readSummary(String json) {
        try {
            if (json == null || json.isBlank() || json.equals("{}")) {
                return new EvaluationRunSummary(0, 0, 0, 0, 0, 0, 0);
            }
            return objectMapper.readValue(json, EvaluationRunSummary.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("评测汇总解析失败", exception);
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
