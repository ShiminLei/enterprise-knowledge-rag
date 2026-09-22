package com.aishare.knowledgerag.answer;

import reactor.core.publisher.Flux;

import java.util.List;

public record RagAnswerStream(
        boolean grounded,
        int retrievedCount,
        List<AnswerCitation> citations,
        String promptVersion,
        Flux<String> content
) {
    public RagAnswerStream {
        citations = List.copyOf(citations);
    }

    public RagAnswerStream(
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations,
            Flux<String> content
    ) {
        this(grounded, retrievedCount, citations, "none", content);
    }
}
