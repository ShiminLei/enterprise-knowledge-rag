package com.aishare.knowledgerag.ingestion;

public interface DocumentParser {

    boolean supports(String fileName, String mediaType);

    ParsedDocument parse(String fileName, String mediaType, byte[] content);
}
