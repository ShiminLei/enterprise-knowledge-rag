package com.aishare.knowledgerag.embedding;

import com.aishare.knowledgerag.resilience.AiResilienceExecutor;
import com.aishare.knowledgerag.resilience.AiResilienceProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringAiEmbeddingGatewayTest {

    @Test
    void explainsHowToEnableEmbeddingWhenModelIsDisabled() {
        SpringAiEmbeddingGateway gateway = new SpringAiEmbeddingGateway(
                Optional.empty(),
                executor()
        );

        assertThatThrownBy(() -> gateway.embed(List.of("正文")))
                .isInstanceOf(EmbeddingUnavailableException.class)
                .hasMessageContaining("AI_EMBEDDING_ENABLED=openai");
    }

    private AiResilienceExecutor executor() {
        return new AiResilienceExecutor(new AiResilienceProperties(
                3, Duration.ZERO, 50, 10, 5, Duration.ofSeconds(30),
                100, Duration.ofSeconds(1)
        ));
    }
}
