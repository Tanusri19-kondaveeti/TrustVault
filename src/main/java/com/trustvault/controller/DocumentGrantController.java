
package com.trustvault.controller;

import com.trustvault.dto.ShareDocumentRequest;
import com.trustvault.dto.ShareDocumentResponse;
import com.trustvault.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentGrantController {

    private final DocumentService documentService;

    public DocumentGrantController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/{documentId}/share")
    public ShareDocumentResponse shareDocument(
            Authentication authentication,
            @PathVariable Long documentId,
            @Valid @RequestBody ShareDocumentRequest request) {

        return documentService.shareDocument(
                authentication.getName(),
                documentId,
                request.getRecipientEmail(),
                request.getPermission()
        );
    }

    @DeleteMapping("/{documentId}/share/{recipientId}")
    public ResponseEntity<Void> revokeAccess(
            Authentication authentication,
            @PathVariable Long documentId,
            @PathVariable Long recipientId) {

        documentService.revokeAccess(
                authentication.getName(),
                documentId,
                recipientId
        );

        return ResponseEntity.noContent().build();
    }
}
