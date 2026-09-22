package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.retrieval.RetrievedChunk;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcKeywordSearchRepositoryTest {

    @Test
    @SuppressWarnings("unchecked")
    void buildsChineseTrigramQueryWithAccessFilters() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.query(
                anyString(),
                any(SqlParameterSource.class),
                any(RowMapper.class)
        )).thenReturn(List.<RetrievedChunk>of());
        JdbcKeywordSearchRepository repository = new JdbcKeywordSearchRepository(jdbcTemplate);

        repository.search(new VectorSearchQuery(
                "VPN 错误码 720",
                new AccessContext(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "zhangsan",
                        Set.of("信息技术部"),
                        PermissionLevel.INTERNAL
                ),
                null,
                20,
                0.35
        ));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(
                sql.capture(),
                any(SqlParameterSource.class),
                any(RowMapper.class)
        );
        assertThat(sql.getValue())
                .contains("<% lower(c.content)")
                .contains("d.status = 'ACTIVE'")
                .contains("FROM tenant_user_permission permission")
                .contains("permission.department = c.department")
                .contains("permission.user_id = :userId");
    }
}
