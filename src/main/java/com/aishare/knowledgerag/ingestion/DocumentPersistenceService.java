package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.document.DocumentStatus;
import com.aishare.knowledgerag.document.KnowledgeChunkRepository;
import com.aishare.knowledgerag.document.KnowledgeDocument;
import com.aishare.knowledgerag.document.KnowledgeDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class DocumentPersistenceService {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;

    public DocumentPersistenceService(
            KnowledgeDocumentRepository documentRepository,
            KnowledgeChunkRepository chunkRepository
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    @Transactional
    public DocumentImportResult persist(PreparedIngestion prepared) {
        KnowledgeDocument candidate = prepared.document();
        Optional<KnowledgeDocument> sameContent = documentRepository.findByTenantIdAndChecksum(
                candidate.tenantId(),
                candidate.checksum()
        );
        if (sameContent.isPresent()) {
            return duplicateResult(sameContent.get(), prepared);
        }

        documentRepository.findByTenantIdAndExternalDocumentIdAndVersion(
                candidate.tenantId(),
                candidate.externalDocumentId(),
                candidate.version()
        ).ifPresent(existing -> {
            throw new DocumentVersionConflictException(
                    "文档 %s 的版本 %s 已存在，但文件内容不同"
                            .formatted(candidate.externalDocumentId(), candidate.version())
            );
        });

        KnowledgeDocument processing = withStatus(candidate, DocumentStatus.PROCESSING);
        if (!documentRepository.insert(processing)) {
            Optional<KnowledgeDocument> concurrentDuplicate =
                    documentRepository.findByTenantIdAndChecksum(
                            candidate.tenantId(),
                            candidate.checksum()
                    );
            if (concurrentDuplicate.isPresent()) {
                return duplicateResult(concurrentDuplicate.get(), prepared);
            }
            throw new DocumentVersionConflictException("文档被其他请求并发导入，请重新查询文档状态");
        }

        chunkRepository.insertAll(prepared.chunks());
        documentRepository.updateStatus(candidate.id(), DocumentStatus.ACTIVE);
        return new DocumentImportResult(
                DocumentImportOutcome.IMPORTED,
                candidate.id(),
                candidate.checksum(),
                prepared.chunks().size(),
                "文档导入成功",
                prepared.warnings()
        );
    }

    private DocumentImportResult duplicateResult(
            KnowledgeDocument existing,
            PreparedIngestion prepared
    ) {
        return new DocumentImportResult(
                DocumentImportOutcome.DUPLICATE,
                existing.id(),
                existing.checksum(),
                0,
                "相同文件已经导入，本次未重复写入",
                prepared.warnings()
        );
    }

    private KnowledgeDocument withStatus(KnowledgeDocument document, DocumentStatus status) {
        return new KnowledgeDocument(
                document.id(),
                document.tenantId(),
                document.externalDocumentId(),
                document.title(),
                document.source(),
                document.fileName(),
                document.mediaType(),
                document.category(),
                document.version(),
                document.updatedAt(),
                document.permissionLevel(),
                document.department(),
                status,
                document.checksum()
        );
    }
}
