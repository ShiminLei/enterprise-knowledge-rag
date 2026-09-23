package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.retrieval.KeywordSearchRepository;
import com.aishare.knowledgerag.retrieval.Bm25Scorer;
import com.aishare.knowledgerag.retrieval.RetrievedChunk;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcKeywordSearchRepository implements KeywordSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Bm25Scorer bm25Scorer;

    public JdbcKeywordSearchRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            Bm25Scorer bm25Scorer
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.bm25Scorer = bm25Scorer;
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
                       c.start_paragraph_number,
                       c.end_paragraph_number,
                       c.category,
                       c.document_version,
                       c.source
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
                ORDER BY c.document_updated_at DESC, c.chunk_index
                LIMIT 5000
                """.replace("__CATEGORY_FILTER__", categoryFilter);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", query.accessContext().tenantId())
                .addValue("userId", query.accessContext().userId());
        if (query.category() != null) {
            parameters.addValue("category", query.category().name());
        }

        List<RetrievedChunk> corpus = jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new RetrievedChunk(
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
                0
        ));
        return bm25Scorer.rank(
                query.question(), corpus, query.topK(), query.minScore()
        );
    }

}
