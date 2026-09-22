package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.api.dto.AnswerRequest;
import com.aishare.knowledgerag.audit.AuditedConversationalAnswerService;
import com.aishare.knowledgerag.conversation.ConversationAnswer;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.aishare.knowledgerag.security.AccessContext;
import com.aishare.knowledgerag.security.AccessContextService;
import com.aishare.knowledgerag.security.AuthenticatedIdentity;
import com.aishare.knowledgerag.security.CurrentAuthenticatedIdentityProvider;
import com.aishare.knowledgerag.streaming.AnswerStreamingService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/answers")
public class KnowledgeAnswerController {

    private final AuditedConversationalAnswerService answerService;
    private final RetrievalProperties retrievalProperties;
    private final AccessContextService accessContextService;
    private final CurrentAuthenticatedIdentityProvider identityProvider;
    private final AnswerStreamingService streamingService;

    public KnowledgeAnswerController(
            AuditedConversationalAnswerService answerService,
            RetrievalProperties retrievalProperties,
            AccessContextService accessContextService,
            CurrentAuthenticatedIdentityProvider identityProvider,
            AnswerStreamingService streamingService
    ) {
        this.answerService = answerService;
        this.retrievalProperties = retrievalProperties;
        this.accessContextService = accessContextService;
        this.identityProvider = identityProvider;
        this.streamingService = streamingService;
    }

    @PostMapping
    public ConversationAnswer answer(@Valid @RequestBody AnswerRequest request) {
        AccessContext accessContext = currentAccessContext();
        return answerService.answer(
                Optional.ofNullable(request.conversationId()),
                request.toQuery(retrievalProperties, accessContext)
        );
    }

    @PostMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8"
    )
    public SseEmitter stream(@Valid @RequestBody AnswerRequest request) {
        AccessContext accessContext = currentAccessContext();
        return streamingService.stream(
                Optional.ofNullable(request.conversationId()),
                request.toQuery(retrievalProperties, accessContext)
        );
    }

    private AccessContext currentAccessContext() {
        AuthenticatedIdentity identity = identityProvider.current();
        return accessContextService.resolve(identity.tenantId(), identity.userId());
    }
}
