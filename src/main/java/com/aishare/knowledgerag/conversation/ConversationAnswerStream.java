package com.aishare.knowledgerag.conversation;

import com.aishare.knowledgerag.answer.AnswerCitation;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public record ConversationAnswerStream(
        UUID conversationId,
        boolean grounded,
        int retrievedCount,
        List<AnswerCitation> citations,
        String promptVersion,
        Flux<String> content
) {
    public ConversationAnswerStream {
        citations = List.copyOf(citations);
    }

    public ConversationAnswerStream(
            UUID conversationId,
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations,
            Flux<String> content
    ) {
        this(conversationId, grounded, retrievedCount, citations, "none", content);
    }

    public ConversationAnswerStream withContent(Flux<String> decoratedContent) {
        return new ConversationAnswerStream(
                conversationId, grounded, retrievedCount, citations, promptVersion,
                decoratedContent
        );
    }
}
