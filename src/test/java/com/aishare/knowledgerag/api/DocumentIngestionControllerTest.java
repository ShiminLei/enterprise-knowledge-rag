package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.chunking.ChunkingProperties;
import com.aishare.knowledgerag.chunking.SectionAwareTextChunker;
import com.aishare.knowledgerag.common.ApiExceptionHandler;
import com.aishare.knowledgerag.ingestion.DocumentCleaningPipeline;
import com.aishare.knowledgerag.ingestion.DocumentIngestionPreviewService;
import com.aishare.knowledgerag.ingestion.DocumentParserRegistry;
import com.aishare.knowledgerag.ingestion.MarkdownTextDocumentParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentIngestionControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MarkdownTextDocumentParser parser =
                new MarkdownTextDocumentParser(new DocumentCleaningPipeline());
        DocumentIngestionPreviewService service = new DocumentIngestionPreviewService(
                new DocumentParserRegistry(List.of(parser)),
                new SectionAwareTextChunker(new ChunkingProperties(120, 20))
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DocumentIngestionController(service))
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
}
