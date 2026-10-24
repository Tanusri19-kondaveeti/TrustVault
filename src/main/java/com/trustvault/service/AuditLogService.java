
package com.trustvault.service;

import com.trustvault.entity.AuditLog;
import com.trustvault.entity.Document;
import com.trustvault.entity.User;
import com.trustvault.repository.AuditLogRepository;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void logEvent(
            String actorEmail,
            Long documentId,
            String action,
            String outcome,
            String details) {

        AuditLog auditLog = new AuditLog();

        if (actorEmail != null && !actorEmail.isBlank()) {
            userRepository.findByEmail(actorEmail)
                    .ifPresent(auditLog::setActor);
        }

        if (documentId != null) {
            documentRepository.findById(documentId)
                    .ifPresent(auditLog::setDocument);
        }

        auditLog.setAction(action);
        auditLog.setOutcome(outcome);
        auditLog.setDetails(details);

        auditLogRepository.save(auditLog);
        try {
            auditLogRepository.saveAndFlush(auditLog);
        } catch (Exception ex) {
            System.err.println("AUDIT LOG SAVE FAILED: " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
    }
}
