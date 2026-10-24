
package com.trustvault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        name = "trustvault.storage.type",
        havingValue = "s3"
)
public class DocumentStorageService implements DocumentStorage {

    private final S3Client s3Client;
    private final String bucket;

    public DocumentStorageService(
            S3Client s3Client,
            @Value("${trustvault.storage.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public String store(
            InputStream inputStream,
            long fileSize,
            String originalFilename,
            String contentType) throws IOException {

        String storageKey = UUID.randomUUID().toString();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType == null
                        ? "application/octet-stream"
                        : contentType)
                .build();

        s3Client.putObject(
                request,
                RequestBody.fromInputStream(inputStream, fileSize)
        );

        return storageKey;
    }

    @Override
    public void delete(String storageKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();

        s3Client.deleteObject(request);
    }

    @Override
    public InputStream download(String storageKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();

        return s3Client.getObject(request);
    }
}
