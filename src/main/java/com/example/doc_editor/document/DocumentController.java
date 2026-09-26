package com.example.doc_editor.document;

import com.example.doc_editor.document.dto.DocumentRequest;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // ==========================================
    // CREATE DOCUMENT
    // ==========================================

    @PostMapping
    public Document createDocument(
            @Valid @RequestBody DocumentRequest request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        return documentService.createDocument(
                email,
                request.getTitle(),
                request.getContent()
        );
    }

    // ==========================================
    // MY DOCUMENTS
    // ==========================================

    @GetMapping
    public List<Document> getMyDocuments(
            Authentication authentication
    ) {

        String email = authentication.getName();

        return documentService.getMyDocuments(email);
    }

    // ==========================================
    // GET DOCUMENT
    // ==========================================

    @GetMapping("/{id}")
    public Document getDocument(
            @PathVariable Long id,
            Authentication authentication
    ) {

        String email = authentication.getName();

        return documentService.getDocument(
                id,
                email
        );
    }

    // ==========================================
    // UPDATE DOCUMENT
    // ==========================================

    @PutMapping("/{id}")
    public Document updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequest request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        return documentService.updateDocument(
                id,
                email,
                request.getTitle(),
                request.getContent()
        );
    }

    // ==========================================
    // DELETE DOCUMENT
    // ==========================================

    @DeleteMapping("/{id}")
    public String deleteDocument(
            @PathVariable Long id,
            Authentication authentication
    ) {

        String email = authentication.getName();

        return documentService.deleteDocument(
                id,
                email
        );
    }
}