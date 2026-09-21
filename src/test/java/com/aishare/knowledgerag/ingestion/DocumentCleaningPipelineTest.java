package com.aishare.knowledgerag.ingestion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentCleaningPipelineTest {

    private final DocumentCleaningPipeline pipeline = new DocumentCleaningPipeline();

    @Test
    void normalizesWhitespaceLineBreaksAndControlCharacters() {
        String raw = "\uFEFF标题\r\n\r\n\r\n正文\t  内容\u0000\r下一行";

        String cleaned = pipeline.clean(raw);

        assertThat(cleaned).isEqualTo("标题\n\n正文 内容\n下一行");
    }

    @Test
    void rejectsNullContent() {
        assertThatThrownBy(() -> pipeline.clean(null))
                .isInstanceOf(DocumentParseException.class)
                .hasMessage("文档内容不能为 null");
    }
}
