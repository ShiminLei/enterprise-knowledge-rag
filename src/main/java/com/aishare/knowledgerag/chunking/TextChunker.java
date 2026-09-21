package com.aishare.knowledgerag.chunking;

import com.aishare.knowledgerag.ingestion.ParsedDocument;

import java.util.List;

public interface TextChunker {

    List<ChunkCandidate> chunk(ParsedDocument document);
}
