package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.api.dto.DocumentImportMetadataRequest;
import com.aishare.knowledgerag.ingestion.DocumentIngestionPreparationService;
import com.aishare.knowledgerag.ingestion.DocumentIngestionPreviewService;
import com.aishare.knowledgerag.ingestion.DocumentImportOutcome;
import com.aishare.knowledgerag.ingestion.DocumentImportResult;
import com.aishare.knowledgerag.ingestion.DocumentImportService;
import com.aishare.knowledgerag.ingestion.DocumentParseException;
import com.aishare.knowledgerag.ingestion.IngestionPreview;
import com.aishare.knowledgerag.ingestion.PreparedIngestion;
import com.aishare.knowledgerag.security.AuthenticatedIdentity;
import com.aishare.knowledgerag.security.CurrentAuthenticatedIdentityProvider;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentIngestionController {

    private final DocumentIngestionPreviewService previewService;
    private final DocumentIngestionPreparationService preparationService;
    private final DocumentImportService importService;
    private final CurrentAuthenticatedIdentityProvider identityProvider;

    public DocumentIngestionController(
            DocumentIngestionPreviewService previewService,
            DocumentIngestionPreparationService preparationService,
            DocumentImportService importService,
            CurrentAuthenticatedIdentityProvider identityProvider
    ) {
        this.previewService = previewService;
        this.preparationService = preparationService;
        this.importService = importService;
        this.identityProvider = identityProvider;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestionPreview preview(@RequestPart("file") MultipartFile file) {
        try {
            return previewService.preview(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
            );
        } catch (IOException exception) {
            throw new DocumentParseException("读取上传文件失败", exception);
        }
    }

    @PostMapping(value = "/prepare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PreparedIngestion prepare(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") DocumentImportMetadataRequest metadata
    ) {
        try {
            AuthenticatedIdentity identity = identityProvider.current();
            return preparationService.prepare(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes(),
                    metadata.toDomain(identity.tenantId())
            );
        } catch (IOException exception) {
            throw new DocumentParseException("读取上传文件失败", exception);
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentImportResult> importDocument(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") DocumentImportMetadataRequest metadata
    ) {
        try {
            AuthenticatedIdentity identity = identityProvider.current();
            DocumentImportResult result = importService.importDocument(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes(),
                    metadata.toDomain(identity.tenantId())
            );
            HttpStatus status = result.outcome() == DocumentImportOutcome.IMPORTED
                    ? HttpStatus.CREATED
                    : HttpStatus.OK;
            return ResponseEntity.status(status).body(result);
        } catch (IOException exception) {
            throw new DocumentParseException("读取上传文件失败", exception);
        }
    }
}
