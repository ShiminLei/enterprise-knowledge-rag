package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.api.dto.AnswerRequest;
import com.aishare.knowledgerag.conversation.ConversationAnswer;
import com.aishare.knowledgerag.conversation.ConversationalAnswerService;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/answers")
public class KnowledgeAnswerController {

    private final ConversationalAnswerService answerService;
    private final RetrievalProperties retrievalProperties;

    public KnowledgeAnswerController(
            ConversationalAnswerService answerService,
            RetrievalProperties retrievalProperties
    ) {
        this.answerService = answerService;
        this.retrievalProperties = retrievalProperties;
    }

    @PostMapping
    public ConversationAnswer answer(@Valid @RequestBody AnswerRequest request) {
        return answerService.answer(
                Optional.ofNullable(request.conversationId()),
                request.toQuery(retrievalProperties)
        );
    }
}
