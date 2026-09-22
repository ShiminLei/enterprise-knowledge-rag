package com.aishare.knowledgerag.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SpringAiEmbeddingGateway implements EmbeddingGateway {

    private final Optional<EmbeddingModel> embeddingModel;

    public SpringAiEmbeddingGateway(Optional<EmbeddingModel> embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        EmbeddingModel model = embeddingModel.orElseThrow(() ->
                new EmbeddingUnavailableException(
                        "Embedding 模型未启用，请配置 AI_EMBEDDING_ENABLED=openai 和 AI_API_KEY"
                )
        );
        try {
            return model.embed(texts);
        } catch (RuntimeException exception) {
            throw new EmbeddingGenerationException("调用 Embedding 模型失败", exception);
        }
    }
}
