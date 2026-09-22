package com.aishare.knowledgerag.streaming;

import com.aishare.knowledgerag.answer.AnswerCitation;

import java.util.List;

public record AnswerStreamCitations(List<AnswerCitation> citations) {

    public AnswerStreamCitations {
        citations = List.copyOf(citations);
    }
}
