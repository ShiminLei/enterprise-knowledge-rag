package com.aishare.knowledgerag.common;

import com.aishare.knowledgerag.answer.ChatGenerationException;
import com.aishare.knowledgerag.answer.ChatUnavailableException;
import com.aishare.knowledgerag.conversation.ConversationNotFoundException;
import com.aishare.knowledgerag.security.KnowledgeAccessDeniedException;
import com.aishare.knowledgerag.security.InvalidAuthenticatedIdentityException;
import com.aishare.knowledgerag.ingestion.DocumentParseException;
import com.aishare.knowledgerag.ingestion.DocumentVersionConflictException;
import com.aishare.knowledgerag.embedding.EmbeddingGenerationException;
import com.aishare.knowledgerag.embedding.EmbeddingUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DocumentParseException.class)
    public ResponseEntity<ApiError> handleDocumentParse(
            DocumentParseException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "DOCUMENT_PARSE_FAILED", exception.getMessage(), request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingPart(
            MissingServletRequestPartException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "UPLOAD_FILE_REQUIRED", "必须上传 file", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "UPLOAD_TOO_LARGE", "上传文件超过 20MB", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .sorted()
                .reduce((left, right) -> left + "; " + right)
                .orElse("请求参数校验失败");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request);
    }

    @ExceptionHandler(DocumentVersionConflictException.class)
    public ResponseEntity<ApiError> handleDocumentVersionConflict(
            DocumentVersionConflictException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, "DOCUMENT_VERSION_CONFLICT", exception.getMessage(), request);
    }

    @ExceptionHandler(EmbeddingUnavailableException.class)
    public ResponseEntity<ApiError> handleEmbeddingUnavailable(
            EmbeddingUnavailableException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "EMBEDDING_UNAVAILABLE", exception.getMessage(), request);
    }

    @ExceptionHandler(EmbeddingGenerationException.class)
    public ResponseEntity<ApiError> handleEmbeddingGeneration(
            EmbeddingGenerationException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_GATEWAY, "EMBEDDING_FAILED", exception.getMessage(), request);
    }

    @ExceptionHandler(ChatUnavailableException.class)
    public ResponseEntity<ApiError> handleChatUnavailable(
            ChatUnavailableException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "CHAT_UNAVAILABLE", exception.getMessage(), request);
    }

    @ExceptionHandler(ChatGenerationException.class)
    public ResponseEntity<ApiError> handleChatGeneration(
            ChatGenerationException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_GATEWAY, "CHAT_GENERATION_FAILED", exception.getMessage(), request);
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ApiError> handleConversationNotFound(
            ConversationNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(KnowledgeAccessDeniedException.class)
    public ResponseEntity<ApiError> handleKnowledgeAccessDenied(
            KnowledgeAccessDeniedException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.FORBIDDEN, "KNOWLEDGE_ACCESS_DENIED", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidAuthenticatedIdentityException.class)
    public ResponseEntity<ApiError> handleInvalidAuthenticatedIdentity(
            InvalidAuthenticatedIdentityException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_AUTHENTICATED_IDENTITY",
                exception.getMessage(), request);
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI()
        ));
    }
}
