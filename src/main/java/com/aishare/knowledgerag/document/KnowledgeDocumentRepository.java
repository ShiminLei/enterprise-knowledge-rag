package com.aishare.knowledgerag.document;

import java.util.Optional;
import java.util.UUID;

public interface KnowledgeDocumentRepository {

    Optional<KnowledgeDocument> findByTenantIdAndChecksum(UUID tenantId, String checksum);

    Optional<KnowledgeDocument> findByTenantIdAndExternalDocumentIdAndVersion(
            UUID tenantId,
            String externalDocumentId,
            String version
    );

    boolean insert(KnowledgeDocument document);

    void updateStatus(UUID documentId, DocumentStatus status);
}
