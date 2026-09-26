package com.example.doc_editor.document;

import com.example.doc_editor.document.permission.DocumentPermission;
import com.example.doc_editor.document.permission.DocumentShareRequest;
import com.example.doc_editor.document.permission.DocumentShareRequestRepository;
import com.example.doc_editor.document.permission.Permission;
import com.example.doc_editor.document.permission.PermissionRepository;
import com.example.doc_editor.document.permission.ShareRequestStatus;
import com.example.doc_editor.exception.AccessDeniedException;
import com.example.doc_editor.exception.DocumentNotFoundException;
import com.example.doc_editor.user.User;
import com.example.doc_editor.user.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final DocumentShareRequestRepository shareRequestRepository;


    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            PermissionRepository permissionRepository,
            DocumentShareRequestRepository shareRequestRepository
    ) {

        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.shareRequestRepository = shareRequestRepository;
    }


    // =========================================
    // CREATE DOCUMENT
    // =========================================

    public Document createDocument(
            String email,
            String title,
            String content
    ) {

        User user = getUser(email);

        Document document =
                new Document(
                        title,
                        content,
                        user
                );

        return documentRepository.save(document);
    }


    // =========================================
    // MY DOCUMENTS
    // =========================================

    public List<Document> getMyDocuments(
            String email
    ) {

        User user = getUser(email);

        return documentRepository
                .findByOwnerId(user.getId());
    }


    // =========================================
    // SHARED DOCUMENTS
    // =========================================

    public List<Document> getSharedDocuments(
            String email
    ) {

        User user = getUser(email);

        List<DocumentPermission> permissions =
                permissionRepository
                        .findByUserId(user.getId());

        return permissions
                .stream()
                .map(DocumentPermission::getDocument)
                .toList();
    }


    // =========================================
    // GET DOCUMENT
    // =========================================

    public Document getDocument(
            Long documentId,
            String email
    ) {

        User user = getUser(email);

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(() ->
                                new DocumentNotFoundException(
                                        "Document not found"
                                )
                        );


        // Owner
        if (document.getOwner()
                .getId()
                .equals(user.getId())) {

            return document;
        }


        // Shared user
        permissionRepository
                .findByDocumentIdAndUserId(
                        documentId,
                        user.getId()
                )
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You do not have access to this document"
                        )
                );

        return document;
    }


    // =========================================
    // UPDATE DOCUMENT
    // =========================================

    public Document updateDocument(
            Long documentId,
            String email,
            String title,
            String content
    ) {

        User user = getUser(email);

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(() ->
                                new DocumentNotFoundException(
                                        "Document not found"
                                )
                        );


        // Owner
        if (document.getOwner()
                .getId()
                .equals(user.getId())) {

            document.setTitle(title);
            document.setContent(content);

            return documentRepository.save(document);
        }


        // Shared user
        DocumentPermission permission =
                permissionRepository
                        .findByDocumentIdAndUserId(
                                documentId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "You do not have access to this document"
                                )
                        );


        if (permission.getPermission()
                != Permission.EDITOR) {

            throw new AccessDeniedException(
                    "You only have view access"
            );
        }


        document.setTitle(title);
        document.setContent(content);

        return documentRepository.save(document);
    }


    // =========================================
    // SEND SHARE REQUEST
    // =========================================

    public String shareDocument(
            Long documentId,
            String ownerEmail,
            String userEmail,
            Permission permission
    ) {

        User owner = getUser(ownerEmail);


        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(() ->
                                new DocumentNotFoundException(
                                        "Document not found"
                                )
                        );


        // Only owner can share
        if (!document.getOwner()
                .getId()
                .equals(owner.getId())) {

            throw new AccessDeniedException(
                    "Only the owner can share this document"
            );
        }


        User receiver =
                getUser(userEmail);


        // Cannot share with yourself
        if (owner.getId()
                .equals(receiver.getId())) {

            throw new AccessDeniedException(
                    "You cannot share a document with yourself"
            );
        }


        // Already has access
        if (permissionRepository
                .findByDocumentIdAndUserId(
                        documentId,
                        receiver.getId()
                )
                .isPresent()) {

            throw new AccessDeniedException(
                    "User already has access to this document"
            );
        }


        // Existing pending request
        if (shareRequestRepository
                .existsByDocumentIdAndReceiverIdAndStatus(
                        documentId,
                        receiver.getId(),
                        ShareRequestStatus.PENDING
                )) {

            return "Share request already pending";
        }


        DocumentShareRequest request =
                new DocumentShareRequest(
                        document,
                        owner,
                        receiver,
                        permission
                );


        shareRequestRepository.save(request);


        return "Share request sent successfully";
    }


    // =========================================
    // GET PENDING REQUESTS
    // =========================================

    public List<DocumentShareRequest>
    getPendingRequests(
            String email
    ) {

        User user = getUser(email);

        return shareRequestRepository
                .findByReceiverIdAndStatusOrderByCreatedAtDesc(
                        user.getId(),
                        ShareRequestStatus.PENDING
                );
    }


    // =========================================
    // ACCEPT SHARE REQUEST
    // =========================================

    public String acceptShareRequest(
            Long requestId,
            String email
    ) {

        User receiver = getUser(email);


        DocumentShareRequest request =
                shareRequestRepository
                        .findByIdAndReceiverId(
                                requestId,
                                receiver.getId()
                        )
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Share request not found"
                                )
                        );


        if (request.getStatus()
                != ShareRequestStatus.PENDING) {

            return "Request already processed";
        }


        DocumentPermission permission =
                permissionRepository
                        .findByDocumentIdAndUserId(
                                request.getDocument().getId(),
                                receiver.getId()
                        )
                        .orElse(null);


        if (permission == null) {

            permission =
                    new DocumentPermission(
                            request.getDocument(),
                            receiver,
                            request.getPermission()
                    );

        } else {

            permission.setPermission(
                    request.getPermission()
            );
        }


        permissionRepository.save(permission);


        request.setStatus(
                ShareRequestStatus.ACCEPTED
        );


        shareRequestRepository.save(request);


        return "Share request accepted";
    }


    // =========================================
    // DECLINE SHARE REQUEST
    // =========================================

    public String declineShareRequest(
            Long requestId,
            String email
    ) {

        User receiver = getUser(email);


        DocumentShareRequest request =
                shareRequestRepository
                        .findByIdAndReceiverId(
                                requestId,
                                receiver.getId()
                        )
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Share request not found"
                                )
                        );


        if (request.getStatus()
                != ShareRequestStatus.PENDING) {

            return "Request already processed";
        }


        request.setStatus(
                ShareRequestStatus.DECLINED
        );


        shareRequestRepository.save(request);


        return "Share request declined";
    }


    // =========================================
    // MEMBERS
    // =========================================

    public List<DocumentPermission> getMembers(
            Long documentId,
            String email
    ) {

        User user = getUser(email);

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(() ->
                                new DocumentNotFoundException(
                                        "Document not found"
                                )
                        );


        if (!document.getOwner()
                .getId()
                .equals(user.getId())) {

            throw new AccessDeniedException(
                    "Only the owner can view members"
            );
        }


        return permissionRepository
                .findByDocumentId(documentId);
    }


    // =========================================
    // DELETE
    // =========================================

    public String deleteDocument(
            Long documentId,
            String email
    ) {

        User user = getUser(email);

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(() ->
                                new DocumentNotFoundException(
                                        "Document not found"
                                )
                        );


        if (!document.getOwner()
                .getId()
                .equals(user.getId())) {

            throw new AccessDeniedException(
                    "Only the owner can delete this document"
            );
        }


        documentRepository.delete(document);

        return "Document deleted successfully";
    }


    // =========================================
    // VIEW ACCESS
    // =========================================

    public boolean canViewDocument(
            Long documentId,
            String email
    ) {

        getDocument(
                documentId,
                email
        );

        return true;
    }


    // =========================================
    // EDIT ACCESS
    // =========================================

    public boolean canEditDocument(
            Long documentId,
            String email
    ) {

        User user = getUser(email);

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElseThrow(() ->
                                new DocumentNotFoundException(
                                        "Document not found"
                                )
                        );


        // Owner
        if (document.getOwner()
                .getId()
                .equals(user.getId())) {

            return true;
        }


        DocumentPermission permission =
                permissionRepository
                        .findByDocumentIdAndUserId(
                                documentId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "You do not have access to this document"
                                )
                        );


        if (permission.getPermission()
                != Permission.EDITOR) {

            throw new AccessDeniedException(
                    "You only have view access"
            );
        }


        return true;
    }


    // =========================================
    // USER
    // =========================================

    private User getUser(
            String email
    ) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "User not found"
                        )
                );
    }
}