package com.aishare.knowledgerag.document;

import com.aishare.knowledgerag.security.PermissionLevel;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeChunk(
        UUID id,
        UUID documentId,
        UUID tenantId,
        int chunkIndex,
        String content,
        String titlePath,
        Integer pageNumber,
        Integer paragraphNumber,
        DocumentCategory category,
        String documentVersion,
        Instant documentUpdatedAt,
        PermissionLevel permissionLevel,
        String department,
        String source
) {
}
