package com.example.doc_editor.document.dto;

import com.example.doc_editor.document.Document;

public class DocumentResponse {

    private Long id;
    private String title;
    private String content;
    private Long version;

    private Long ownerId;
    private String ownerName;
    private String ownerEmail;

    public DocumentResponse() {
    }

    public DocumentResponse(
            Long id,
            String title,
            String content,
            Long version,
            Long ownerId,
            String ownerName,
            String ownerEmail
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.version = version;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
    }

    public static DocumentResponse fromDocument(Document document) {

        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getContent(),
                document.getVersion(),
                document.getOwner().getId(),
                document.getOwner().getName(),
                document.getOwner().getEmail()
        );
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Long getVersion() {
        return version;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }
}