package com.aishare.knowledgerag.ingestion;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownTextDocumentParserTest {

    private final MarkdownTextDocumentParser parser =
            new MarkdownTextDocumentParser(new DocumentCleaningPipeline());

    @Test
    void parsesMarkdownFrontMatterAndHeadingHierarchy() {
        String markdown = """
                ---
                title: VPN 使用手册
                version: 2.0
                ---

                # VPN 使用手册

                总体说明。

                ## 无法连接

                检查系统时间。

                再次下载配置。
                """;

        ParsedDocument document = parser.parse(
                "vpn.md",
                "text/markdown; charset=UTF-8",
                markdown.getBytes(StandardCharsets.UTF_8)
        );

        assertThat(document.title()).isEqualTo("VPN 使用手册");
        assertThat(document.mediaType()).isEqualTo("text/markdown");
        assertThat(document.sections()).hasSize(2);
        assertThat(document.sections().get(0).titlePath()).isEqualTo("VPN 使用手册");
        assertThat(document.sections().get(1).titlePath()).isEqualTo("VPN 使用手册 > 无法连接");
        assertThat(document.sections().get(1).paragraphs())
                .containsExactly("检查系统时间。", "再次下载配置。");
        assertThat(document.fullText()).doesNotContain("version: 2.0");
    }

    @Test
    void parsesTextMetadataHeaderWithoutIndexingItAsBody() {
        String text = """
                documentId: IT-CRM-005
                title: CRM 系统常见问题
                version: 1.8

                问题：为什么看不到其他销售人员的客户？
                回答：普通销售只能查看本人负责的客户。
                """;

        ParsedDocument document = parser.parse(
                "crm-faq.txt",
                "text/plain",
                text.getBytes(StandardCharsets.UTF_8)
        );

        assertThat(document.title()).isEqualTo("CRM 系统常见问题");
        assertThat(document.sections()).singleElement()
                .satisfies(section -> {
                    assertThat(section.titlePath()).isEqualTo("CRM 系统常见问题");
                    assertThat(section.paragraphs()).singleElement()
                            .asString()
                            .doesNotContain("documentId:")
                            .contains("为什么看不到");
                });
    }

    @Test
    void rejectsUnsupportedOrEmptyDocuments() {
        assertThat(parser.supports("manual.pdf", "application/pdf")).isFalse();

        assertThatThrownBy(() -> parser.parse("manual.pdf", "application/pdf", new byte[]{1}))
                .isInstanceOf(DocumentParseException.class)
                .hasMessageContaining("暂不支持");

        assertThatThrownBy(() -> parser.parse("empty.txt", "text/plain", new byte[0]))
                .isInstanceOf(DocumentParseException.class)
                .hasMessageContaining("文档内容为空");
    }
}
