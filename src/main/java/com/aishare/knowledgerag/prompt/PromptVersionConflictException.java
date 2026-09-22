package com.aishare.knowledgerag.prompt;

public class PromptVersionConflictException extends RuntimeException {

    public PromptVersionConflictException(String message) {
        super(message);
    }
}
