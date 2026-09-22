package com.aishare.knowledgerag.api.dto;

import com.aishare.knowledgerag.retrieval.RetrievedChunk;

import java.util.List;

public record VectorSearchResponse(
        String question,
        int count,
        List<RetrievedChunk> results
) {
    public VectorSearchResponse {
        results = List.copyOf(results);
    }
}
