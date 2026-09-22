package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.chunking.ChunkCandidate;

import java.util.List;

public record ProcessedDocument(
        ParsedDocument document,
        List<ChunkCandidate> chunks
) {
    public ProcessedDocument {
        chunks = List.copyOf(chunks);
    }
}
