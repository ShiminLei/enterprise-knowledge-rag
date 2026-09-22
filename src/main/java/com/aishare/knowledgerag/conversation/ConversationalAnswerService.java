package com.aishare.knowledgerag.conversation;

import com.aishare.knowledgerag.answer.GroundedAnswer;
import com.aishare.knowledgerag.answer.RagAnswerService;
import com.aishare.knowledgerag.answer.RagAnswerStream;
import com.aishare.knowledgerag.retrieval.VectorSearchQuery;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
        ConversationContext context = prepareConversation(conversationId, query);

        GroundedAnswer answer = answerService.answer(query, context.history());
        persistenceService.save(
                context.conversation(),
                context.isNew(),
                query.question(),
                answer
        );
        return ConversationAnswer.from(context.conversation().id(), answer);
    }

    public ConversationAnswerStream stream(
            Optional<UUID> conversationId,
            VectorSearchQuery query
    ) {
        ConversationContext context = prepareConversation(conversationId, query);
        RagAnswerStream answer = answerService.stream(query, context.history());
        StringBuilder fullAnswer = new StringBuilder();
        Flux<String> content = answer.content()
                .doOnNext(fullAnswer::append)
                .doOnComplete(() -> persistenceService.save(
                        context.conversation(),
                        context.isNew(),
                        query.question(),
                        new GroundedAnswer(
                                fullAnswer.toString(),
                                answer.grounded(),
                                answer.retrievedCount(),
                                answer.citations(),
                                answer.promptVersion()
                        )
                ));
        return new ConversationAnswerStream(
                context.conversation().id(),
                answer.grounded(),
                answer.retrievedCount(),
                answer.citations(),
                answer.promptVersion(),
                content
        );
    }

    private ConversationContext prepareConversation(
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
        return new ConversationContext(conversation, isNew, history);
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

    private record ConversationContext(
            Conversation conversation,
            boolean isNew,
            List<ConversationTurn> history
    ) {
    }
}
