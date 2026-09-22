package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.conversation.ConversationMessageRepository;
import com.aishare.knowledgerag.conversation.ConversationTurn;
import com.aishare.knowledgerag.conversation.MessageRole;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class JdbcConversationMessageRepository implements ConversationMessageRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcConversationMessageRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ConversationTurn> findRecent(UUID conversationId, int limit) {
        return jdbcTemplate.query("""
                        SELECT role, content
                        FROM (
                            SELECT role, content, sequence_number
                            FROM conversation_message
                            WHERE conversation_id = :conversationId
                            ORDER BY sequence_number DESC
                            LIMIT :limit
                        ) recent
                        ORDER BY sequence_number ASC
                        """,
                new MapSqlParameterSource()
                        .addValue("conversationId", conversationId)
                        .addValue("limit", limit),
                (resultSet, rowNumber) -> new ConversationTurn(
                        MessageRole.valueOf(resultSet.getString("role")),
                        resultSet.getString("content")
                )
        );
    }

    @Override
    public void append(
            UUID conversationId,
            MessageRole role,
            String content,
            Map<String, Object> metadata
    ) {
        jdbcTemplate.update("""
                        INSERT INTO conversation_message (
                            id, conversation_id, role, content, metadata
                        ) VALUES (
                            :id, :conversationId, :role, :content, CAST(:metadata AS jsonb)
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("conversationId", conversationId)
                        .addValue("role", role.name())
                        .addValue("content", content)
                        .addValue("metadata", toJson(metadata))
        );
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JacksonException exception) {
            throw new IllegalStateException("会话消息元数据序列化失败", exception);
        }
    }
}
