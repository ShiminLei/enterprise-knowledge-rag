package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.document.DocumentStatus;
import com.aishare.knowledgerag.document.KnowledgeDocument;
import com.aishare.knowledgerag.document.KnowledgeDocumentRepository;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcKnowledgeDocumentRepository implements KnowledgeDocumentRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, tenant_id, external_document_id, title, source, file_name, media_type,
                   category, version, document_updated_at, permission_level, department,
                   status, checksum
            FROM knowledge_document
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcKnowledgeDocumentRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<KnowledgeDocument> findByTenantIdAndChecksum(UUID tenantId, String checksum) {
        return jdbcTemplate.query(
                        SELECT_COLUMNS + " WHERE tenant_id = :tenantId AND checksum = :checksum",
                        new MapSqlParameterSource()
                                .addValue("tenantId", tenantId)
                                .addValue("checksum", checksum),
                        this::mapDocument
                )
                .stream()
                .findFirst();
    }

    @Override
    public Optional<KnowledgeDocument> findByTenantIdAndExternalDocumentIdAndVersion(
            UUID tenantId,
            String externalDocumentId,
            String version
    ) {
        return jdbcTemplate.query(
                        SELECT_COLUMNS + """
                                 WHERE tenant_id = :tenantId
                                   AND external_document_id = :externalDocumentId
                                   AND version = :version
                                """,
                        new MapSqlParameterSource()
                                .addValue("tenantId", tenantId)
                                .addValue("externalDocumentId", externalDocumentId)
                                .addValue("version", version),
                        this::mapDocument
                )
                .stream()
                .findFirst();
    }

    @Override
    public boolean insert(KnowledgeDocument document) {
        int updated = jdbcTemplate.update("""
                        INSERT INTO knowledge_document (
                            id, tenant_id, external_document_id, title, source, file_name,
                            media_type, category, version, document_updated_at,
                            permission_level, department, status, checksum, metadata
                        ) VALUES (
                            :id, :tenantId, :externalDocumentId, :title, :source, :fileName,
                            :mediaType, :category, :version, :documentUpdatedAt,
                            :permissionLevel, :department, :status, :checksum, '{}'::jsonb
                        )
                        ON CONFLICT DO NOTHING
                        """,
                documentParameters(document)
        );
        return updated == 1;
    }

    @Override
    public void updateStatus(UUID documentId, DocumentStatus status) {
        int updated = jdbcTemplate.update("""
                        UPDATE knowledge_document
                        SET status = :status, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :documentId
                        """,
                new MapSqlParameterSource()
                        .addValue("documentId", documentId)
                        .addValue("status", status.name())
        );
        if (updated != 1) {
            throw new IllegalStateException("更新文档状态失败: " + documentId);
        }
    }

    private MapSqlParameterSource documentParameters(KnowledgeDocument document) {
        return new MapSqlParameterSource()
                .addValue("id", document.id())
                .addValue("tenantId", document.tenantId())
                .addValue("externalDocumentId", document.externalDocumentId())
                .addValue("title", document.title())
                .addValue("source", document.source())
                .addValue("fileName", document.fileName())
                .addValue("mediaType", document.mediaType())
                .addValue("category", document.category().name())
                .addValue("version", document.version())
                .addValue("documentUpdatedAt", Timestamp.from(document.updatedAt()))
                .addValue("permissionLevel", document.permissionLevel().name())
                .addValue("department", document.department())
                .addValue("status", document.status().name())
                .addValue("checksum", document.checksum());
    }

    private KnowledgeDocument mapDocument(ResultSet resultSet, int rowNumber) throws SQLException {
        return new KnowledgeDocument(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("tenant_id", UUID.class),
                resultSet.getString("external_document_id"),
                resultSet.getString("title"),
                resultSet.getString("source"),
                resultSet.getString("file_name"),
                resultSet.getString("media_type"),
                DocumentCategory.valueOf(resultSet.getString("category")),
                resultSet.getString("version"),
                resultSet.getTimestamp("document_updated_at").toInstant(),
                PermissionLevel.valueOf(resultSet.getString("permission_level")),
                resultSet.getString("department"),
                DocumentStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("checksum")
        );
    }
}
