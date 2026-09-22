package com.aishare.knowledgerag.prompt;

import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    public static final String RAG_ANSWER_PROMPT_KEY = "rag-answer-system";

    private final PromptTemplateRepository repository;

    public PromptTemplateService(PromptTemplateRepository repository) {
        this.repository = repository;
    }

    public PromptTemplate activeRagAnswerPrompt() {
        return repository.findActive(RAG_ANSWER_PROMPT_KEY)
                .orElseThrow(() -> new PromptTemplateNotFoundException(
                        "未配置启用的 RAG 回答 Prompt: " + RAG_ANSWER_PROMPT_KEY
                ));
    }
}
