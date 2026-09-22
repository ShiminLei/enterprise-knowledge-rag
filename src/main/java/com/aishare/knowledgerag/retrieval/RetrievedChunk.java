package com.aishare.knowledgerag.retrieval;

import com.aishare.knowledgerag.document.DocumentCategory;

import java.util.UUID;

public record RetrievedChunk(
        UUID chunkId,
        UUID documentId,
        int chunkIndex,
        String content,
        String titlePath,
        Integer pageNumber,
        DocumentCategory category,
        String documentVersion,
        String source,
        double score
) {
}
