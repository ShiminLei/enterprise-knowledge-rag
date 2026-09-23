package com.aishare.knowledgerag.answer;

import java.util.List;

public record GroundedAnswer(
        String answer,
        boolean grounded,
        int retrievedCount,
        List<AnswerCitation> citations,
        String promptVersion,
        List<AnswerRetrievedChunk> retrievedChunks,
        double confidence,
        String cannotAnswerReason
) {
    public GroundedAnswer {
        citations = List.copyOf(citations);
        retrievedChunks = List.copyOf(retrievedChunks);
    }

    public GroundedAnswer(
            String answer,
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations
    ) {
        this(answer, grounded, retrievedCount, citations, "none", List.of(),
                grounded ? 1.0 : 0.0, grounded ? null : "NO_ACCESSIBLE_EVIDENCE");
    }

    public GroundedAnswer(
            String answer,
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations,
            String promptVersion
    ) {
        this(answer, grounded, retrievedCount, citations, promptVersion, List.of(),
                grounded ? 1.0 : 0.0, grounded ? null : "NO_ACCESSIBLE_EVIDENCE");
    }
}
