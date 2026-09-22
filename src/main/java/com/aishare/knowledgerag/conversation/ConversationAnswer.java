package com.aishare.knowledgerag.conversation;

import com.aishare.knowledgerag.answer.AnswerCitation;
import com.aishare.knowledgerag.answer.GroundedAnswer;

import java.util.List;
import java.util.UUID;

public record ConversationAnswer(
        UUID conversationId,
        String answer,
        boolean grounded,
        int retrievedCount,
        List<AnswerCitation> citations,
        String promptVersion
) {
    public ConversationAnswer {
        citations = List.copyOf(citations);
    }

    public ConversationAnswer(
            UUID conversationId,
            String answer,
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations
    ) {
        this(conversationId, answer, grounded, retrievedCount, citations, "none");
    }

    public static ConversationAnswer from(UUID conversationId, GroundedAnswer answer) {
        return new ConversationAnswer(
                conversationId,
                answer.answer(),
                answer.grounded(),
                answer.retrievedCount(),
                answer.citations(),
                answer.promptVersion()
        );
    }
}
