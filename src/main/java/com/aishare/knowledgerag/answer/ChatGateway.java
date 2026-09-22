package com.aishare.knowledgerag.answer;

public interface ChatGateway {

    String generate(String systemPrompt, String userPrompt);
}
