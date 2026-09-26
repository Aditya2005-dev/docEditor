package com.example.doc_editor.document;

import com.example.doc_editor.document.dto.DocumentRequest;
import com.example.doc_editor.document.permission.DocumentPermission;
import com.example.doc_editor.document.permission.DocumentShareRequest;
import com.example.doc_editor.document.permission.Permission;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;


    public DocumentController(
            DocumentService documentService
    ) {

        this.documentService = documentService;
    }


    // =========================================
    // CREATE
    // =========================================

    @PostMapping
    public Document createDocument(
            @Valid @RequestBody DocumentRequest request,
            Authentication authentication
    ) {

        return documentService.createDocument(
                authentication.getName(),
                request.getTitle(),
                request.getContent()
        );
    }


    // =========================================
    // MY DOCUMENTS
    // =========================================

    @GetMapping
    public List<Document> getMyDocuments(
            Authentication authentication
    ) {

        return documentService.getMyDocuments(
                authentication.getName()
        );
    }


    // =========================================
    // SHARED DOCUMENTS
    // =========================================

    @GetMapping("/shared")
    public List<Document> getSharedDocuments(
            Authentication authentication
    ) {

        return documentService.getSharedDocuments(
                authentication.getName()
        );
    }


    // =========================================
    // GET DOCUMENT
    // =========================================

    @GetMapping("/{id}")
    public Document getDocument(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return documentService.getDocument(
                id,
                authentication.getName()
        );
    }


    // =========================================
    // UPDATE
    // =========================================

    @PutMapping("/{id}")
    public Document updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequest request,
            Authentication authentication
    ) {

        return documentService.updateDocument(
                id,
                authentication.getName(),
                request.getTitle(),
                request.getContent()
        );
    }


    // =========================================
    // DELETE
    // =========================================

    @DeleteMapping("/{id}")
    public String deleteDocument(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return documentService.deleteDocument(
                id,
                authentication.getName()
        );
    }


    // =========================================
    // SEND SHARE REQUEST
    // =========================================

    @PostMapping("/{id}/share")
    public String shareDocument(
            @PathVariable Long id,
            @RequestParam String email,
            @RequestParam Permission permission,
            Authentication authentication
    ) {

        return documentService.shareDocument(
                id,
                authentication.getName(),
                email,
                permission
        );
    }


    // =========================================
    // MEMBERS
    // =========================================

    @GetMapping("/{id}/members")
    public List<DocumentPermission> getMembers(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return documentService.getMembers(
                id,
                authentication.getName()
        );
    }


    // =========================================
    // PENDING SHARE REQUESTS
    // =========================================

    @GetMapping("/share-requests")
    public List<DocumentShareRequest> getShareRequests(
            Authentication authentication
    ) {

        return documentService.getPendingRequests(
                authentication.getName()
        );
    }


    // =========================================
    // ACCEPT
    // =========================================

    @PostMapping(
            "/share-requests/{requestId}/accept"
    )
    public String acceptShareRequest(
            @PathVariable Long requestId,
            Authentication authentication
    ) {

        return documentService.acceptShareRequest(
                requestId,
                authentication.getName()
        );
    }


    // =========================================
    // DECLINE
    // =========================================

    @PostMapping(
            "/share-requests/{requestId}/decline"
    )
    public String declineShareRequest(
            @PathVariable Long requestId,
            Authentication authentication
    ) {

        return documentService.declineShareRequest(
                requestId,
                authentication.getName()
        );
    }
}