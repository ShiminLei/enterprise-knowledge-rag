package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.answer.GroundedAnswer;
import com.aishare.knowledgerag.answer.RagAnswerService;
import com.aishare.knowledgerag.api.dto.AnswerRequest;
import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/answers")
public class KnowledgeAnswerController {

    private final RagAnswerService answerService;
    private final RetrievalProperties retrievalProperties;

    public KnowledgeAnswerController(
            RagAnswerService answerService,
            RetrievalProperties retrievalProperties
    ) {
        this.answerService = answerService;
        this.retrievalProperties = retrievalProperties;
    }

    @PostMapping
    public GroundedAnswer answer(@Valid @RequestBody AnswerRequest request) {
        return answerService.answer(request.toQuery(retrievalProperties));
    }
}
