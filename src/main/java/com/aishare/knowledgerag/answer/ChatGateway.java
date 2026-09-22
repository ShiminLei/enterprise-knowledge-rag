package com.aishare.knowledgerag.answer;

import reactor.core.publisher.Flux;

public interface ChatGateway {

    String generate(String systemPrompt, String userPrompt);

    default Flux<String> stream(String systemPrompt, String userPrompt) {
        return Flux.just(generate(systemPrompt, userPrompt));
    }
}
