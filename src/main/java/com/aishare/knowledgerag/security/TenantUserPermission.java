package com.aishare.knowledgerag.security;

import java.util.UUID;

public record TenantUserPermission(
        UUID tenantId,
        String userId,
        String department,
        PermissionLevel permissionLevel
) {
}
