package com.aishare.knowledgerag.document;

import com.aishare.knowledgerag.security.PermissionLevel;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocument(
        UUID id,
        UUID tenantId,
        String externalDocumentId,
        String title,
        String source,
        String fileName,
        String mediaType,
        DocumentCategory category,
        String version,
        Instant updatedAt,
        PermissionLevel permissionLevel,
        String department,
        DocumentStatus status,
        String checksum
) {
}
