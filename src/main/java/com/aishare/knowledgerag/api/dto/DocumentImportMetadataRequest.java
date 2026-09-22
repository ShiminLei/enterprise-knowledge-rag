package com.aishare.knowledgerag.api.dto;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.ingestion.DocumentImportMetadata;
import com.aishare.knowledgerag.security.PermissionLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record DocumentImportMetadataRequest(
        @NotNull(message = "不能为空") UUID tenantId,
        @NotBlank(message = "不能为空") @Size(max = 128, message = "长度不能超过 128") String externalDocumentId,
        @NotBlank(message = "不能为空") @Size(max = 300, message = "长度不能超过 300") String title,
        @NotBlank(message = "不能为空") @Size(max = 500, message = "长度不能超过 500") String source,
        @NotNull(message = "不能为空") DocumentCategory category,
        @NotBlank(message = "不能为空") @Size(max = 64, message = "长度不能超过 64") String version,
        @NotNull(message = "不能为空") Instant updatedAt,
        @NotNull(message = "不能为空") PermissionLevel permissionLevel,
        @NotBlank(message = "不能为空") @Size(max = 128, message = "长度不能超过 128") String department
) {
    public DocumentImportMetadata toDomain() {
        return new DocumentImportMetadata(
                tenantId,
                externalDocumentId.strip(),
                title.strip(),
                source.strip(),
                category,
                version.strip(),
                updatedAt,
                permissionLevel,
                department.strip()
        );
    }
}
