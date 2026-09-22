package com.aishare.knowledgerag.answer;

import java.util.List;

public record GroundedAnswer(
        String answer,
        boolean grounded,
        int retrievedCount,
        List<AnswerCitation> citations,
        String promptVersion
) {
    public GroundedAnswer {
        citations = List.copyOf(citations);
    }

    public GroundedAnswer(
            String answer,
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations
    ) {
        this(answer, grounded, retrievedCount, citations, "none");
    }
}
