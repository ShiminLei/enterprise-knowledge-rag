package com.aishare.knowledgerag.conversation;

import com.aishare.knowledgerag.answer.AnswerCitation;
import com.aishare.knowledgerag.answer.AnswerRetrievedChunk;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public record ConversationAnswerStream(
        UUID conversationId,
        boolean grounded,
        int retrievedCount,
        List<AnswerCitation> citations,
        String promptVersion,
        List<AnswerRetrievedChunk> retrievedChunks,
        double confidence,
        String cannotAnswerReason,
        Flux<String> content
) {
    public ConversationAnswerStream {
        citations = List.copyOf(citations);
        retrievedChunks = List.copyOf(retrievedChunks);
    }

    public ConversationAnswerStream(
            UUID conversationId,
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations,
            Flux<String> content
    ) {
        this(conversationId, grounded, retrievedCount, citations, "none", List.of(),
                grounded ? 1.0 : 0.0, grounded ? null : "NO_ACCESSIBLE_EVIDENCE", content);
    }

    public ConversationAnswerStream(
            UUID conversationId,
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations,
            String promptVersion,
            Flux<String> content
    ) {
        this(conversationId, grounded, retrievedCount, citations, promptVersion, List.of(),
                grounded ? 1.0 : 0.0, grounded ? null : "NO_ACCESSIBLE_EVIDENCE", content);
    }

    public ConversationAnswerStream withContent(Flux<String> decoratedContent) {
        return new ConversationAnswerStream(
                conversationId, grounded, retrievedCount, citations, promptVersion,
                retrievedChunks, confidence, cannotAnswerReason, decoratedContent
        );
    }
}
