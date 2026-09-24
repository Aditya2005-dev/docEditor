package com.example.doc_editor.document;

import com.example.doc_editor.document.permission.DocumentPermission;
import com.example.doc_editor.document.permission.Permission;
import com.example.doc_editor.document.permission.PermissionRepository;
import com.example.doc_editor.user.User;
import com.example.doc_editor.user.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            PermissionRepository permissionRepository
    ) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
    }


    // ==========================================
    // CREATE DOCUMENT
    // ==========================================

    public Document createDocument(
            String email,
            String title,
            String content
    ) {

        User user = getUser(email);

        Document document =
                new Document(title, content, user);

        return documentRepository.save(document);
    }


    // ==========================================
    // MY DOCUMENTS
    // ==========================================

    public List<Document> getMyDocuments(
            String email
    ) {

        User user = getUser(email);

        return documentRepository
                .findByOwnerId(user.getId());
    }


    // ==========================================
    // SHARED DOCUMENTS
    // ==========================================

    public List<Document> getSharedDocuments(
            String email
    ) {

        User user = getUser(email);

        List<DocumentPermission> permissions =
                permissionRepository.findByUserId(
                        user.getId()
                );

        return permissions
                .stream()
                .map(DocumentPermission::getDocument)
                .toList();
    }


    // ==========================================
    // GET DOCUMENT
    // ==========================================

    public Document getDocument(
            Long documentId,
            String email
    ) {

        User user = getUser(email);

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Document not found"
                        )
                );

        if (document.getOwner().getId()
                .equals(user.getId())) {

            return document;
        }

        DocumentPermission permission =
                permissionRepository
                        .findByDocumentIdAndUserId(
                                documentId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "You do not have access"
                                )
                        );

        return document;
    }


    // ==========================================
    // UPDATE DOCUMENT
    // ==========================================

    public Document updateDocument(
            Long documentId,
            String email,
            String title,
            String content
    ) {

        User user = getUser(email);

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Document not found"
                        )
                );


        // Owner can edit
        if (document.getOwner().getId()
                .equals(user.getId())) {

            document.setTitle(title);
            document.setContent(content);

            return documentRepository.save(document);
        }


        // Check shared permission
        DocumentPermission permission =
                permissionRepository
                        .findByDocumentIdAndUserId(
                                documentId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "You do not have access"
                                )
                        );


        if (permission.getPermission()
                != Permission.EDITOR) {

            throw new RuntimeException(
                    "You only have view access"
            );
        }


        document.setTitle(title);
        document.setContent(content);

        return documentRepository.save(document);
    }


    // ==========================================
    // SHARE DOCUMENT
    // ==========================================

    public String shareDocument(
            Long documentId,
            String ownerEmail,
            String userEmail,
            Permission permission
    ) {

        User owner = getUser(ownerEmail);

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Document not found"
                        )
                );


        // Only owner can share
        if (!document.getOwner().getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "Only the owner can share this document"
            );
        }


        User userToShare =
                getUser(userEmail);


        // Owner cannot share with himself
        if (owner.getId()
                .equals(userToShare.getId())) {

            throw new RuntimeException(
                    "Owner already has access"
            );
        }


        // Check if permission already exists
        var existingPermission =
                permissionRepository
                        .findByDocumentIdAndUserId(
                                documentId,
                                userToShare.getId()
                        );


        if (existingPermission.isPresent()) {

            existingPermission
                    .get()
                    .setPermission(permission);

            permissionRepository.save(
                    existingPermission.get()
            );

            return "Permission updated";
        }


        DocumentPermission newPermission =
                new DocumentPermission(
                        document,
                        userToShare,
                        permission
                );

        permissionRepository.save(
                newPermission
        );

        return "Document shared successfully";
    }


    // ==========================================
    // GET MEMBERS
    // ==========================================

    public List<DocumentPermission> getMembers(
            Long documentId,
            String email
    ) {

        User user = getUser(email);

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Document not found"
                        )
                );


        if (!document.getOwner().getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "Only the owner can view members"
            );
        }


        return permissionRepository
                .findByDocumentId(documentId);
    }


    // ==========================================
    // DELETE DOCUMENT
    // ==========================================

    public String deleteDocument(
            Long documentId,
            String email
    ) {

        User user = getUser(email);

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Document not found"
                        )
                );


        if (!document.getOwner().getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "Only the owner can delete this document"
            );
        }


        documentRepository.delete(document);

        return "Document deleted successfully";
    }


    // ==========================================
    // GET USER
    // ==========================================

    private User getUser(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );
    }
}