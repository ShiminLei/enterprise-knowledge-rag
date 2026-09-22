package com.aishare.knowledgerag.prompt;

import com.aishare.knowledgerag.ingestion.DocumentChecksumService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Service
public class PromptTemplateService {

    public static final String RAG_ANSWER_PROMPT_KEY = "rag-answer-system";

    private final PromptTemplateRepository repository;
    private final DocumentChecksumService checksumService;

    public PromptTemplateService(
            PromptTemplateRepository repository,
            DocumentChecksumService checksumService
    ) {
        this.repository = repository;
        this.checksumService = checksumService;
    }

    public PromptTemplate activeRagAnswerPrompt() {
        return repository.findActive(RAG_ANSWER_PROMPT_KEY)
                .orElseThrow(() -> new PromptTemplateNotFoundException(
                        "未配置启用的 RAG 回答 Prompt: " + RAG_ANSWER_PROMPT_KEY
                ));
    }

    public List<PromptTemplate> ragAnswerPromptVersions() {
        return repository.findAll(RAG_ANSWER_PROMPT_KEY);
    }

    @Transactional
    public PromptTemplate createRagAnswerPromptVersion(
            String version,
            String content,
            String createdBy
    ) {
        repository.lockVersions(RAG_ANSWER_PROMPT_KEY);
        if (repository.findByVersion(RAG_ANSWER_PROMPT_KEY, version).isPresent()) {
            throw duplicateVersion(version);
        }
        PromptTemplate template = new PromptTemplate(
                RAG_ANSWER_PROMPT_KEY,
                version,
                content,
                checksumService.sha256(content.getBytes(StandardCharsets.UTF_8)),
                false,
                createdBy,
                Instant.now()
        );
        try {
            repository.insert(template);
        } catch (DuplicateKeyException exception) {
            throw duplicateVersion(version);
        }
        return template;
    }

    @Transactional
    public PromptTemplate activateRagAnswerPromptVersion(String version) {
        repository.lockVersions(RAG_ANSWER_PROMPT_KEY);
        PromptTemplate target = repository
                .findByVersion(RAG_ANSWER_PROMPT_KEY, version)
                .orElseThrow(() -> new PromptVersionNotFoundException(
                        "Prompt 版本不存在: " + version
                ));
        if (target.active()) {
            return target;
        }
        repository.deactivateAll(RAG_ANSWER_PROMPT_KEY);
        if (repository.activate(RAG_ANSWER_PROMPT_KEY, version) != 1) {
            throw new PromptVersionNotFoundException("Prompt 版本不存在: " + version);
        }
        return repository.findByVersion(RAG_ANSWER_PROMPT_KEY, version)
                .orElseThrow(() -> new PromptVersionNotFoundException(
                        "Prompt 版本不存在: " + version
                ));
    }

    private PromptVersionConflictException duplicateVersion(String version) {
        return new PromptVersionConflictException("Prompt 版本已存在: " + version);
    }
}
