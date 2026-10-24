
package com.trustvault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "document_grants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_grantee",
                        columnNames = {"document_id", "granted_to_user_id"}
                )
        }
)
public class DocumentGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(optional = false)
    @JoinColumn(name = "granted_to_user_id", nullable = false)
    private User grantedToUser;

    @Column(nullable = false, length = 10)
    private String permission;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    public DocumentGrant() {
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public User getGrantedToUser() {
        return grantedToUser;
    }

    public void setGrantedToUser(User grantedToUser) {
        this.grantedToUser = grantedToUser;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }
    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
    }
}
