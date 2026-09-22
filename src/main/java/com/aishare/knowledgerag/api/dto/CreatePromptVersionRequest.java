package com.aishare.knowledgerag.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePromptVersionRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*",
                message = "只能包含字母、数字、点、下划线和连字符")
        String version,

        @NotBlank
        @Size(max = 20000)
        String content
) {
}
