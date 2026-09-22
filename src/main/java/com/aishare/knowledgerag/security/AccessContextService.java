package com.aishare.knowledgerag.security;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccessContextService {

    private final TenantUserPermissionRepository permissionRepository;

    public AccessContextService(TenantUserPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public AccessContext resolve(UUID tenantId, String userId) {
        List<TenantUserPermission> permissions =
                permissionRepository.findByTenantIdAndUserId(tenantId, userId);
        if (permissions.isEmpty()) {
            throw new KnowledgeAccessDeniedException("用户不属于该租户或没有知识库访问权限");
        }
        Set<String> departments = permissions.stream()
                .map(TenantUserPermission::department)
                .collect(Collectors.toUnmodifiableSet());
        PermissionLevel highestLevel = permissions.stream()
                .map(TenantUserPermission::permissionLevel)
                .max(Comparator.comparingInt(PermissionLevel::rank))
                .orElseThrow();
        return new AccessContext(tenantId, userId, departments, highestLevel);
    }
}
