package com.aishare.knowledgerag.ingestion;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfDocumentParserTest {

    private final PdfDocumentParser parser =
            new PdfDocumentParser(new DocumentCleaningPipeline());

    @Test
    void extractsTextAndPreservesPageNumbers() throws IOException {
        byte[] pdf = createTwoPagePdf();

        ParsedDocument document = parser.parse("vpn-guide.pdf", "application/pdf", pdf);

        assertThat(document.title()).isEqualTo("VPN Guide");
        assertThat(document.mediaType()).isEqualTo("application/pdf");
        assertThat(document.sections()).hasSize(2);
        assertThat(document.sections()).extracting(ParsedSection::pageNumber)
                .containsExactly(1, 2);
        assertThat(document.sections().get(0).paragraphs())
                .anySatisfy(text -> assertThat(text).contains("Connect with company account"));
        assertThat(document.sections().get(1).paragraphs())
                .anySatisfy(text -> assertThat(text).contains("Error E401"));
    }

    @Test
    void rejectsPdfWithoutExtractableText() throws IOException {
        byte[] pdf;
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            pdf = output.toByteArray();
        }

        assertThatThrownBy(() -> parser.parse("scan.pdf", "application/pdf", pdf))
                .isInstanceOf(DocumentParseException.class)
                .hasMessageContaining("需要 OCR");
    }

    private byte[] createTwoPagePdf() throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDDocumentInformation information = new PDDocumentInformation();
            information.setTitle("VPN Guide");
            document.setDocumentInformation(information);
            addPage(document, "Connect with company account.");
            addPage(document, "Error E401 means authentication failed.");
            document.save(output);
            return output.toByteArray();
        }
    }

    private void addPage(PDDocument document, String text) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.newLineAtOffset(72, 720);
            contentStream.showText(text);
            contentStream.endText();
        }
    }
}
