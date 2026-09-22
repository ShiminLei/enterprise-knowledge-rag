package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.conversation.Conversation;
import com.aishare.knowledgerag.conversation.ConversationRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcConversationRepository implements ConversationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcConversationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Conversation> findOwned(UUID conversationId, UUID tenantId, String userId) {
        return jdbcTemplate.query("""
                        SELECT id, tenant_id, user_id, title, created_at
                        FROM conversation
                        WHERE id = :id AND tenant_id = :tenantId AND user_id = :userId
                        """,
                new MapSqlParameterSource()
                        .addValue("id", conversationId)
                        .addValue("tenantId", tenantId)
                        .addValue("userId", userId),
                (resultSet, rowNumber) -> new Conversation(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("tenant_id", UUID.class),
                        resultSet.getString("user_id"),
                        resultSet.getString("title"),
                        resultSet.getTimestamp("created_at").toInstant()
                )
        ).stream().findFirst();
    }

    @Override
    public void insert(Conversation conversation) {
        jdbcTemplate.update("""
                        INSERT INTO conversation (
                            id, tenant_id, user_id, title, created_at, updated_at
                        ) VALUES (:id, :tenantId, :userId, :title, :createdAt, :createdAt)
                        """,
                new MapSqlParameterSource()
                        .addValue("id", conversation.id())
                        .addValue("tenantId", conversation.tenantId())
                        .addValue("userId", conversation.userId())
                        .addValue("title", conversation.title())
                        .addValue("createdAt", Timestamp.from(conversation.createdAt()))
        );
    }

    @Override
    public void touch(UUID conversationId) {
        jdbcTemplate.update("""
                        UPDATE conversation SET updated_at = CURRENT_TIMESTAMP WHERE id = :id
                        """,
                new MapSqlParameterSource("id", conversationId)
        );
    }
}
