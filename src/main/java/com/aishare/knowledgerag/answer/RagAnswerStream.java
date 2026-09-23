package com.aishare.knowledgerag.answer;

import reactor.core.publisher.Flux;

import java.util.List;

public record RagAnswerStream(
        boolean grounded,
        int retrievedCount,
        List<AnswerCitation> citations,
        String promptVersion,
        List<AnswerRetrievedChunk> retrievedChunks,
        double confidence,
        String cannotAnswerReason,
        Flux<String> content
) {
    public RagAnswerStream {
        citations = List.copyOf(citations);
        retrievedChunks = List.copyOf(retrievedChunks);
    }

    public RagAnswerStream(
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations,
            Flux<String> content
    ) {
        this(grounded, retrievedCount, citations, "none", List.of(),
                grounded ? 1.0 : 0.0, grounded ? null : "NO_ACCESSIBLE_EVIDENCE", content);
    }

    public RagAnswerStream(
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations,
            String promptVersion,
            Flux<String> content
    ) {
        this(grounded, retrievedCount, citations, promptVersion, List.of(),
                grounded ? 1.0 : 0.0, grounded ? null : "NO_ACCESSIBLE_EVIDENCE", content);
    }
}
