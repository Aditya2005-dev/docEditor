package com.example.doc_editor.document.permission;

import com.example.doc_editor.document.Document;
import com.example.doc_editor.user.User;

import jakarta.persistence.*;

@Entity
@Table(name = "document_permissions")
public class DocumentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private Permission permission;

    public DocumentPermission() {
    }

    public DocumentPermission(
            Document document,
            User user,
            Permission permission
    ) {
        this.document = document;
        this.user = user;
        this.permission = permission;
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public User getUser() {
        return user;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public void setUser(User user) {
        this.user = user;
    }
}