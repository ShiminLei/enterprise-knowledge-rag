package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.retrieval.KeywordSearchRepository;
import com.aishare.knowledgerag.retrieval.RetrievedChunk;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcKeywordSearchRepository implements KeywordSearchRepository {

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
                .addValue("userId", query.accessContext().userId())
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

}
