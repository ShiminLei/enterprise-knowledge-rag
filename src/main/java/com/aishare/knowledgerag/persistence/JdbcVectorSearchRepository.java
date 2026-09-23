package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.retrieval.RetrievedChunk;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import com.aishare.knowledgerag.retrieval.VectorSearchRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcVectorSearchRepository implements VectorSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcVectorSearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<RetrievedChunk> search(VectorSearchQuery query, float[] queryEmbedding) {
        String categoryFilter = query.category() == null ? "" : "AND c.category = :category";
        String sql = """
                SELECT c.id,
                       c.document_id,
                       c.chunk_index,
                       c.content,
                       c.title_path,
                       c.page_number,
                       c.start_paragraph_number,
                       c.end_paragraph_number,
                       c.category,
                       c.document_version,
                       c.source,
                       1 - (c.embedding <=> CAST(:embedding AS vector)) AS score
                FROM knowledge_chunk c
                JOIN knowledge_document d ON d.id = c.document_id
                WHERE c.tenant_id = :tenantId
                  AND d.tenant_id = :tenantId
                  AND d.status = 'ACTIVE'
                  AND c.embedding IS NOT NULL
                  AND (
                      c.permission_level = 'PUBLIC'
                      OR EXISTS (
                          SELECT 1
                          FROM tenant_user_permission permission
                          WHERE permission.tenant_id = c.tenant_id
                            AND permission.user_id = :userId
                            AND permission.department = c.department
                            AND CASE permission.permission_level
                                    WHEN 'PUBLIC' THEN 0
                                    WHEN 'INTERNAL' THEN 10
                                    WHEN 'CONFIDENTIAL' THEN 20
                                    WHEN 'RESTRICTED' THEN 30
                                    ELSE -1
                                END >= CASE c.permission_level
                                    WHEN 'PUBLIC' THEN 0
                                    WHEN 'INTERNAL' THEN 10
                                    WHEN 'CONFIDENTIAL' THEN 20
                                    WHEN 'RESTRICTED' THEN 30
                                    ELSE 999
                                END
                      )
                  )
                  %s
                  AND 1 - (c.embedding <=> CAST(:embedding AS vector)) >= :minScore
                ORDER BY c.embedding <=> CAST(:embedding AS vector)
                LIMIT :topK
                """.formatted(categoryFilter);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", query.accessContext().tenantId())
                .addValue("userId", query.accessContext().userId())
                .addValue("embedding", vectorLiteral(queryEmbedding))
                .addValue("minScore", query.minScore())
                .addValue("topK", query.topK());
        if (query.category() != null) {
            parameters.addValue("category", query.category().name());
        }

        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new RetrievedChunk(
                resultSet.getObject("id", java.util.UUID.class),
                resultSet.getObject("document_id", java.util.UUID.class),
                resultSet.getInt("chunk_index"),
                resultSet.getString("content"),
                resultSet.getString("title_path"),
                resultSet.getObject("page_number", Integer.class),
                resultSet.getObject("start_paragraph_number", Integer.class),
                resultSet.getObject("end_paragraph_number", Integer.class),
                DocumentCategory.valueOf(resultSet.getString("category")),
                resultSet.getString("document_version"),
                resultSet.getString("source"),
                resultSet.getDouble("score")
        ));
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
