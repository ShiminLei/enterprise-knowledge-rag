package com.aishare.knowledgerag.ingestion;

import java.util.List;
import java.util.UUID;

public record DocumentImportResult(
        DocumentImportOutcome outcome,
        UUID documentId,
        String checksum,
        int chunkCount,
        String message,
        List<String> warnings
) {
    public DocumentImportResult {
        warnings = List.copyOf(warnings);
    }
}
