package com.aishare.knowledgerag.audit;

import java.util.List;
import java.util.UUID;

public record RagRequestAudit(
        UUID id,
        String requestId,
        UUID tenantId,
        String userId,
        UUID conversationId,
        String questionDigest,
        String retrievalMode,
        String modelName,
        String promptVersion,
        List<UUID> retrievedDocumentIds,
        String outcome,
        long latencyMs,
        String errorCode
) {
    public RagRequestAudit {
        retrievedDocumentIds = List.copyOf(retrievedDocumentIds);
    }
}
