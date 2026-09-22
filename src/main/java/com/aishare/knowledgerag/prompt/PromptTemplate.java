package com.aishare.knowledgerag.prompt;

import java.time.Instant;

public record PromptTemplate(
        String key,
        String version,
        String content,
        String checksum,
        boolean active,
        String createdBy,
        Instant createdAt
) {
    public PromptTemplate(
            String key,
            String version,
            String content,
            String checksum
    ) {
        this(key, version, content, checksum, false, "test", Instant.EPOCH);
    }
}
