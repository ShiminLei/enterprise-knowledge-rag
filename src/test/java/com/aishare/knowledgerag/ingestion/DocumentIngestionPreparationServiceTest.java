package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.chunking.ChunkingProperties;
import com.aishare.knowledgerag.chunking.SectionAwareTextChunker;
import com.aishare.knowledgerag.document.DocumentCategory;
import com.aishare.knowledgerag.document.DocumentStatus;
import com.aishare.knowledgerag.security.PermissionLevel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentIngestionPreparationServiceTest {

    private final MarkdownTextDocumentParser parser =
            new MarkdownTextDocumentParser(new DocumentCleaningPipeline());
    private final DocumentProcessingService processingService = new DocumentProcessingService(
            new DocumentParserRegistry(List.of(parser)),
            new SectionAwareTextChunker(new ChunkingProperties(120, 20))
    );
    private final DocumentIngestionPreparationService service =
            new DocumentIngestionPreparationService(
                    processingService,
                    new DocumentChecksumService()
            );

    @Test
    void bindsBusinessMetadataToDocumentAndEveryChunk() {
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant updatedAt = Instant.parse("2026-05-20T00:00:00Z");
        DocumentImportMetadata metadata = new DocumentImportMetadata(
                tenantId,
                "IT-VPN-004",
                "企业 VPN 使用手册",
                "IT 服务台/远程办公",
                DocumentCategory.MANUAL,
                "2.3",
                updatedAt,
                PermissionLevel.INTERNAL,
                "信息技术部"
        );
        byte[] content = "# 企业 VPN 使用手册\n\n## 登录\n\n使用公司账号登录。"
                .getBytes(StandardCharsets.UTF_8);

        PreparedIngestion prepared = service.prepare(
                "vpn.md",
                "text/markdown",
                content,
                metadata
        );

        assertThat(prepared.document().tenantId()).isEqualTo(tenantId);
        assertThat(prepared.document().externalDocumentId()).isEqualTo("IT-VPN-004");
        assertThat(prepared.document().status()).isEqualTo(DocumentStatus.PENDING);
        assertThat(prepared.document().checksum()).hasSize(64);
        assertThat(prepared.warnings()).isEmpty();
        assertThat(prepared.chunks()).isNotEmpty().allSatisfy(chunk -> {
            assertThat(chunk.documentId()).isEqualTo(prepared.document().id());
            assertThat(chunk.tenantId()).isEqualTo(tenantId);
            assertThat(chunk.permissionLevel()).isEqualTo(PermissionLevel.INTERNAL);
            assertThat(chunk.department()).isEqualTo("信息技术部");
            assertThat(chunk.documentVersion()).isEqualTo("2.3");
        });
    }

    @Test
    void warnsWhenDeclaredAndParsedTitlesDiffer() {
        DocumentImportMetadata metadata = new DocumentImportMetadata(
                UUID.randomUUID(),
                "IT-VPN-004",
                "管理员填写的标题",
                "IT 服务台",
                DocumentCategory.MANUAL,
                "1.0",
                Instant.parse("2026-01-01T00:00:00Z"),
                PermissionLevel.INTERNAL,
                "信息技术部"
        );

        PreparedIngestion prepared = service.prepare(
                "vpn.md",
                "text/markdown",
                "# 文档中的标题\n\n正文。".getBytes(StandardCharsets.UTF_8),
                metadata
        );

        assertThat(prepared.parsedTitle()).isEqualTo("文档中的标题");
        assertThat(prepared.warnings()).containsExactly(
                "元数据标题与文档解析标题不一致，请确认版本和来源"
        );
    }
}
