package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.embedding.EmbeddedChunk;

import java.util.List;

public record EmbeddedIngestion(
        PreparedIngestion prepared,
        List<EmbeddedChunk> chunks
) {
    public EmbeddedIngestion {
        chunks = List.copyOf(chunks);
        if (prepared.chunks().size() != chunks.size()) {
            throw new IllegalArgumentException("切块数与向量数不一致");
        }
    }
}
