package com.example.doc_editor.document;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }


    @PostMapping
    public Document createDocument(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        return documentService.createDocument(
                email,
                request.get("title"),
                request.get("content")
        );
    }


    @GetMapping
    public List<Document> getMyDocuments(
            Authentication authentication
    ) {

        String email = authentication.getName();

        return documentService.getMyDocuments(email);
    }


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


    @PutMapping("/{id}")
    public Document updateDocument(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        return documentService.updateDocument(
                id,
                email,
                request.get("title"),
                request.get("content")
        );
    }


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