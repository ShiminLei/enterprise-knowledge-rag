package com.aishare.knowledgerag.retrieval;

public record HybridSearchResult(
        RetrievedChunk chunk,
        Integer vectorRank,
        Integer keywordRank,
        Double vectorScore,
        Double keywordScore,
        double rrfScore,
        double rerankScore
) {
    public HybridSearchResult(
            RetrievedChunk chunk,
            Integer vectorRank,
            Integer keywordRank,
            Double vectorScore,
            Double keywordScore,
            double rrfScore
    ) {
        this(chunk, vectorRank, keywordRank, vectorScore, keywordScore,
                rrfScore, rrfScore);
    }

    public HybridSearchResult withRerankScore(double score) {
        return new HybridSearchResult(
                chunk, vectorRank, keywordRank, vectorScore, keywordScore,
                rrfScore, score
        );
    }
}
