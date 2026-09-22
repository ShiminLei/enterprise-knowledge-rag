package com.aishare.knowledgerag.embedding;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringAiEmbeddingGatewayTest {

    @Test
    void explainsHowToEnableEmbeddingWhenModelIsDisabled() {
        SpringAiEmbeddingGateway gateway = new SpringAiEmbeddingGateway(Optional.empty());

        assertThatThrownBy(() -> gateway.embed(List.of("正文")))
                .isInstanceOf(EmbeddingUnavailableException.class)
                .hasMessageContaining("AI_EMBEDDING_ENABLED=openai");
    }
}
