package com.aishare.knowledgerag.answer;

import org.springframework.ai.chat.messages.SystemMessage;
import com.aishare.knowledgerag.resilience.AiResilienceExecutor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SpringAiChatGateway implements ChatGateway {

    private final Optional<ChatModel> chatModel;
    private final AiResilienceExecutor resilienceExecutor;

    public SpringAiChatGateway(
            Optional<ChatModel> chatModel,
            AiResilienceExecutor resilienceExecutor
    ) {
        this.chatModel = chatModel;
        this.resilienceExecutor = resilienceExecutor;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        ChatModel model = chatModel.orElseThrow(() -> new ChatUnavailableException(
                "聊天模型未启用，请配置 AI_CHAT_ENABLED=openai 和 AI_API_KEY"
        ));
        try {
            ChatResponse response = resilienceExecutor.executeChat(() ->
                    model.call(new Prompt(List.of(
                            new SystemMessage(systemPrompt),
                            new UserMessage(userPrompt)
                    )))
            );
            if (response == null || response.getResult() == null
                    || response.getResult().getOutput() == null
                    || response.getResult().getOutput().getText() == null
                    || response.getResult().getOutput().getText().isBlank()) {
                throw new ChatGenerationException("聊天模型返回了空答案");
            }
            return response.getResult().getOutput().getText().strip();
        } catch (ChatGenerationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ChatGenerationException("调用聊天模型失败", exception);
        }
    }
}
