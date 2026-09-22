package com.aishare.knowledgerag.answer;

import java.util.List;

public record GroundedAnswer(
        String answer,
        boolean grounded,
        int retrievedCount,
        List<AnswerCitation> citations
) {
    public GroundedAnswer {
        citations = List.copyOf(citations);
    }
}
