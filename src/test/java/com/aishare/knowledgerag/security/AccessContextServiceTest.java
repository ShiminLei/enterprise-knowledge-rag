package com.aishare.knowledgerag.security;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessContextServiceTest {

    private static final UUID TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void buildsAccessContextFromDatabasePermissions() {
        TenantUserPermissionRepository repository = (tenantId, userId) -> List.of(
                new TenantUserPermission(
                        tenantId, userId, "信息技术部", PermissionLevel.CONFIDENTIAL
                ),
                new TenantUserPermission(
                        tenantId, userId, "人力资源部", PermissionLevel.INTERNAL
                )
        );
        AccessContextService service = new AccessContextService(repository);

        AccessContext context = service.resolve(TENANT_ID, "zhangsan");

        assertThat(context.departments()).containsExactlyInAnyOrder("信息技术部", "人力资源部");
        assertThat(context.permissionLevel()).isEqualTo(PermissionLevel.CONFIDENTIAL);
    }

    @Test
    void rejectsUserWithoutTenantPermission() {
        AccessContextService service = new AccessContextService((tenantId, userId) -> List.of());

        assertThatThrownBy(() -> service.resolve(TENANT_ID, "unknown"))
                .isInstanceOf(KnowledgeAccessDeniedException.class)
                .hasMessageContaining("没有知识库访问权限");
    }
}
