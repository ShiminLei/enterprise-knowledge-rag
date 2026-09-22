package com.aishare.knowledgerag.api;

import com.aishare.knowledgerag.ingestion.DocumentIngestionPreviewService;
import com.aishare.knowledgerag.ingestion.DocumentParseException;
import com.aishare.knowledgerag.ingestion.IngestionPreview;
import org.springframework.http.MediaType;
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

    public DocumentIngestionController(DocumentIngestionPreviewService previewService) {
        this.previewService = previewService;
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
}
