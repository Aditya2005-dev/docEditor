package com.example.doc_editor.document.history;

import com.example.doc_editor.document.DocumentService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentVersionController {

    private final DocumentVersionService versionService;
    private final DocumentService documentService;

    public DocumentVersionController(
            DocumentVersionService versionService,
            DocumentService documentService
    ) {
        this.versionService = versionService;
        this.documentService = documentService;
    }

    // ==========================================
    // GET DOCUMENT HISTORY
    // ==========================================

    @GetMapping("/{documentId}/history")
    public List<DocumentVersion> getHistory(
            @PathVariable Long documentId,
            Authentication authentication
    ) {

        String email = authentication.getName();

        // Make sure the user has access to this document
        documentService.canViewDocument(
                documentId,
                email
        );

        return versionService.getHistory(
                documentId
        );
    }
}