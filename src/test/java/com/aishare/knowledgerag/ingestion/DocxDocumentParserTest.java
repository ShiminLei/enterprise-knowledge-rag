package com.aishare.knowledgerag.ingestion;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocxDocumentParserTest {

    private final DocxDocumentParser parser =
            new DocxDocumentParser(new DocumentCleaningPipeline());

    @Test
    void extractsHeadingHierarchyParagraphsAndTables() throws IOException {
        byte[] docx = createDocx();

        ParsedDocument document = parser.parse(
                "expense-policy.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docx
        );

        assertThat(document.title()).isEqualTo("差旅费制度");
        assertThat(document.sections()).hasSize(2);
        assertThat(document.sections().get(0).titlePath()).isEqualTo("差旅费制度 > 住宿费");
        assertThat(document.sections().get(0).paragraphs()).contains("一线城市每天不超过 600 元。");
        assertThat(document.sections().get(1).titlePath()).isEqualTo("差旅费制度 > 交通费");
        assertThat(document.sections().get(1).paragraphs())
                .anySatisfy(text -> assertThat(text)
                        .contains("[表格]")
                        .contains("普通员工 | 二等座"));
    }

    @Test
    void rejectsBrokenDocx() {
        assertThatThrownBy(() -> parser.parse(
                "broken.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{1, 2, 3}
        ))
                .isInstanceOf(DocumentParseException.class)
                .hasMessageContaining("Word 文档解析失败");
    }

    @Test
    void doesNotClaimToSupportLegacyDocFormat() {
        assertThat(parser.supports("legacy.doc", "application/msword")).isFalse();
    }

    private byte[] createDocx() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            heading(document, "差旅费制度", "Heading1");
            heading(document, "住宿费", "Heading2");
            document.createParagraph().createRun().setText("一线城市每天不超过 600 元。");

            heading(document, "交通费", "Heading2");
            XWPFTable table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("人员");
            table.getRow(0).getCell(1).setText("高铁标准");
            table.getRow(1).getCell(0).setText("普通员工");
            table.getRow(1).getCell(1).setText("二等座");

            document.write(output);
            return output.toByteArray();
        }
    }

    private void heading(XWPFDocument document, String text, String style) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setStyle(style);
        paragraph.createRun().setText(text);
    }
}
