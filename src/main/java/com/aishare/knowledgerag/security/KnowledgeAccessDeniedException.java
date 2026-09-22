package com.aishare.knowledgerag.security;

public class KnowledgeAccessDeniedException extends RuntimeException {

    public KnowledgeAccessDeniedException(String message) {
        super(message);
    }
}
