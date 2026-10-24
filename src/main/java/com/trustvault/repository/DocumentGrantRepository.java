
package com.trustvault.repository;

import com.trustvault.entity.DocumentGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentGrantRepository
        extends JpaRepository<DocumentGrant, Long> {

    // Find a particular user's grant for a document
    Optional<DocumentGrant> findByDocumentIdAndGrantedToUserId(
            Long documentId,
            Long userId
    );

    // Find all grants received by a user
    List<DocumentGrant> findByGrantedToUserId(Long userId);

    // Find grants received by a user for a specific permission
    List<DocumentGrant> findByGrantedToUserIdAndPermissionIgnoreCase(
            Long userId,
            String permission
    );

    void deleteByDocumentId(Long documentId);

}
