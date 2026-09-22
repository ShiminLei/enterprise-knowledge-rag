package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.api.dto.AnswerRequest;
import com.aishare.knowledgerag.audit.AuditedConversationalAnswerService;
import com.aishare.knowledgerag.conversation.ConversationAnswer;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.AccessContextService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/answers")
public class KnowledgeAnswerController {

    private final AuditedConversationalAnswerService answerService;
    private final RetrievalProperties retrievalProperties;
    private final AccessContextService accessContextService;

    public KnowledgeAnswerController(
            AuditedConversationalAnswerService answerService,
            RetrievalProperties retrievalProperties,
            AccessContextService accessContextService
    ) {
        this.answerService = answerService;
        this.retrievalProperties = retrievalProperties;
        this.accessContextService = accessContextService;
    }

    @PostMapping
    public ConversationAnswer answer(@Valid @RequestBody AnswerRequest request) {
        AccessContext accessContext = accessContextService.resolve(
                request.tenantId(), request.userId()
        );
        return answerService.answer(
                Optional.ofNullable(request.conversationId()),
                request.toQuery(retrievalProperties, accessContext)
        );
    }
}
