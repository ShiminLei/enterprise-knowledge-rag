package com.aishare.knowledgerag.ingestion;

public class DocumentVersionConflictException extends RuntimeException {

    public DocumentVersionConflictException(String message) {
        super(message);
    }
}
