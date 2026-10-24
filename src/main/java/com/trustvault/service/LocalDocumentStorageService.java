
package com.trustvault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        name = "trustvault.storage.type",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalDocumentStorageService implements DocumentStorage {

    private final Path storageDirectory;

    public LocalDocumentStorageService(
            @Value("${trustvault.storage.local-directory:uploads}")
            String directory) throws IOException {
        storageDirectory = Paths.get(directory).toAbsolutePath().normalize();
        Files.createDirectories(storageDirectory);
    }

    @Override
    public String store(
            InputStream inputStream,
            long fileSize,
            String originalFilename,
            String contentType) throws IOException {
        String key = UUID.randomUUID().toString();
        Files.copy(inputStream, resolveKey(key));
        return key;
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveKey(storageKey));
    }

    @Override
    public InputStream download(String storageKey) throws IOException {
        return Files.newInputStream(resolveKey(storageKey));
    }

    private Path resolveKey(String key) {
        Path path = storageDirectory.resolve(key).normalize();
        if (!path.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}
