package com.aishare.knowledgerag.prompt;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptTemplateServiceTest {

    @Test
    void returnsActiveRagAnswerPrompt() {
        PromptTemplate expected = new PromptTemplate(
                PromptTemplateService.RAG_ANSWER_PROMPT_KEY,
                "v2",
                "只根据资料回答",
                "checksum"
        );
        PromptTemplateService service = new PromptTemplateService(
                key -> Optional.of(expected)
        );

        assertThat(service.activeRagAnswerPrompt()).isEqualTo(expected);
    }

    @Test
    void failsClosedWhenNoPromptIsActive() {
        PromptTemplateService service = new PromptTemplateService(
                key -> Optional.empty()
        );

        assertThatThrownBy(service::activeRagAnswerPrompt)
                .isInstanceOf(PromptTemplateNotFoundException.class)
                .hasMessageContaining(PromptTemplateService.RAG_ANSWER_PROMPT_KEY);
    }
}
