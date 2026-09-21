package com.aishare.knowledgerag.ingestion;

public enum IngestionStatus {
    QUEUED,
    PARSING,
    CHUNKING,
    EMBEDDING,
    INDEXING,
    COMPLETED,
    FAILED
}
