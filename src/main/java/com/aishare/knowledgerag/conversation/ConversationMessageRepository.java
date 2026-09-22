package com.aishare.knowledgerag.conversation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ConversationMessageRepository {

    List<ConversationTurn> findRecent(UUID conversationId, int limit);

    void append(
            UUID conversationId,
            MessageRole role,
            String content,
            Map<String, Object> metadata
    );
}
