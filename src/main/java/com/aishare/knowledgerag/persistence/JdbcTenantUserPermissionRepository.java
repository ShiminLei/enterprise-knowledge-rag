package com.aishare.knowledgerag.persistence;

import com.aishare.knowledgerag.security.PermissionLevel;
import com.aishare.knowledgerag.security.TenantUserPermission;
import com.aishare.knowledgerag.security.TenantUserPermissionRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JdbcTenantUserPermissionRepository implements TenantUserPermissionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcTenantUserPermissionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TenantUserPermission> findByTenantIdAndUserId(
            UUID tenantId,
            String userId
    ) {
        return jdbcTemplate.query("""
                        SELECT tenant_id, user_id, department, permission_level
                        FROM tenant_user_permission
                        WHERE tenant_id = :tenantId AND user_id = :userId
                        ORDER BY department
                        """,
                new MapSqlParameterSource()
                        .addValue("tenantId", tenantId)
                        .addValue("userId", userId),
                (resultSet, rowNumber) -> new TenantUserPermission(
                        resultSet.getObject("tenant_id", UUID.class),
                        resultSet.getString("user_id"),
                        resultSet.getString("department"),
                        PermissionLevel.valueOf(resultSet.getString("permission_level"))
                )
        );
    }
}
