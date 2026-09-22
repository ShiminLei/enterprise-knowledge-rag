package com.aishare.knowledgerag.streaming;

import java.util.UUID;

public record AnswerStreamCompleted(
        UUID conversationId,
        boolean grounded,
        int retrievedCount
) {
}
