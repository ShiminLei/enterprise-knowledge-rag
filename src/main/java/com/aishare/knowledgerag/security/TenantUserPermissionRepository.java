package com.aishare.knowledgerag.security;

import java.util.List;
import java.util.UUID;

public interface TenantUserPermissionRepository {

    List<TenantUserPermission> findByTenantIdAndUserId(UUID tenantId, String userId);
}
