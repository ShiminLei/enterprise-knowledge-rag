package com.aishare.knowledgerag.conversation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.conversation")
public record ConversationProperties(int historyLimit, int maxHistoryCharacters) {

    public ConversationProperties {
        if (historyLimit <= 0 || historyLimit > 50) {
            throw new IllegalArgumentException("rag.conversation.history-limit 必须在 1 到 50 之间");
        }
        if (maxHistoryCharacters < 500) {
            throw new IllegalArgumentException("rag.conversation.max-history-characters 不能小于 500");
        }
    }
}
