package com.aishare.knowledgerag.answer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag.answer")
public record AnswerProperties(int maxContextCharacters) {

    public AnswerProperties {
        if (maxContextCharacters < 1000) {
            throw new IllegalArgumentException("rag.answer.max-context-characters 不能小于 1000");
        }
    }
}
