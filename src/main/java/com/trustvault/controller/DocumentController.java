
package com.trustvault.controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.trustvault.entity.Document;
import com.trustvault.entity.User;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import java.io.IOException;
import com.trustvault.dto.DocumentUploadResponse;
import com.trustvault.service.DocumentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.trustvault.repository.DocumentGrantRepository;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import java.io.InputStream;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentGrantRepository documentGrantRepository;


    public DocumentController(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            DocumentService documentService,
            DocumentGrantRepository documentGrantRepository) {

        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.documentService = documentService;
        this.documentGrantRepository = documentGrantRepository;
    }





    @GetMapping
    public Map<String, Object> getDocuments(
            Authentication authentication,
            @RequestParam(required = false) String search) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User account not found"
                ));

        List<Document> ownedDocuments;

        if (search == null || search.isBlank()) {
            ownedDocuments = documentRepository.findByOwnerId(user.getId());
        } else {
            ownedDocuments = documentRepository
                    .findByOwnerIdAndFilenameContainingIgnoreCase(
                            user.getId(), search.trim());
        }

        List<Document> sharedDocuments = documentGrantRepository
                .findByGrantedToUserId(user.getId())
                .stream()
                .map(grant -> grant.getDocument())
                .filter(document -> search == null || search.isBlank()
                        || document.getFilename().toLowerCase()
                        .contains(search.trim().toLowerCase()))
                .toList();

        Map<Long, Map<String, Object>> results = new java.util.LinkedHashMap<>();

        for (Document document : ownedDocuments) {
            results.put(document.getId(), documentSummary(document, "OWNER"));
        }

        for (Document document : sharedDocuments) {
            results.putIfAbsent(document.getId(),
                    documentSummary(document, "SHARED"));
        }

        List<Map<String, Object>> documentList =
                new java.util.ArrayList<>(results.values());

        return Map.of(
                "message", "Documents retrieved successfully",
                "count", documentList.size(),
                "documents", documentList
        );
    }

    private Map<String, Object> documentSummary(
            Document document, String accessType) {
        return Map.of(
                "id", document.getId(),
                "filename", document.getFilename(),
                "contentType", document.getContentType() == null
                        ? "" : document.getContentType(),
                "fileSize", document.getFileSize(),
                "createdAt", document.getCreatedAt().toString(),
                "accessType", accessType
        );
    }


    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {

        DocumentUploadResponse response = documentService.upload(
                authentication.getName(),
                file
        );

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable Long documentId,
            Authentication authentication) {

        InputStream inputStream = documentService.download(
                authentication.getName(), documentId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"document-" + documentId + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(inputStream));
    }


    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            Authentication authentication,
            @PathVariable Long documentId) {

        documentService.deleteDocument(
                authentication.getName(), documentId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(
            value = "/{documentId}/content",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentUploadResponse> replaceDocument(
            Authentication authentication,
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file) {

        DocumentUploadResponse response = documentService.replaceDocument(
                authentication.getName(),
                documentId,
                file
        );

        return ResponseEntity.ok(response);
    }





}
