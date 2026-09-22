package com.aishare.knowledgerag.ingestion;

import org.springframework.stereotype.Service;

@Service
public class DocumentImportService {

    private final DocumentIngestionPreparationService preparationService;
    private final DocumentPersistenceService persistenceService;

    public DocumentImportService(
            DocumentIngestionPreparationService preparationService,
            DocumentPersistenceService persistenceService
    ) {
        this.preparationService = preparationService;
        this.persistenceService = persistenceService;
    }

    public DocumentImportResult importDocument(
            String originalFileName,
            String mediaType,
            byte[] content,
            DocumentImportMetadata metadata
    ) {
        PreparedIngestion prepared = preparationService.prepare(
                originalFileName,
                mediaType,
                content,
                metadata
        );
        return persistenceService.persist(prepared);
    }
}
