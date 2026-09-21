package com.aishare.knowledgerag.ingestion;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MarkdownTextDocumentParser implements DocumentParser {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("md", "markdown", "txt");
    private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of(
            "text/markdown",
            "text/plain",
            "text/x-markdown"
    );
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*#*\\s*$");
    private static final Pattern TITLE_METADATA_PATTERN = Pattern.compile("(?mi)^title\\s*:\\s*[\\\"']?(.+?)[\\\"']?\\s*$");
    private static final Pattern METADATA_LINE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]*\\s*:\\s*.+$");

    private final DocumentCleaningPipeline cleaningPipeline;

    public MarkdownTextDocumentParser(DocumentCleaningPipeline cleaningPipeline) {
        this.cleaningPipeline = cleaningPipeline;
    }

    @Override
    public boolean supports(String fileName, String mediaType) {
        String extension = extensionOf(fileName);
        String normalizedMediaType = normalizeMediaType(mediaType);
        return SUPPORTED_EXTENSIONS.contains(extension)
                || SUPPORTED_MEDIA_TYPES.contains(normalizedMediaType);
    }

    @Override
    public ParsedDocument parse(String fileName, String mediaType, byte[] content) {
        if (!supports(fileName, mediaType)) {
            throw new DocumentParseException("暂不支持该文档格式: " + fileName);
        }
        if (content == null || content.length == 0) {
            throw new DocumentParseException("文档内容为空: " + fileName);
        }

        String rawText = decodeUtf8(fileName, content);
        String cleanedText = cleaningPipeline.clean(rawText);
        if (cleanedText.isBlank()) {
            throw new DocumentParseException("文档清洗后没有有效内容: " + fileName);
        }

        String title = extractMetadataTitle(cleanedText);
        String body = stripLeadingMetadata(cleanedText);
        List<ParsedSection> sections = parseSections(body, title, fileName);
        if (sections.isEmpty()) {
            throw new DocumentParseException("文档中没有可索引的正文: " + fileName);
        }

        String resolvedTitle = title;
        if (resolvedTitle == null || resolvedTitle.isBlank()) {
            resolvedTitle = sections.get(0).titlePath();
        }
        if (resolvedTitle == null || resolvedTitle.isBlank()) {
            resolvedTitle = baseName(fileName);
        }

        return new ParsedDocument(
                resolvedTitle,
                fileName,
                normalizeMediaType(mediaType),
                sections
        );
    }

    private String decodeUtf8(String fileName, byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new DocumentParseException("文档不是有效的 UTF-8 编码: " + fileName, exception);
        }
    }

    private List<ParsedSection> parseSections(String body, String metadataTitle, String fileName) {
        List<ParsedSection> sections = new ArrayList<>();
        List<String> headingStack = new ArrayList<>();
        List<String> paragraphs = new ArrayList<>();
        StringBuilder currentParagraph = new StringBuilder();
        String fallbackTitle = metadataTitle == null || metadataTitle.isBlank()
                ? baseName(fileName)
                : metadataTitle;

        for (String line : body.split("\n", -1)) {
            Matcher headingMatcher = HEADING_PATTERN.matcher(line);
            if (headingMatcher.matches()) {
                flushParagraph(currentParagraph, paragraphs);
                flushSection(sections, headingStack, fallbackTitle, paragraphs);

                int level = headingMatcher.group(1).length();
                while (headingStack.size() >= level) {
                    headingStack.remove(headingStack.size() - 1);
                }
                while (headingStack.size() < level - 1) {
                    headingStack.add("");
                }
                headingStack.add(headingMatcher.group(2).strip());
                continue;
            }

            if (line.isBlank()) {
                flushParagraph(currentParagraph, paragraphs);
            } else {
                if (!currentParagraph.isEmpty()) {
                    currentParagraph.append('\n');
                }
                currentParagraph.append(line);
            }
        }

        flushParagraph(currentParagraph, paragraphs);
        flushSection(sections, headingStack, fallbackTitle, paragraphs);
        return List.copyOf(sections);
    }

    private void flushParagraph(StringBuilder currentParagraph, List<String> paragraphs) {
        if (!currentParagraph.isEmpty()) {
            paragraphs.add(currentParagraph.toString().strip());
            currentParagraph.setLength(0);
        }
    }

    private void flushSection(
            List<ParsedSection> sections,
            List<String> headingStack,
            String fallbackTitle,
            List<String> paragraphs
    ) {
        if (paragraphs.isEmpty()) {
            return;
        }
        String titlePath = headingStack.stream()
                .filter(heading -> !heading.isBlank())
                .reduce((left, right) -> left + " > " + right)
                .orElse(fallbackTitle);
        sections.add(new ParsedSection(titlePath, null, List.copyOf(paragraphs)));
        paragraphs.clear();
    }

    private String extractMetadataTitle(String text) {
        Matcher matcher = TITLE_METADATA_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1).strip() : null;
    }

    private String stripLeadingMetadata(String text) {
        List<String> lines = new ArrayList<>(Arrays.asList(text.split("\n", -1)));
        if (lines.isEmpty()) {
            return text;
        }

        if (lines.get(0).equals("---")) {
            for (int index = 1; index < lines.size(); index++) {
                if (lines.get(index).equals("---")) {
                    return String.join("\n", lines.subList(index + 1, lines.size())).strip();
                }
            }
        }

        int metadataLines = 0;
        while (metadataLines < lines.size()
                && METADATA_LINE_PATTERN.matcher(lines.get(metadataLines)).matches()) {
            metadataLines++;
        }
        if (metadataLines > 0
                && metadataLines < lines.size()
                && lines.get(metadataLines).isBlank()) {
            return String.join("\n", lines.subList(metadataLines + 1, lines.size())).strip();
        }
        return text;
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int extensionSeparator = fileName.lastIndexOf('.');
        if (extensionSeparator < 0 || extensionSeparator == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
    }

    private String baseName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "未命名文档";
        }
        String normalized = fileName.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);
        int extensionSeparator = name.lastIndexOf('.');
        return extensionSeparator > 0 ? name.substring(0, extensionSeparator) : name;
    }

    private String normalizeMediaType(String mediaType) {
        if (mediaType == null) {
            return "application/octet-stream";
        }
        int parameterSeparator = mediaType.indexOf(';');
        String normalized = parameterSeparator >= 0
                ? mediaType.substring(0, parameterSeparator)
                : mediaType;
        return normalized.strip().toLowerCase(Locale.ROOT);
    }
}
