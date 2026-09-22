package com.aishare.knowledgerag.evaluation;

import java.util.UUID;

public record EvaluationRetrievedChunk(
        int rank,
        UUID chunkId,
        UUID documentId,
        Integer vectorRank,
        Integer keywordRank,
        double rrfScore
) {
}
