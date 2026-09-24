package com.example.doc_editor.document;

import com.example.doc_editor.user.User;
import com.example.doc_editor.user.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository
    ) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    public Document createDocument(
            String email,
            String title,
            String content
    ) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Document document =
                new Document(title, content, user);

        return documentRepository.save(document);
    }

    public List<Document> getMyDocuments(String email) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return documentRepository.findByOwnerId(user.getId());
    }

    public Document getDocument(Long id) {

        return documentRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Document not found"));
    }

    public Document updateDocument(
            Long id,
            String email,
            String title,
            String content
    ) {

        Document document = getDocument(id);

        if (!document.getOwner().getEmail().equals(email)) {
            throw new RuntimeException(
                    "You are not the owner of this document"
            );
        }

        document.setTitle(title);
        document.setContent(content);

        return documentRepository.save(document);
    }

    public String deleteDocument(
            Long id,
            String email
    ) {

        Document document = getDocument(id);

        if (!document.getOwner().getEmail().equals(email)) {
            throw new RuntimeException(
                    "You are not the owner of this document"
            );
        }

        documentRepository.delete(document);

        return "Document deleted successfully";
    }
}