package com.aishare.knowledgerag.prompt;

public record PromptTemplate(
        String key,
        String version,
        String content,
        String checksum
) {
}
