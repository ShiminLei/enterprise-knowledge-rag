package com.aishare.knowledgerag.retrieval;

public record HybridSearchResult(
        RetrievedChunk chunk,
        Integer vectorRank,
        Integer keywordRank,
        Double vectorScore,
        Double keywordScore,
        double rrfScore
) {
}
