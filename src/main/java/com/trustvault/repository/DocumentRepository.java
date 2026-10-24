
package com.trustvault.repository;

import com.trustvault.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Find documents owned by a particular user
    List<Document> findByOwnerId(Long ownerId);

    // Search documents owned by a user by filename
    List<Document> findByOwnerIdAndFilenameContainingIgnoreCase(
            Long ownerId,
            String filename
    );
}
