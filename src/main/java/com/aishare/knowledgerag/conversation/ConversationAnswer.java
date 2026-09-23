package com.aishare.knowledgerag.conversation;

import com.aishare.knowledgerag.answer.AnswerCitation;
import com.aishare.knowledgerag.answer.AnswerRetrievedChunk;
import com.aishare.knowledgerag.answer.GroundedAnswer;

import java.util.List;
import java.util.UUID;

public record ConversationAnswer(
        UUID conversationId,
        String answer,
        boolean grounded,
        int retrievedCount,
        List<AnswerCitation> citations,
        String promptVersion,
        List<AnswerRetrievedChunk> retrievedChunks,
        double confidence,
        String cannotAnswerReason
) {
    public ConversationAnswer {
        citations = List.copyOf(citations);
        retrievedChunks = List.copyOf(retrievedChunks);
    }

    public ConversationAnswer(
            UUID conversationId,
            String answer,
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations
    ) {
        this(conversationId, answer, grounded, retrievedCount, citations, "none", List.of(),
                grounded ? 1.0 : 0.0, grounded ? null : "NO_ACCESSIBLE_EVIDENCE");
    }

    public ConversationAnswer(
            UUID conversationId,
            String answer,
            boolean grounded,
            int retrievedCount,
            List<AnswerCitation> citations,
            String promptVersion
    ) {
        this(conversationId, answer, grounded, retrievedCount, citations, promptVersion,
                List.of(), grounded ? 1.0 : 0.0,
                grounded ? null : "NO_ACCESSIBLE_EVIDENCE");
    }

    public static ConversationAnswer from(UUID conversationId, GroundedAnswer answer) {
        return new ConversationAnswer(
                conversationId,
                answer.answer(),
                answer.grounded(),
                answer.retrievedCount(),
                answer.citations(),
                answer.promptVersion(),
                answer.retrievedChunks(),
                answer.confidence(),
                answer.cannotAnswerReason()
        );
    }
}
