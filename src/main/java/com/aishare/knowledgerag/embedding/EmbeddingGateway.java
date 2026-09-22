package com.aishare.knowledgerag.embedding;

import java.util.List;

public interface EmbeddingGateway {

    List<float[]> embed(List<String> texts);
}
