package com.aishare.knowledgerag.retrieval;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Okapi BM25：中文使用相邻双字，英文和数字使用词元。 */
@Component
public class Bm25Scorer {

    private static final double K1 = 1.2;
    private static final double B = 0.75;
    private static final Pattern TOKEN_GROUP = Pattern.compile("[\\p{IsHan}]+|[\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);

    public List<RetrievedChunk> rank(
            String question,
            List<RetrievedChunk> corpus,
            int topK,
            double minScore
    ) {
        List<String> queryTerms = tokens(question);
        if (queryTerms.isEmpty() || corpus.isEmpty()) {
            return List.of();
        }
        List<DocumentTerms> documents = corpus.stream().map(this::documentTerms).toList();
        double averageLength = documents.stream()
                .mapToInt(document -> document.terms().size())
                .average().orElse(1.0);
        Set<String> uniqueQueryTerms = new HashSet<>(queryTerms);
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (String term : uniqueQueryTerms) {
            int count = (int) documents.stream()
                    .filter(document -> document.frequencies().containsKey(term))
                    .count();
            documentFrequency.put(term, count);
        }
        int documentCount = documents.size();
        return documents.stream()
                .map(document -> score(document, uniqueQueryTerms, documentFrequency,
                        documentCount, averageLength))
                .filter(chunk -> chunk.score() >= minScore)
                .sorted(java.util.Comparator.comparingDouble(RetrievedChunk::score).reversed()
                        .thenComparing(RetrievedChunk::chunkIndex))
                .limit(topK)
                .toList();
    }

    private DocumentTerms documentTerms(RetrievedChunk chunk) {
        List<String> terms = new ArrayList<>(tokens(chunk.content()));
        // 标题重复一次形成可解释的字段权重，但仍使用同一个 BM25 公式。
        terms.addAll(tokens(chunk.titlePath()));
        terms.addAll(tokens(chunk.titlePath()));
        Map<String, Integer> frequencies = new HashMap<>();
        terms.forEach(term -> frequencies.merge(term, 1, Integer::sum));
        return new DocumentTerms(chunk, terms, frequencies);
    }

    private RetrievedChunk score(
            DocumentTerms document,
            Set<String> queryTerms,
            Map<String, Integer> documentFrequency,
            int documentCount,
            double averageLength
    ) {
        double raw = 0;
        int matchedTerms = 0;
        for (String term : queryTerms) {
            int frequency = document.frequencies().getOrDefault(term, 0);
            if (frequency == 0) {
                continue;
            }
            matchedTerms++;
            int containingDocuments = documentFrequency.getOrDefault(term, 0);
            double idf = Math.log(1 + (documentCount - containingDocuments + 0.5)
                    / (containingDocuments + 0.5));
            double denominator = frequency + K1 * (1 - B
                    + B * document.terms().size() / averageLength);
            raw += idf * frequency * (K1 + 1) / denominator;
        }
        double queryCoverage = (double) matchedTerms / queryTerms.size();
        double normalized = (1 - Math.exp(-raw)) * queryCoverage;
        RetrievedChunk chunk = document.chunk();
        return new RetrievedChunk(
                chunk.chunkId(), chunk.documentId(), chunk.chunkIndex(), chunk.content(),
                chunk.titlePath(), chunk.pageNumber(), chunk.startParagraphNumber(),
                chunk.endParagraphNumber(), chunk.category(),
                chunk.documentVersion(), chunk.source(), normalized
        );
    }

    List<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Matcher matcher = TOKEN_GROUP.matcher(value.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String group = matcher.group();
            if (group.codePoints().allMatch(codePoint -> Character.UnicodeScript.of(codePoint)
                    == Character.UnicodeScript.HAN)) {
                int[] points = group.codePoints().toArray();
                if (points.length == 1) {
                    result.add(group);
                } else {
                    for (int index = 0; index < points.length - 1; index++) {
                        result.add(new String(points, index, 2));
                    }
                }
            } else {
                result.add(group);
            }
        }
        return result;
    }

    private record DocumentTerms(
            RetrievedChunk chunk,
            List<String> terms,
            Map<String, Integer> frequencies
    ) {
    }
}
