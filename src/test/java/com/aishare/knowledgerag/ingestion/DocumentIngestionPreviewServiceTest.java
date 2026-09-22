package com.aishare.knowledgerag.ingestion;

import com.aishare.knowledgerag.chunking.ChunkingProperties;
import com.aishare.knowledgerag.chunking.SectionAwareTextChunker;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentIngestionPreviewServiceTest {

    private final MarkdownTextDocumentParser parser =
            new MarkdownTextDocumentParser(new DocumentCleaningPipeline());
    private final DocumentIngestionPreviewService service = new DocumentIngestionPreviewService(
            new DocumentProcessingService(
                    new DocumentParserRegistry(List.of(parser)),
                    new SectionAwareTextChunker(new ChunkingProperties(120, 20))
            )
    );

    @Test
    void orchestratesParsingCleaningAndChunking() {
        String markdown = """
                ---
                title: 差旅制度
                ---

                # 差旅制度

                ## 住宿标准

                一线城市住宿标准为每天六百元。超出标准需要事前审批。

                ## 报销时限

                差旅结束后三十个自然日内提交报销。
                """;

        IngestionPreview preview = service.preview(
                "travel.md",
                "text/markdown",
                markdown.getBytes(StandardCharsets.UTF_8)
        );

        assertThat(preview.title()).isEqualTo("差旅制度");
        assertThat(preview.sectionCount()).isEqualTo(2);
        assertThat(preview.paragraphCount()).isEqualTo(2);
        assertThat(preview.chunkCount()).isEqualTo(2);
        assertThat(preview.chunks()).extracting("titlePath")
                .containsExactly("差旅制度 > 住宿标准", "差旅制度 > 报销时限");
    }

    @Test
    void rejectsUnsafeFileName() {
        assertThatThrownBy(() -> service.preview(
                "../secret.txt",
                "text/plain",
                "content".getBytes(StandardCharsets.UTF_8)
        ))
                .isInstanceOf(DocumentParseException.class)
                .hasMessageContaining("非法路径");
    }
}
