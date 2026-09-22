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
        List<AnswerCitation> citations
) {
    public ConversationAnswer {
        citations = List.copyOf(citations);
    }

    public static ConversationAnswer from(UUID conversationId, GroundedAnswer answer) {
        return new ConversationAnswer(
                conversationId,
                answer.answer(),
                answer.grounded(),
                answer.retrievedCount(),
                answer.citations()
        );
    }
}
