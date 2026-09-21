package com.aishare.knowledgerag.ingestion;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentCleaningPipeline {

    public String clean(String rawText) {
        if (rawText == null) {
            throw new DocumentParseException("文档内容不能为 null");
        }

        String normalized = Normalizer.normalize(rawText, Normalizer.Form.NFKC)
                .replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        StringBuilder printable = new StringBuilder(normalized.length());
        normalized.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint)
                        || codePoint == '\n'
                        || codePoint == '\t')
                .forEach(printable::appendCodePoint);

        List<String> cleanedLines = new ArrayList<>();
        boolean previousLineWasBlank = false;
        for (String line : printable.toString().split("\n", -1)) {
            String cleanedLine = line
                    .replace('\t', ' ')
                    .replaceAll("[\\p{Zs} ]+", " ")
                    .strip();
            boolean blank = cleanedLine.isBlank();
            if (!blank || !previousLineWasBlank) {
                cleanedLines.add(cleanedLine);
            }
            previousLineWasBlank = blank;
        }

        return String.join("\n", cleanedLines).strip();
    }
}
