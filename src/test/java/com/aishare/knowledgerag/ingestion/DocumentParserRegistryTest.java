package com.aishare.knowledgerag.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentParserRegistryTest {

    private final MarkdownTextDocumentParser parser =
            new MarkdownTextDocumentParser(new DocumentCleaningPipeline());
    private final DocumentParserRegistry registry = new DocumentParserRegistry(List.of(parser));

    @Test
    void selectsParserByExtensionOrMediaType() {
        assertThat(registry.requireParser("policy.md", "application/octet-stream"))
                .isSameAs(parser);
        assertThat(registry.requireParser("no-extension", "text/plain"))
                .isSameAs(parser);
    }

    @Test
    void rejectsUnsupportedDocument() {
        assertThatThrownBy(() -> registry.requireParser("manual.pdf", "application/pdf"))
                .isInstanceOf(DocumentParseException.class)
                .hasMessageContaining("没有可用的文档解析器");
    }
}
