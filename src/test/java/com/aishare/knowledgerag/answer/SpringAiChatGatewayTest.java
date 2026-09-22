package com.aishare.knowledgerag.answer;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringAiChatGatewayTest {

    @Test
    void explainsHowToEnableChatWhenModelIsDisabled() {
        SpringAiChatGateway gateway = new SpringAiChatGateway(Optional.empty());

        assertThatThrownBy(() -> gateway.generate("system", "user"))
                .isInstanceOf(ChatUnavailableException.class)
                .hasMessageContaining("AI_CHAT_ENABLED=openai");
    }
}
