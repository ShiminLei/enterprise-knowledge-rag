package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.api.dto.CreatePromptVersionRequest;
import com.aishare.knowledgerag.api.dto.PromptVersionResponse;
import com.aishare.knowledgerag.prompt.PromptTemplate;
import com.aishare.knowledgerag.prompt.PromptTemplateService;
import com.aishare.knowledgerag.security.CurrentAuthenticatedIdentityProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/admin/prompts/rag-answer-system/versions")
public class PromptManagementController {

    private final PromptTemplateService promptTemplateService;
    private final CurrentAuthenticatedIdentityProvider identityProvider;

    public PromptManagementController(
            PromptTemplateService promptTemplateService,
            CurrentAuthenticatedIdentityProvider identityProvider
    ) {
        this.promptTemplateService = promptTemplateService;
        this.identityProvider = identityProvider;
    }

    @GetMapping
    public List<PromptVersionResponse> list() {
        return promptTemplateService.ragAnswerPromptVersions().stream()
                .map(PromptVersionResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<PromptVersionResponse> create(
            @Valid @RequestBody CreatePromptVersionRequest request
    ) {
        PromptTemplate created = promptTemplateService.createRagAnswerPromptVersion(
                request.version(),
                request.content(),
                identityProvider.current().userId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PromptVersionResponse.from(created));
    }

    @PutMapping("/{version}/activate")
    public PromptVersionResponse activate(
            @PathVariable
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*")
            String version
    ) {
        return PromptVersionResponse.from(
                promptTemplateService.activateRagAnswerPromptVersion(version)
        );
    }
}
