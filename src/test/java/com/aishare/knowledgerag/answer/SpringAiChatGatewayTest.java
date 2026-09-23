package com.aishare.knowledgerag.answer;

import com.aishare.knowledgerag.resilience.AiResilienceExecutor;
import com.aishare.knowledgerag.resilience.AiResilienceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.List;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void exposesNativeSpringAiResponseChunks() {
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(
                response("请使用"),
                response("公司账号"),
                response("登录。[1]")
        ));
        SpringAiChatGateway gateway = new SpringAiChatGateway(
                Optional.of(model),
                executor()
        );

        assertThat(gateway.stream("system", "user").collectList().block())
                .containsExactly("请使用", "公司账号", "登录。[1]");
    }

    @Test
    void ignoresTerminalResponseWithoutText() {
        ChatModel model = mock(ChatModel.class);
        ChatResponse terminalResponse = mock(ChatResponse.class);
        when(terminalResponse.getResult()).thenReturn(null);
        when(model.stream(any(Prompt.class))).thenReturn(Flux.just(
                response("连续五次登录失败"),
                terminalResponse
        ));
        SpringAiChatGateway gateway = new SpringAiChatGateway(
                Optional.of(model),
                executor()
        );

        assertThat(gateway.stream("system", "user").collectList().block())
                .containsExactly("连续五次登录失败");
    }

    private ChatResponse response(String text) {
        return new ChatResponse(List.of(
                new Generation(new AssistantMessage(text))
        ));
    }

    private AiResilienceExecutor executor() {
        return new AiResilienceExecutor(new AiResilienceProperties(
                3, Duration.ZERO, 50, 10, 5, Duration.ofSeconds(30),
                100, Duration.ofSeconds(1)
        ));
    }
}
