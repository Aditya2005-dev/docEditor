package com.example.doc_editor.document.history;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentVersionController {

    private final DocumentVersionService versionService;

    public DocumentVersionController(
            DocumentVersionService versionService
    ) {
        this.versionService = versionService;
    }

    @GetMapping("/{documentId}/history")
    public List<DocumentVersion> getHistory(
            @PathVariable Long documentId
    ) {
        return versionService.getHistory(documentId);
    }
}