package com.aishare.knowledgerag.security;

import java.util.Set;
import java.util.UUID;

public record AccessContext(
        UUID tenantId,
        String userId,
        Set<String> departments,
        PermissionLevel permissionLevel
) {
}
