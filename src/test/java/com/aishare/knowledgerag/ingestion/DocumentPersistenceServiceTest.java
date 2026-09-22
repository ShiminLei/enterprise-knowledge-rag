package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.document.DocumentStatus;
import com.aishare.knowledgerag.document.KnowledgeChunk;
import com.aishare.knowledgerag.document.KnowledgeChunkRepository;
import com.aishare.knowledgerag.document.KnowledgeDocument;
import com.aishare.knowledgerag.document.KnowledgeDocumentRepository;
import com.aishare.knowledgerag.embedding.EmbeddedChunk;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentPersistenceServiceTest {

    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Mock
    private KnowledgeChunkRepository chunkRepository;

    private DocumentPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new DocumentPersistenceService(documentRepository, chunkRepository);
    }

    @Test
    void insertsDocumentAndChunksThenActivatesDocument() {
        PreparedIngestion prepared = preparedIngestion();
        KnowledgeDocument candidate = prepared.document();
        when(documentRepository.findByTenantIdAndChecksum(
                candidate.tenantId(), candidate.checksum()
        )).thenReturn(Optional.empty());
        when(documentRepository.findByTenantIdAndExternalDocumentIdAndVersion(
                candidate.tenantId(), candidate.externalDocumentId(), candidate.version()
        )).thenReturn(Optional.empty());
        when(documentRepository.insert(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        EmbeddedIngestion embedded = embeddedIngestion(prepared);
        DocumentImportResult result = service.persist(embedded);

        ArgumentCaptor<KnowledgeDocument> inserted = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(documentRepository).insert(inserted.capture());
        assertThat(inserted.getValue().status()).isEqualTo(DocumentStatus.PROCESSING);
        verify(chunkRepository).insertAll(embedded.chunks());
        verify(documentRepository).updateStatus(candidate.id(), DocumentStatus.ACTIVE);
        assertThat(result.outcome()).isEqualTo(DocumentImportOutcome.IMPORTED);
        assertThat(result.chunkCount()).isEqualTo(1);
    }

    @Test
    void treatsSameChecksumAsIdempotentDuplicate() {
        PreparedIngestion prepared = preparedIngestion();
        KnowledgeDocument existing = withStatus(prepared.document(), DocumentStatus.ACTIVE);
        when(documentRepository.findByTenantIdAndChecksum(
                existing.tenantId(), existing.checksum()
        )).thenReturn(Optional.of(existing));

        DocumentImportResult result = service.persist(embeddedIngestion(prepared));

        assertThat(result.outcome()).isEqualTo(DocumentImportOutcome.DUPLICATE);
        assertThat(result.documentId()).isEqualTo(existing.id());
        verify(documentRepository, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(chunkRepository, never()).insertAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsSameBusinessVersionWithDifferentContent() {
        PreparedIngestion prepared = preparedIngestion();
        KnowledgeDocument candidate = prepared.document();
        KnowledgeDocument existing = new KnowledgeDocument(
                UUID.randomUUID(),
                candidate.tenantId(),
                candidate.externalDocumentId(),
                candidate.title(),
                candidate.source(),
                "other.md",
                candidate.mediaType(),
                candidate.category(),
                candidate.version(),
                candidate.updatedAt(),
                candidate.permissionLevel(),
                candidate.department(),
                DocumentStatus.ACTIVE,
                "different-checksum"
        );
        when(documentRepository.findByTenantIdAndChecksum(
                candidate.tenantId(), candidate.checksum()
        )).thenReturn(Optional.empty());
        when(documentRepository.findByTenantIdAndExternalDocumentIdAndVersion(
                candidate.tenantId(), candidate.externalDocumentId(), candidate.version()
        )).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.persist(embeddedIngestion(prepared)))
                .isInstanceOf(DocumentVersionConflictException.class)
                .hasMessageContaining("版本 2.3 已存在");
        verify(documentRepository, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(chunkRepository, never()).insertAll(org.mockito.ArgumentMatchers.any());
    }

    private PreparedIngestion preparedIngestion() {
        UUID documentId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant updatedAt = Instant.parse("2026-05-20T00:00:00Z");
        KnowledgeDocument document = new KnowledgeDocument(
                documentId,
                tenantId,
                "IT-VPN-004",
                "VPN 手册",
                "IT 服务台",
                "vpn.md",
                "text/markdown",
                DocumentCategory.MANUAL,
                "2.3",
                updatedAt,
                PermissionLevel.INTERNAL,
                "信息技术部",
                DocumentStatus.PENDING,
                "checksum"
        );
        KnowledgeChunk chunk = new KnowledgeChunk(
                UUID.randomUUID(),
                documentId,
                tenantId,
                0,
                "使用公司账号登录。",
                "VPN 手册 > 登录",
                null,
                1,
                1,
                DocumentCategory.MANUAL,
                "2.3",
                updatedAt,
                PermissionLevel.INTERNAL,
                "信息技术部",
                "IT 服务台"
        );
        return new PreparedIngestion(document, "VPN 手册", 1, List.of(chunk), List.of());
    }

    private EmbeddedIngestion embeddedIngestion(PreparedIngestion prepared) {
        return new EmbeddedIngestion(
                prepared,
                prepared.chunks().stream()
                        .map(chunk -> new EmbeddedChunk(chunk, new float[1024]))
                        .toList()
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
