
package com.trustvault.dto;

public class DocumentUploadResponse {

    private Long id;
    private String filename;
    private String contentType;
    private Long fileSize;
    private String message;

    public DocumentUploadResponse(
            Long id,
            String filename,
            String contentType,
            Long fileSize,
            String message) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getMessage() {
        return message;
    }
}
