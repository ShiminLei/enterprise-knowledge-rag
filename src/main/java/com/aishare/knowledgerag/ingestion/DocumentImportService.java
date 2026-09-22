package com.aishare.knowledgerag.ingestion;

import org.springframework.stereotype.Service;

@Service
public class DocumentImportService {

    private final DocumentIngestionPreparationService preparationService;
    private final DocumentEmbeddingService embeddingService;
    private final DocumentPersistenceService persistenceService;

    public DocumentImportService(
            DocumentIngestionPreparationService preparationService,
            DocumentEmbeddingService embeddingService,
            DocumentPersistenceService persistenceService
    ) {
        this.preparationService = preparationService;
        this.embeddingService = embeddingService;
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
        EmbeddedIngestion embedded = embeddingService.embed(prepared);
        return persistenceService.persist(embedded);
    }
}
