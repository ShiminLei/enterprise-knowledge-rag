package com.aishare.knowledgerag.conversation;

import com.aishare.knowledgerag.answer.GroundedAnswer;
import com.aishare.knowledgerag.answer.RagAnswerService;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConversationalAnswerService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ConversationExchangePersistenceService persistenceService;
    private final RagAnswerService answerService;
    private final ConversationProperties properties;

    public ConversationalAnswerService(
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            ConversationExchangePersistenceService persistenceService,
            RagAnswerService answerService,
            ConversationProperties properties
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.persistenceService = persistenceService;
        this.answerService = answerService;
        this.properties = properties;
    }

    public ConversationAnswer answer(
            Optional<UUID> conversationId,
            VectorSearchQuery query
    ) {
        Conversation conversation = conversationId
                .map(id -> findOwned(id, query))
                .orElseGet(() -> newConversation(query));
        boolean isNew = conversationId.isEmpty();
        List<ConversationTurn> history = isNew
                ? List.of()
                : recentHistory(conversation.id());

        GroundedAnswer answer = answerService.answer(query, history);
        persistenceService.save(
                conversation,
                isNew,
                query.question(),
                answer
        );
        return ConversationAnswer.from(conversation.id(), answer);
    }

    private Conversation findOwned(UUID id, VectorSearchQuery query) {
        return conversationRepository.findOwned(
                        id,
                        query.accessContext().tenantId(),
                        query.accessContext().userId()
                )
                .orElseThrow(() -> new ConversationNotFoundException("会话不存在或无权访问"));
    }

    private Conversation newConversation(VectorSearchQuery query) {
        String title = query.question().length() <= 100
                ? query.question()
                : query.question().substring(0, 100);
        return new Conversation(
                UUID.randomUUID(),
                query.accessContext().tenantId(),
                query.accessContext().userId(),
                title,
                Instant.now()
        );
    }

    private List<ConversationTurn> recentHistory(UUID conversationId) {
        List<ConversationTurn> recent = messageRepository.findRecent(
                conversationId,
                properties.historyLimit()
        );
        List<ConversationTurn> selectedNewestFirst = new ArrayList<>();
        int used = 0;
        for (int index = recent.size() - 1; index >= 0; index--) {
            ConversationTurn turn = recent.get(index);
            if (used + turn.content().length() > properties.maxHistoryCharacters()) {
                break;
            }
            selectedNewestFirst.add(turn);
            used += turn.content().length();
        }
        Collections.reverse(selectedNewestFirst);
        return List.copyOf(selectedNewestFirst);
    }
}
