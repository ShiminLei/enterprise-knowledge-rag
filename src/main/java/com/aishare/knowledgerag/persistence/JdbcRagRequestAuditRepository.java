package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.audit.RagRequestAudit;
import com.aishare.knowledgerag.audit.RagRequestAuditRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRagRequestAuditRepository implements RagRequestAuditRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcRagRequestAuditRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void insert(RagRequestAudit audit) {
        jdbcTemplate.update("""
                        INSERT INTO rag_request_audit (
                            id, request_id, tenant_id, user_id, conversation_id,
                            question_digest, retrieval_mode, model_name, prompt_version,
                            retrieved_document_ids, outcome, latency_ms, error_code
                        ) VALUES (
                            :id, :requestId, :tenantId, :userId, :conversationId,
                            :questionDigest, :retrievalMode, :modelName, :promptVersion,
                            CAST(:retrievedDocumentIds AS jsonb), :outcome, :latencyMs, :errorCode
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", audit.id())
                        .addValue("requestId", audit.requestId())
                        .addValue("tenantId", audit.tenantId())
                        .addValue("userId", audit.userId())
                        .addValue("conversationId", audit.conversationId())
                        .addValue("questionDigest", audit.questionDigest())
                        .addValue("retrievalMode", audit.retrievalMode())
                        .addValue("modelName", audit.modelName())
                        .addValue("promptVersion", audit.promptVersion())
                        .addValue("retrievedDocumentIds", toJson(audit))
                        .addValue("outcome", audit.outcome())
                        .addValue("latencyMs", audit.latencyMs())
                        .addValue("errorCode", audit.errorCode())
        );
    }

    private String toJson(RagRequestAudit audit) {
        try {
            return objectMapper.writeValueAsString(audit.retrievedDocumentIds());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("审计文档列表序列化失败", exception);
        }
    }
}
