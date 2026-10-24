
package com.trustvault.service;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentStorage {

    String store(
            InputStream inputStream,
            long fileSize,
            String originalFilename,
            String contentType) throws IOException;

    void delete(String storageKey) throws IOException;

    InputStream download(String storageKey) throws IOException;
}
