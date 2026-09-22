package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.chunking.ChunkingProperties;
import com.aishare.knowledgerag.chunking.SectionAwareTextChunker;
import com.aishare.knowledgerag.common.ApiExceptionHandler;
import com.aishare.knowledgerag.ingestion.DocumentCleaningPipeline;
import com.aishare.knowledgerag.ingestion.DocumentIngestionPreviewService;
import com.aishare.knowledgerag.ingestion.DocumentIngestionPreparationService;
import com.aishare.knowledgerag.ingestion.DocumentChecksumService;
import com.aishare.knowledgerag.ingestion.DocumentImportOutcome;
import com.aishare.knowledgerag.ingestion.DocumentImportResult;
import com.aishare.knowledgerag.ingestion.DocumentImportService;
import com.aishare.knowledgerag.ingestion.DocumentParserRegistry;
import com.aishare.knowledgerag.ingestion.DocumentProcessingService;
import com.aishare.knowledgerag.ingestion.MarkdownTextDocumentParser;
import com.aishare.knowledgerag.security.AuthenticatedIdentity;
import com.aishare.knowledgerag.security.CurrentAuthenticatedIdentityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentIngestionControllerTest {

    private MockMvc mockMvc;
    private DocumentImportService importService;

    @BeforeEach
    void setUp() {
        MarkdownTextDocumentParser parser =
                new MarkdownTextDocumentParser(new DocumentCleaningPipeline());
        DocumentProcessingService processingService = new DocumentProcessingService(
                new DocumentParserRegistry(List.of(parser)),
                new SectionAwareTextChunker(new ChunkingProperties(120, 20))
        );
        DocumentIngestionPreviewService service = new DocumentIngestionPreviewService(processingService);
        importService = mock(DocumentImportService.class);
        CurrentAuthenticatedIdentityProvider identityProvider =
                mock(CurrentAuthenticatedIdentityProvider.class);
        when(identityProvider.current()).thenReturn(new AuthenticatedIdentity(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "zhangsan"
        ));
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DocumentIngestionController(
                        service,
                        new DocumentIngestionPreparationService(
                                processingService,
                                new DocumentChecksumService()
                        ),
                        importService,
                        identityProvider
                ))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void previewsUploadedMarkdown() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vpn.md",
                "text/markdown",
                "# VPN 手册\n\n## 登录\n\n使用公司账号登录。"
                        .getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/documents/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("VPN 手册"))
                .andExpect(jsonPath("$.chunkCount").value(1))
                .andExpect(jsonPath("$.chunks[0].titlePath").value("VPN 手册 > 登录"));
    }

    @Test
    void returnsStructuredErrorForUnsupportedDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/documents/preview").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOCUMENT_PARSE_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/v1/documents/preview"));
    }

    @Test
    void preparesDocumentWithBusinessMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vpn.md",
                "text/markdown",
                "# VPN 手册\n\n## 登录\n\n使用公司账号登录。"
                        .getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "metadata.json",
                "application/json",
                """
                        {
                          "externalDocumentId": "IT-VPN-004",
                          "title": "VPN 手册",
                          "source": "IT 服务台",
                          "category": "MANUAL",
                          "version": "2.3",
                          "updatedAt": "2026-05-20T00:00:00Z",
                          "permissionLevel": "INTERNAL",
                          "department": "信息技术部"
                        }
                        """.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/documents/prepare")
                        .file(file)
                        .file(metadata))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.externalDocumentId").value("IT-VPN-004"))
                .andExpect(jsonPath("$.document.checksum").isNotEmpty())
                .andExpect(jsonPath("$.chunks[0].department").value("信息技术部"))
                .andExpect(jsonPath("$.chunks[0].permissionLevel").value("INTERNAL"));
    }

    @Test
    void returnsStructuredValidationErrorForMissingMetadataField() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vpn.md",
                "text/markdown",
                "# VPN 手册\n\n正文。".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "metadata.json",
                "application/json",
                """
                        {
                          "externalDocumentId": "IT-VPN-004",
                          "title": "VPN 手册",
                          "source": "IT 服务台",
                          "category": "MANUAL",
                          "version": "2.3",
                          "updatedAt": "2026-05-20T00:00:00Z",
                          "permissionLevel": "INTERNAL"
                        }
                        """.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/documents/prepare")
                        .file(file)
                        .file(metadata))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("department: 不能为空"));
    }

    @Test
    void importsDocumentAndReturnsCreated() throws Exception {
        when(importService.importDocument(any(), any(), any(), any()))
                .thenReturn(new DocumentImportResult(
                        DocumentImportOutcome.IMPORTED,
                        UUID.fromString("10000000-0000-0000-0000-000000000001"),
                        "abc123",
                        3,
                        "文档导入成功",
                        List.of()
                ));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vpn.md",
                "text/markdown",
                "# VPN 手册\n\n正文。".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile metadata = validMetadata();

        mockMvc.perform(multipart("/api/v1/documents/import")
                        .file(file)
                        .file(metadata))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("IMPORTED"))
                .andExpect(jsonPath("$.chunkCount").value(3));
    }

    private MockMultipartFile validMetadata() {
        return new MockMultipartFile(
                "metadata",
                "metadata.json",
                "application/json",
                """
                        {
                          "externalDocumentId": "IT-VPN-004",
                          "title": "VPN 手册",
                          "source": "IT 服务台",
                          "category": "MANUAL",
                          "version": "2.3",
                          "updatedAt": "2026-05-20T00:00:00Z",
                          "permissionLevel": "INTERNAL",
                          "department": "信息技术部"
                        }
                        """.getBytes(StandardCharsets.UTF_8)
        );
    }
}
