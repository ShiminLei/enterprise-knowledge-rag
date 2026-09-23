package com.aishare.knowledgerag.answer;

import java.util.UUID;

public record AnswerCitation(
        int marker,
        UUID chunkId,
        UUID documentId,
        String titlePath,
        Integer pageNumber,
        Integer startParagraphNumber,
        Integer endParagraphNumber,
        String excerpt,
        String source,
        String documentVersion,
        String sourceLink
) {
    public AnswerCitation(
            int marker,
            UUID chunkId,
            UUID documentId,
            String titlePath,
            Integer pageNumber,
            String source,
            String documentVersion
    ) {
        this(marker, chunkId, documentId, titlePath, pageNumber, null, null,
                null, source, documentVersion, null);
    }
}
