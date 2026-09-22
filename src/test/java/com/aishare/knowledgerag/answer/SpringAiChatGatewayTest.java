package com.aishare.knowledgerag.answer;

import com.aishare.knowledgerag.resilience.AiResilienceExecutor;
import com.aishare.knowledgerag.resilience.AiResilienceProperties;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringAiChatGatewayTest {

    @Test
    void explainsHowToEnableChatWhenModelIsDisabled() {
        SpringAiChatGateway gateway = new SpringAiChatGateway(
                Optional.empty(),
                executor()
        );

        assertThatThrownBy(() -> gateway.generate("system", "user"))
                .isInstanceOf(ChatUnavailableException.class)
                .hasMessageContaining("AI_CHAT_ENABLED=openai");
    }

    private AiResilienceExecutor executor() {
        return new AiResilienceExecutor(new AiResilienceProperties(
                3, Duration.ZERO, 50, 10, 5, Duration.ofSeconds(30)
        ));
    }
}
