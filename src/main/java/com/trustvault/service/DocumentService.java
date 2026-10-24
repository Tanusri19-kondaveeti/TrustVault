
package com.trustvault.service;

import com.trustvault.dto.DocumentUploadResponse;
import com.trustvault.dto.ShareDocumentResponse;
import com.trustvault.entity.Document;
import com.trustvault.entity.DocumentGrant;
import com.trustvault.entity.User;
import com.trustvault.repository.DocumentGrantRepository;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "text/plain"
    );

    private final AuditLogService auditLogService;
    private final DocumentGrantRepository documentGrantRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentStorage storageService;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            DocumentStorage storageService,
            DocumentGrantRepository documentGrantRepository,
            AuditLogService auditLogService) {

        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.documentGrantRepository = documentGrantRepository;
        this.auditLogService = auditLogService;
    }

    // Upload a new document.
    @Transactional
    public DocumentUploadResponse upload(String email, MultipartFile file) {

        validateFile(file);

        String filename = sanitizeFilename(file.getOriginalFilename());
        String contentType = file.getContentType();

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User account not found"));

        String storageKey = storeFile(file, filename, contentType);

        try {
            Document document = new Document();
            document.setOwner(owner);
            document.setFilename(filename);
            document.setStorageKey(storageKey);
            document.setContentType(contentType);
            document.setFileSize(file.getSize());

            Document saved = documentRepository.saveAndFlush(document);

            auditLogService.logEvent(
                    email,
                    saved.getId(),
                    "DOCUMENT_UPLOAD",
                    "SUCCESS",
                    "Document uploaded successfully"
            );

            return new DocumentUploadResponse(
                    saved.getId(),
                    saved.getFilename(),
                    saved.getContentType(),
                    saved.getFileSize(),
                    "Document uploaded successfully"
            );

        } catch (RuntimeException ex) {
            cleanupStorageObject(storageKey, ex);
            throw ex;
        }
    }

    // Download a document if the user owns it or has READ/EDIT access.
    @Transactional(readOnly = true)
    public InputStream download(String email, Long documentId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));

        boolean isOwner =
                document.getOwner().getId().equals(user.getId());

        if (!isOwner) {
            var grant = documentGrantRepository
                    .findByDocumentIdAndGrantedToUserId(
                            documentId, user.getId());

            boolean canRead = grant.isPresent()
                    && ("READ".equalsIgnoreCase(
                    grant.get().getPermission())
                    || "EDIT".equalsIgnoreCase(
                    grant.get().getPermission()));

            if (!canRead) {
                auditLogService.logEvent(
                        email,
                        documentId,
                        "DOCUMENT_DOWNLOAD",
                        "DENIED",
                        "User attempted to download without permission"
                );

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You do not have permission to download this document");
            }
        }

        InputStream file;

        try {
            file = storageService.download(document.getStorageKey());
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve the stored document",
                    ex
            );
        }

        auditLogService.logEvent(
                email,
                documentId,
                "DOCUMENT_DOWNLOAD",
                "SUCCESS",
                "Document download authorized"
        );

        return file;
    }

    // Share a document with READ or EDIT permission.
    @Transactional
    public ShareDocumentResponse shareDocument(
            String ownerEmail,
            Long documentId,
            String recipientEmail,
            String permission) {

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User account not found"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));

        if (!document.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the document owner can share this document");
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Recipient email is required");
        }

        String normalizedEmail =
                recipientEmail.trim().toLowerCase(Locale.ROOT);

        User recipient = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Recipient account not found"));

        if (recipient.getId().equals(owner.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "You already own this document");
        }

        if (permission == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Permission is required");
        }

        String normalizedPermission =
                permission.trim().toUpperCase(Locale.ROOT);

        if (!normalizedPermission.equals("READ")
                && !normalizedPermission.equals("EDIT")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Permission must be READ or EDIT");
        }

        DocumentGrant grant = documentGrantRepository
                .findByDocumentIdAndGrantedToUserId(
                        documentId, recipient.getId())
                .orElseGet(DocumentGrant::new);

        grant.setDocument(document);
        grant.setGrantedToUser(recipient);
        grant.setPermission(normalizedPermission);

        documentGrantRepository.save(grant);

        auditLogService.logEvent(
                ownerEmail,
                documentId,
                "DOCUMENT_SHARE",
                "SUCCESS",
                "Document shared using " + normalizedPermission + " permission"
        );

        return new ShareDocumentResponse(
                documentId,
                recipient.getEmail(),
                normalizedPermission,
                "Document shared successfully"
        );
    }

    // Revoke an existing grant. Only the owner can revoke access.
    @Transactional
    public void revokeAccess(
            String ownerEmail,
            Long documentId,
            Long recipientId) {

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User account not found"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));

        if (!document.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the document owner can revoke access");
        }

        DocumentGrant grant = documentGrantRepository
                .findByDocumentIdAndGrantedToUserId(documentId, recipientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Access grant not found"));

        documentGrantRepository.delete(grant);

        auditLogService.logEvent(
                ownerEmail,
                documentId,
                "DOCUMENT_ACCESS_REVOKED",
                "SUCCESS",
                "Document access revoked for user ID " + recipientId
        );
    }

    // Delete a document. Only the owner can delete it.
    @Transactional
    public void deleteDocument(String email, Long documentId) {

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));

        if (!document.getOwner().getId().equals(owner.getId())) {
            auditLogService.logEvent(
                    email,
                    documentId,
                    "DOCUMENT_DELETE",
                    "DENIED",
                    "User attempted to delete a document they do not own"
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the document owner can delete this document");
        }

        documentGrantRepository.deleteByDocumentId(documentId);

        try {
            storageService.delete(document.getStorageKey());
        } catch (IOException | RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to delete the stored file");
        }

        documentRepository.delete(document);

        auditLogService.logEvent(
                email,
                null,
                "DOCUMENT_DELETE",
                "SUCCESS",
                "Document " + documentId + " deleted successfully"
        );
    }

    // Replace a document's contents. Owner or active EDIT grant required.
    @Transactional
    public DocumentUploadResponse replaceDocument(
            String email,
            Long documentId,
            MultipartFile file) {

        validateFile(file);

        String filename = sanitizeFilename(file.getOriginalFilename());
        String contentType = file.getContentType();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));

        boolean isOwner =
                document.getOwner().getId().equals(user.getId());

        boolean canEdit = isOwner;

        if (!isOwner) {
            var grant = documentGrantRepository
                    .findByDocumentIdAndGrantedToUserId(
                            documentId, user.getId());

            canEdit = grant.isPresent()
                    && "EDIT".equalsIgnoreCase(
                    grant.get().getPermission());
        }

        if (!canEdit) {
            auditLogService.logEvent(
                    email,
                    documentId,
                    "DOCUMENT_EDIT",
                    "DENIED",
                    "User attempted to replace a document without EDIT permission"
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to edit this document");
        }

        String oldStorageKey = document.getStorageKey();
        String newStorageKey = storeFile(file, filename, contentType);

        try {
            document.setFilename(filename);
            document.setStorageKey(newStorageKey);
            document.setContentType(contentType);
            document.setFileSize(file.getSize());

            Document saved = documentRepository.saveAndFlush(document);

            auditLogService.logEvent(
                    email,
                    documentId,
                    "DOCUMENT_EDIT",
                    "SUCCESS",
                    "Document contents replaced successfully"
            );

            // The database now points to the replacement object.
            try {
                storageService.delete(oldStorageKey);
            } catch (IOException | RuntimeException cleanupException) {
                System.err.println(
                        "Warning: old document object needs cleanup.");
            }

            return new DocumentUploadResponse(
                    saved.getId(),
                    saved.getFilename(),
                    saved.getContentType(),
                    saved.getFileSize(),
                    "Document replaced successfully"
            );

        } catch (RuntimeException ex) {
            cleanupStorageObject(newStorageKey, ex);
            throw ex;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Please select a file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "File must not exceed 10 MB");
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !ALLOWED_TYPES.contains(
                contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Allowed file types: PDF, JPEG, PNG and plain text");
        }
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Filename is required");
        }

        String filename = originalFilename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);

        if (filename.isBlank() || filename.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        return filename;
    }

    private String storeFile(
            MultipartFile file,
            String filename,
            String contentType) {

        try (InputStream inputStream = file.getInputStream()) {
            return storageService.store(
                    inputStream,
                    file.getSize(),
                    filename,
                    contentType
            );
        } catch (IOException | RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "File storage failed");
        }
    }

    private void cleanupStorageObject(
            String storageKey, RuntimeException ex) {
        try {
            storageService.delete(storageKey);
        } catch (IOException | RuntimeException cleanupException) {
            ex.addSuppressed(cleanupException);
        }
    }
}
