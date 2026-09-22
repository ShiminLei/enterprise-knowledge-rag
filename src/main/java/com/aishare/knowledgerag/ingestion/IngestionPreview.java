package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.chunking.ChunkCandidate;

import java.util.List;

public record IngestionPreview(
        String title,
        String sourceFileName,
        String mediaType,
        int sectionCount,
        int paragraphCount,
        int chunkCount,
        List<ChunkCandidate> chunks
) {
    public IngestionPreview {
        chunks = List.copyOf(chunks);
    }
}
