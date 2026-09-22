package com.aishare.knowledgerag.conversation;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {

    Optional<Conversation> findOwned(UUID conversationId, UUID tenantId, String userId);

    void insert(Conversation conversation);

    void touch(UUID conversationId);
}
