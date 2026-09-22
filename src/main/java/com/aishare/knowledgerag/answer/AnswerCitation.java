package com.aishare.knowledgerag.answer;

import java.util.UUID;

public record AnswerCitation(
        int marker,
        UUID chunkId,
        UUID documentId,
        String titlePath,
        Integer pageNumber,
        String source,
        String documentVersion
) {
}
