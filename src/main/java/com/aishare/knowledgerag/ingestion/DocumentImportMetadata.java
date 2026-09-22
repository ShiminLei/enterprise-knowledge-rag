package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.security.PermissionLevel;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DocumentImportMetadata(
        UUID tenantId,
        String externalDocumentId,
        String title,
        String source,
        DocumentCategory category,
        String version,
        Instant updatedAt,
        PermissionLevel permissionLevel,
        String department
) {
    public DocumentImportMetadata {
        tenantId = Objects.requireNonNull(tenantId, "tenantId 不能为 null");
        externalDocumentId = requireText(externalDocumentId, "externalDocumentId");
        title = requireText(title, "title");
        source = requireText(source, "source");
        category = Objects.requireNonNull(category, "category 不能为 null");
        version = requireText(version, "version");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt 不能为 null");
        permissionLevel = Objects.requireNonNull(permissionLevel, "permissionLevel 不能为 null");
        department = requireText(department, "department");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return value.strip();
    }
}
