package com.example.doc_editor.document.permission;

import com.example.doc_editor.document.Document;
import com.example.doc_editor.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_share_requests")
public class DocumentShareRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Permission permission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShareRequestStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public DocumentShareRequest() {
        this.status = ShareRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }


    // ==========================================
    // GETTERS
    // ==========================================

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public User getSender() {
        return sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public Permission getPermission() {
        return permission;
    }

    public ShareRequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    // ==========================================
    // SETTERS
    // ==========================================

    public void setDocument(Document document) {
        this.document = document;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public void setStatus(ShareRequestStatus status) {
        this.status = status;
    }
}