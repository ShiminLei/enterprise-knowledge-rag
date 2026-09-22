package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.document.KnowledgeChunk;
import com.aishare.knowledgerag.document.KnowledgeChunkRepository;
import com.aishare.knowledgerag.embedding.EmbeddedChunk;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class JdbcKnowledgeChunkRepository implements KnowledgeChunkRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcKnowledgeChunkRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertAll(List<EmbeddedChunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }
        SqlParameterSource[] batch = chunks.stream()
                .map(this::parameters)
                .toArray(SqlParameterSource[]::new);
        int[] counts = jdbcTemplate.batchUpdate("""
                INSERT INTO knowledge_chunk (
                    id, document_id, tenant_id, chunk_index, content, title_path,
                    page_number, start_paragraph_number, end_paragraph_number,
                    category, document_version, document_updated_at,
                    permission_level, department, source, metadata, embedding
                ) VALUES (
                    :id, :documentId, :tenantId, :chunkIndex, :content, :titlePath,
                    :pageNumber, :startParagraphNumber, :endParagraphNumber,
                    :category, :documentVersion, :documentUpdatedAt,
                    :permissionLevel, :department, :source, '{}'::jsonb,
                    CAST(:embedding AS vector)
                )
                """, batch);
        if (counts.length != chunks.size()) {
            throw new IllegalStateException("Chunk 批量写入数量不一致");
        }
    }

    private MapSqlParameterSource parameters(EmbeddedChunk embeddedChunk) {
        KnowledgeChunk chunk = embeddedChunk.chunk();
        return new MapSqlParameterSource()
                .addValue("id", chunk.id())
                .addValue("documentId", chunk.documentId())
                .addValue("tenantId", chunk.tenantId())
                .addValue("chunkIndex", chunk.chunkIndex())
                .addValue("content", chunk.content())
                .addValue("titlePath", chunk.titlePath())
                .addValue("pageNumber", chunk.pageNumber())
                .addValue("startParagraphNumber", chunk.startParagraphNumber())
                .addValue("endParagraphNumber", chunk.endParagraphNumber())
                .addValue("category", chunk.category().name())
                .addValue("documentVersion", chunk.documentVersion())
                .addValue("documentUpdatedAt", Timestamp.from(chunk.documentUpdatedAt()))
                .addValue("permissionLevel", chunk.permissionLevel().name())
                .addValue("department", chunk.department())
                .addValue("source", chunk.source())
                .addValue("embedding", vectorLiteral(embeddedChunk.embedding()));
    }

    private String vectorLiteral(float[] embedding) {
        StringBuilder value = new StringBuilder(embedding.length * 10).append('[');
        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) {
                value.append(',');
            }
            value.append(Float.toString(embedding[index]));
        }
        return value.append(']').toString();
    }
}
