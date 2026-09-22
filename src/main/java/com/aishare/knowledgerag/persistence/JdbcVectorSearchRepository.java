package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.retrieval.RetrievedChunk;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import com.aishare.knowledgerag.retrieval.VectorSearchRepository;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class JdbcVectorSearchRepository implements VectorSearchRepository {

    private static final String NO_DEPARTMENT = "__NO_ACCESSIBLE_DEPARTMENT__";

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
                  AND c.permission_level IN (:allowedPermissionLevels)
                  AND (c.permission_level = 'PUBLIC' OR c.department IN (:departments))
                  %s
                  AND 1 - (c.embedding <=> CAST(:embedding AS vector)) >= :minScore
                ORDER BY c.embedding <=> CAST(:embedding AS vector)
                LIMIT :topK
                """.formatted(categoryFilter);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", query.accessContext().tenantId())
                .addValue("embedding", vectorLiteral(queryEmbedding))
                .addValue("allowedPermissionLevels", allowedLevels(query))
                .addValue("departments", accessibleDepartments(query))
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
                DocumentCategory.valueOf(resultSet.getString("category")),
                resultSet.getString("document_version"),
                resultSet.getString("source"),
                resultSet.getDouble("score")
        ));
    }

    private List<String> allowedLevels(VectorSearchQuery query) {
        int callerRank = query.accessContext().permissionLevel().rank();
        return Arrays.stream(PermissionLevel.values())
                .filter(level -> level.rank() <= callerRank)
                .map(Enum::name)
                .toList();
    }

    private List<String> accessibleDepartments(VectorSearchQuery query) {
        if (query.accessContext().departments() == null
                || query.accessContext().departments().isEmpty()) {
            return List.of(NO_DEPARTMENT);
        }
        return List.copyOf(query.accessContext().departments());
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
