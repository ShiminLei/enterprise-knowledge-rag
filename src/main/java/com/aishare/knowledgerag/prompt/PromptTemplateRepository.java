package com.aishare.knowledgerag.prompt;

import java.util.Optional;

public interface PromptTemplateRepository {

    Optional<PromptTemplate> findActive(String promptKey);
}
