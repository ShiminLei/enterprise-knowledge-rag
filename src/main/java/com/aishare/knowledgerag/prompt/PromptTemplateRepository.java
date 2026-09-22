package com.aishare.knowledgerag.prompt;

import java.util.Optional;
import java.util.List;

public interface PromptTemplateRepository {

    Optional<PromptTemplate> findActive(String promptKey);

    Optional<PromptTemplate> findByVersion(String promptKey, String version);

    List<PromptTemplate> findAll(String promptKey);

    void lockVersions(String promptKey);

    void insert(PromptTemplate template);

    void deactivateAll(String promptKey);

    int activate(String promptKey, String version);
}
