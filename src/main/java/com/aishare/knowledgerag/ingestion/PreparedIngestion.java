package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.document.KnowledgeChunk;
import com.aishare.knowledgerag.document.KnowledgeDocument;

import java.util.List;

public record PreparedIngestion(
        KnowledgeDocument document,
        String parsedTitle,
        int sectionCount,
        List<KnowledgeChunk> chunks,
        List<String> warnings
) {
    public PreparedIngestion {
        chunks = List.copyOf(chunks);
        warnings = List.copyOf(warnings);
    }
}
