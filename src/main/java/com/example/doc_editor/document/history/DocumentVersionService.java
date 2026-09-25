package com.example.doc_editor.document.history;

import com.example.doc_editor.document.Document;
import com.example.doc_editor.user.User;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentVersionService {

    private final DocumentVersionRepository repository;

    public DocumentVersionService(
            DocumentVersionRepository repository
    ) {
        this.repository = repository;
    }

    public void saveVersion(
            Document document,
            User user
    ) {

        DocumentVersion version =
                new DocumentVersion(
                        document,
                        document.getVersion(),
                        document.getContent(),
                        user
                );

        repository.save(version);
    }

    public List<DocumentVersion> getHistory(
            Long documentId
    ) {

        return repository
                .findByDocumentIdOrderByVersionDesc(
                        documentId
                );
    }
}