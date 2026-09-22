package com.aishare.knowledgerag.api.dto;

import com.aishare.knowledgerag.retrieval.HybridSearchResult;

import java.util.List;

public record HybridSearchResponse(
        String question,
        int count,
        List<HybridSearchResult> results
) {
    public HybridSearchResponse {
        results = List.copyOf(results);
    }
}
