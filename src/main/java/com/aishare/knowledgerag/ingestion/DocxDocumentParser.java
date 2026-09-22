package com.aishare.knowledgerag.ingestion;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DocxDocumentParser implements DocumentParser {

    private static final String DOCX_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of(DOCX_MEDIA_TYPE);
    private static final Pattern HEADING_STYLE_PATTERN = Pattern.compile(
            "(?i)(?:heading|标题)\\s*([1-6])"
    );

    private final DocumentCleaningPipeline cleaningPipeline;

    public DocxDocumentParser(DocumentCleaningPipeline cleaningPipeline) {
        this.cleaningPipeline = cleaningPipeline;
    }

    @Override
    public boolean supports(String fileName, String mediaType) {
        return "docx".equals(extensionOf(fileName))
                || SUPPORTED_MEDIA_TYPES.contains(normalizeMediaType(mediaType));
    }

    @Override
    public ParsedDocument parse(String fileName, String mediaType, byte[] content) {
        if (!supports(fileName, mediaType)) {
            throw new DocumentParseException("暂不支持该 Word 文档格式: " + fileName);
        }
        if (content == null || content.length == 0) {
            throw new DocumentParseException("Word 文档内容为空: " + fileName);
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            List<String> headingStack = new ArrayList<>();
            List<String> currentParagraphs = new ArrayList<>();
            List<RawSection> rawSections = new ArrayList<>();
            String documentTitle = null;

            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = cleaningPipeline.clean(paragraph.getText());
                    if (text.isBlank()) {
                        continue;
                    }
                    int headingLevel = headingLevel(paragraph.getStyle());
                    if (headingLevel > 0) {
                        flushSection(rawSections, headingStack, currentParagraphs);
                        updateHeadingStack(headingStack, headingLevel, text);
                        if (headingLevel == 1 && documentTitle == null) {
                            documentTitle = text;
                        }
                    } else {
                        currentParagraphs.add(text);
                    }
                } else if (element instanceof XWPFTable table) {
                    String tableText = extractTable(table);
                    if (!tableText.isBlank()) {
                        currentParagraphs.add(tableText);
                    }
                }
            }
            flushSection(rawSections, headingStack, currentParagraphs);

            String resolvedTitle = documentTitle == null ? baseName(fileName) : documentTitle;
            List<ParsedSection> sections = rawSections.stream()
                    .map(section -> new ParsedSection(
                            section.titlePath().isBlank() ? resolvedTitle : section.titlePath(),
                            null,
                            section.paragraphs()
                    ))
                    .toList();
            if (sections.isEmpty()) {
                throw new DocumentParseException("Word 文档中没有可索引的正文: " + fileName);
            }

            return new ParsedDocument(resolvedTitle, fileName, DOCX_MEDIA_TYPE, sections);
        } catch (DocumentParseException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new DocumentParseException("Word 文档解析失败: " + fileName, exception);
        }
    }

    private int headingLevel(String style) {
        if (style == null) {
            return 0;
        }
        Matcher matcher = HEADING_STYLE_PATTERN.matcher(style.replace('_', ' '));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private void updateHeadingStack(List<String> headingStack, int level, String title) {
        while (headingStack.size() >= level) {
            headingStack.remove(headingStack.size() - 1);
        }
        while (headingStack.size() < level - 1) {
            headingStack.add("");
        }
        headingStack.add(title);
    }

    private void flushSection(
            List<RawSection> sections,
            List<String> headingStack,
            List<String> paragraphs
    ) {
        if (paragraphs.isEmpty()) {
            return;
        }
        String titlePath = headingStack.stream()
                .filter(heading -> !heading.isBlank())
                .reduce((left, right) -> left + " > " + right)
                .orElse("");
        sections.add(new RawSection(titlePath, List.copyOf(paragraphs)));
        paragraphs.clear();
    }

    private String extractTable(XWPFTable table) {
        return table.getRows().stream()
                .map(row -> row.getTableCells().stream()
                        .map(cell -> cleaningPipeline.clean(cell.getText()))
                        .reduce((left, right) -> left + " | " + right)
                        .orElse(""))
                .filter(row -> !row.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .map(value -> "[表格]\n" + value)
                .orElse("");
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
            return "未命名 Word 文档";
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

    private record RawSection(String titlePath, List<String> paragraphs) {
    }
}
