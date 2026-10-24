package com.trustvault.dto;

public class ShareDocumentResponse {

    private Long documentId;
    private String recipientEmail;
    private String permission;
    private String message;

    public ShareDocumentResponse(
            Long documentId,
            String recipientEmail,
            String permission,
            String message) {
        this.documentId = documentId;
        this.recipientEmail = recipientEmail;
        this.permission = permission;
        this.message = message;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getPermission() {
        return permission;
    }

    public String getMessage() {
        return message;
    }
}