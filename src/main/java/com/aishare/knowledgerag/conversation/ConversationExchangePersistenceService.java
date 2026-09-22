package com.aishare.knowledgerag.conversation;

import com.aishare.knowledgerag.answer.GroundedAnswer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConversationExchangePersistenceService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;

    public ConversationExchangePersistenceService(
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public void save(
            Conversation conversation,
            boolean newConversation,
            String question,
            GroundedAnswer answer
    ) {
        if (newConversation) {
            conversationRepository.insert(conversation);
        }
        messageRepository.append(
                conversation.id(),
                MessageRole.USER,
                question,
                Map.of()
        );
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("grounded", answer.grounded());
        metadata.put("retrievedCount", answer.retrievedCount());
        metadata.put("citations", answer.citations());
        messageRepository.append(
                conversation.id(),
                MessageRole.ASSISTANT,
                answer.answer(),
                metadata
        );
        conversationRepository.touch(conversation.id());
    }
}
