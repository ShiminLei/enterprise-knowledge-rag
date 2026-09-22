package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.document.DocumentStatus;
import com.aishare.knowledgerag.document.KnowledgeChunk;
import com.aishare.knowledgerag.document.KnowledgeDocument;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentIngestionPreparationService {

    private final DocumentProcessingService processingService;
    private final DocumentChecksumService checksumService;

    public DocumentIngestionPreparationService(
            DocumentProcessingService processingService,
            DocumentChecksumService checksumService
    ) {
        this.processingService = processingService;
        this.checksumService = checksumService;
    }

    public PreparedIngestion prepare(
            String originalFileName,
            String mediaType,
            byte[] content,
            DocumentImportMetadata metadata
    ) {
        ProcessedDocument processed = processingService.process(originalFileName, mediaType, content);
        UUID documentId = UUID.randomUUID();
        String checksum = checksumService.sha256(content);
        KnowledgeDocument document = new KnowledgeDocument(
                documentId,
                metadata.tenantId(),
                metadata.externalDocumentId(),
                metadata.title(),
                metadata.source(),
                processed.document().sourceFileName(),
                processed.document().mediaType(),
                metadata.category(),
                metadata.version(),
                metadata.updatedAt(),
                metadata.permissionLevel(),
                metadata.department(),
                DocumentStatus.PENDING,
                checksum
        );

        List<KnowledgeChunk> chunks = processed.chunks().stream()
                .map(candidate -> new KnowledgeChunk(
                        UUID.randomUUID(),
                        documentId,
                        metadata.tenantId(),
                        candidate.chunkIndex(),
                        candidate.content(),
                        candidate.titlePath(),
                        candidate.pageNumber(),
                        candidate.startParagraphNumber(),
                        candidate.endParagraphNumber(),
                        metadata.category(),
                        metadata.version(),
                        metadata.updatedAt(),
                        metadata.permissionLevel(),
                        metadata.department(),
                        metadata.source()
                ))
                .toList();

        List<String> warnings = new ArrayList<>();
        if (!metadata.title().equals(processed.document().title())) {
            warnings.add("元数据标题与文档解析标题不一致，请确认版本和来源");
        }

        return new PreparedIngestion(
                document,
                processed.document().title(),
                processed.document().sections().size(),
                chunks,
                warnings
        );
    }
}
