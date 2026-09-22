package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.retrieval.KeywordSearchRepository;
import com.aishare.knowledgerag.retrieval.RetrievedChunk;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class JdbcKeywordSearchRepository implements KeywordSearchRepository {

    private static final String NO_DEPARTMENT = "__NO_ACCESSIBLE_DEPARTMENT__";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcKeywordSearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<RetrievedChunk> search(VectorSearchQuery query) {
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
                       GREATEST(
                           word_similarity(lower(:question), lower(c.content)),
                           LEAST(1.0, word_similarity(
                               lower(:question),
                               lower(COALESCE(c.title_path, ''))
                           ) * 1.2)
                       ) AS score
                FROM knowledge_chunk c
                JOIN knowledge_document d ON d.id = c.document_id
                WHERE c.tenant_id = :tenantId
                  AND d.tenant_id = :tenantId
                  AND d.status = 'ACTIVE'
                  AND c.permission_level IN (:allowedPermissionLevels)
                  AND (c.permission_level = 'PUBLIC' OR c.department IN (:departments))
                  __CATEGORY_FILTER__
                  AND (
                      lower(:question) <% lower(c.content)
                      OR lower(:question) <% lower(COALESCE(c.title_path, ''))
                      OR lower(c.content) LIKE '%' || lower(:question) || '%'
                  )
                ORDER BY score DESC, c.document_updated_at DESC, c.chunk_index
                LIMIT :topK
                """.replace("__CATEGORY_FILTER__", categoryFilter);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("question", query.question())
                .addValue("tenantId", query.accessContext().tenantId())
                .addValue("allowedPermissionLevels", allowedLevels(query))
                .addValue("departments", accessibleDepartments(query))
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
}
