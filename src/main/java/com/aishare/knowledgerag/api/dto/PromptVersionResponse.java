package com.aishare.knowledgerag.api.dto;

import com.aishare.knowledgerag.prompt.PromptTemplate;

import java.time.Instant;

public record PromptVersionResponse(
        String promptKey,
        String version,
        String content,
        String checksum,
        boolean active,
        String createdBy,
        Instant createdAt
) {
    public static PromptVersionResponse from(PromptTemplate template) {
        return new PromptVersionResponse(
                template.key(),
                template.version(),
                template.content(),
                template.checksum(),
                template.active(),
                template.createdBy(),
                template.createdAt()
        );
    }
}
