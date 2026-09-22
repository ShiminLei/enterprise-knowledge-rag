package com.aishare.knowledgerag.ingestion;

import org.springframework.stereotype.Service;

@Service
public class DocumentIngestionPreviewService {

    private final DocumentProcessingService processingService;

    public DocumentIngestionPreviewService(DocumentProcessingService processingService) {
        this.processingService = processingService;
    }

    public IngestionPreview preview(String originalFileName, String mediaType, byte[] content) {
        ProcessedDocument processed = processingService.process(originalFileName, mediaType, content);
        ParsedDocument document = processed.document();
        int paragraphCount = document.sections().stream()
                .mapToInt(section -> section.paragraphs().size())
                .sum();

        return new IngestionPreview(
                document.title(),
                document.sourceFileName(),
                document.mediaType(),
                document.sections().size(),
                paragraphCount,
                processed.chunks().size(),
                processed.chunks()
        );
    }
}
