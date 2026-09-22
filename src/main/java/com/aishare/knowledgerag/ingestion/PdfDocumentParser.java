package com.aishare.knowledgerag.ingestion;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class PdfDocumentParser implements DocumentParser {

    private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of(
            "application/pdf",
            "application/x-pdf"
    );

    private final DocumentCleaningPipeline cleaningPipeline;

    public PdfDocumentParser(DocumentCleaningPipeline cleaningPipeline) {
        this.cleaningPipeline = cleaningPipeline;
    }

    @Override
    public boolean supports(String fileName, String mediaType) {
        return "pdf".equals(extensionOf(fileName))
                || SUPPORTED_MEDIA_TYPES.contains(normalizeMediaType(mediaType));
    }

    @Override
    public ParsedDocument parse(String fileName, String mediaType, byte[] content) {
        if (!supports(fileName, mediaType)) {
            throw new DocumentParseException("暂不支持该 PDF 文档格式: " + fileName);
        }
        if (content == null || content.length == 0) {
            throw new DocumentParseException("PDF 文档内容为空: " + fileName);
        }

        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String title = resolveTitle(document, fileName);
            List<ParsedSection> sections = new ArrayList<>();

            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                int pageNumber = pageIndex + 1;
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String pageText = cleaningPipeline.clean(stripper.getText(document));
                if (pageText.isBlank()) {
                    continue;
                }
                sections.add(new ParsedSection(
                        title + " > 第 " + pageNumber + " 页",
                        pageNumber,
                        splitParagraphs(pageText)
                ));
            }

            if (sections.isEmpty()) {
                throw new DocumentParseException(
                        "PDF 没有可提取的文本，可能是扫描件，需要 OCR: " + fileName
                );
            }
            return new ParsedDocument(title, fileName, "application/pdf", sections);
        } catch (DocumentParseException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new DocumentParseException("PDF 解析失败: " + fileName, exception);
        }
    }

    private String resolveTitle(PDDocument document, String fileName) {
        String metadataTitle = document.getDocumentInformation().getTitle();
        if (metadataTitle != null && !metadataTitle.isBlank()) {
            return cleaningPipeline.clean(metadataTitle);
        }
        return baseName(fileName);
    }

    private List<String> splitParagraphs(String text) {
        return Arrays.stream(text.split("\\n\\s*\\n"))
                .map(String::strip)
                .filter(paragraph -> !paragraph.isBlank())
                .toList();
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int separator = fileName.lastIndexOf('.');
        return separator >= 0 && separator < fileName.length() - 1
                ? fileName.substring(separator + 1).toLowerCase(Locale.ROOT)
                : "";
    }

    private String baseName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "未命名 PDF";
        }
        String normalized = fileName.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);
        int separator = name.lastIndexOf('.');
        return separator > 0 ? name.substring(0, separator) : name;
    }

    private String normalizeMediaType(String mediaType) {
        if (mediaType == null) {
            return "";
        }
        int separator = mediaType.indexOf(';');
        return (separator >= 0 ? mediaType.substring(0, separator) : mediaType)
                .strip()
                .toLowerCase(Locale.ROOT);
    }
}
