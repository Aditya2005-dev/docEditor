package com.example.doc_editor.document;

import com.example.doc_editor.user.User;

import jakarta.persistence.*;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    public Document() {
    }

    public Document(
            String title,
            String content,
            User owner
    ) {
        this.title = title;
        this.content = content;
        this.owner = owner;
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

    public User getOwner() {
        return owner;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}