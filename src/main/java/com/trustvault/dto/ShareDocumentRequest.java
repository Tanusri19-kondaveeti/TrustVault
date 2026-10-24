package com.trustvault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ShareDocumentRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String recipientEmail;

    @NotBlank(message = "Permission is required")
    @Pattern(
            regexp = "READ|EDIT",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Permission must be READ or EDIT"
    )
    private String permission;

    public ShareDocumentRequest() {
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}