package com.aishare.knowledgerag.conversation;

import java.time.Instant;
import java.util.UUID;

public record Conversation(
        UUID id,
        UUID tenantId,
        String userId,
        String title,
        Instant createdAt
) {
}
