package com.example.doc_editor.document;

import com.example.doc_editor.document.history.DocumentVersion;
import com.example.doc_editor.document.history.DocumentVersionRepository;
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

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final DocumentShareRequestRepository shareRequestRepository;
    private final DocumentVersionRepository versionRepository;


    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            PermissionRepository permissionRepository,
            DocumentShareRequestRepository shareRequestRepository,
            DocumentVersionRepository versionRepository
    ) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.shareRequestRepository = shareRequestRepository;
        this.versionRepository = versionRepository;
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

        Document document = new Document();

        document.setTitle(title);
        document.setContent(content);
        document.setOwner(user);
        document.setVersion(0L);

        return documentRepository.save(document);
    }


    // ==========================================
    // GET MY DOCUMENTS
    // ==========================================

    public List<Document> getMyDocuments(
            String email
    ) {

        User user = getUser(email);

        return documentRepository.findByOwnerId(
                user.getId()
        );
    }


    // ==========================================
    // GET SHARED DOCUMENTS
    // ==========================================

    public List<Document> getSharedDocuments(
            String email
    ) {

        User user = getUser(email);

        return permissionRepository
                .findByUserId(user.getId())
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

        Document document =
                getDocumentEntity(documentId);

        if (!canViewDocument(
                documentId,
                email
        )) {

            throw new AccessDeniedException(
                    "You do not have access to this document"
            );
        }

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

        Document document =
                getDocumentEntity(documentId);

        if (!document.getOwner()
                .getEmail()
                .equals(email)) {

            throw new AccessDeniedException(
                    "Only the owner can update this document"
            );
        }

        document.setTitle(title);
        document.setContent(content);

        return documentRepository.save(document);
    }


    // ==========================================
    // DELETE DOCUMENT
    // ==========================================

    @Transactional
    public String deleteDocument(
            Long documentId,
            String email
    ) {

        Document document =
                getDocumentEntity(documentId);


        // ------------------------------------------
        // ONLY OWNER CAN DELETE
        // ------------------------------------------

        if (!document.getOwner()
                .getEmail()
                .equals(email)) {

            throw new AccessDeniedException(
                    "Only the owner can delete this document"
            );
        }


        // ------------------------------------------
        // 1. DELETE DOCUMENT PERMISSIONS
        // ------------------------------------------

        List<DocumentPermission> permissions =
                permissionRepository
                        .findByDocumentId(documentId);

        if (!permissions.isEmpty()) {

            permissionRepository.deleteAll(
                    permissions
            );
        }


        // ------------------------------------------
        // 2. DELETE SHARE REQUESTS
        // ------------------------------------------

        List<DocumentShareRequest> requests =
                shareRequestRepository
                        .findByDocumentId(documentId);

        if (!requests.isEmpty()) {

            shareRequestRepository.deleteAll(
                    requests
            );
        }


        // ------------------------------------------
        // 3. DELETE DOCUMENT VERSIONS
        // ------------------------------------------

        List<DocumentVersion> versions =
                versionRepository
                        .findByDocumentIdOrderByVersionDesc(
                                documentId
                        );

        if (!versions.isEmpty()) {

            versionRepository.deleteAll(
                    versions
            );
        }


        // ------------------------------------------
        // 4. DELETE DOCUMENT
        // ------------------------------------------

        documentRepository.delete(document);

        return "Document deleted successfully";
    }


    // ==========================================
    // SHARE DOCUMENT
    // ==========================================

    public String shareDocument(
            Long documentId,
            String senderEmail,
            String receiverEmail,
            Permission permission
    ) {

        Document document =
                getDocumentEntity(documentId);

        User sender =
                getUser(senderEmail);

        User receiver =
                getUser(receiverEmail);


        // ------------------------------------------
        // ONLY OWNER CAN SHARE
        // ------------------------------------------

        if (!document.getOwner()
                .getId()
                .equals(sender.getId())) {

            throw new AccessDeniedException(
                    "Only the owner can share this document"
            );
        }


        // ------------------------------------------
        // CANNOT SHARE WITH YOURSELF
        // ------------------------------------------

        if (sender.getId()
                .equals(receiver.getId())) {

            throw new IllegalArgumentException(
                    "You cannot share a document with yourself"
            );
        }


        // ------------------------------------------
        // CHECK EXISTING ACCESS
        // ------------------------------------------

        if (permissionRepository
                .findByDocumentIdAndUserId(
                        documentId,
                        receiver.getId()
                )
                .isPresent()) {

            throw new IllegalArgumentException(
                    "User already has access to this document"
            );
        }


        // ------------------------------------------
        // CHECK PENDING REQUEST
        // ------------------------------------------

        if (shareRequestRepository
                .existsByDocumentIdAndReceiverIdAndStatus(
                        documentId,
                        receiver.getId(),
                        ShareRequestStatus.PENDING
                )) {

            throw new IllegalArgumentException(
                    "A share request is already pending"
            );
        }


        // ------------------------------------------
        // CREATE SHARE REQUEST
        // ------------------------------------------

        DocumentShareRequest request =
                new DocumentShareRequest();

        request.setDocument(document);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setPermission(permission);

        shareRequestRepository.save(request);

        return "Share request sent successfully";
    }


    // ==========================================
    // GET PENDING SHARE REQUESTS
    // ==========================================

    public List<DocumentShareRequest> getPendingRequests(
            String email
    ) {

        User user =
                getUser(email);

        return shareRequestRepository
                .findByReceiverIdAndStatusOrderByCreatedAtDesc(
                        user.getId(),
                        ShareRequestStatus.PENDING
                );
    }


    // ==========================================
    // ACCEPT SHARE REQUEST
    // ==========================================

 @Transactional
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
                            new IllegalArgumentException(
                                    "Share request not found"
                            )
                    );

    if (request.getStatus() != ShareRequestStatus.PENDING) {

        throw new IllegalArgumentException(
                "Share request is no longer pending"
        );
    }

    Document document = request.getDocument();

    // Check whether permission already exists
    DocumentPermission existingPermission =
            permissionRepository
                    .findByDocumentIdAndUserId(
                            document.getId(),
                            receiver.getId()
                    )
                    .orElse(null);

    if (existingPermission == null) {

        // Use constructor directly
        DocumentPermission newPermission =
                new DocumentPermission(
                        document,
                        receiver,
                        request.getPermission()
                );

        permissionRepository.saveAndFlush(newPermission);

    } else {

        existingPermission.setPermission(
                request.getPermission()
        );

        permissionRepository.saveAndFlush(
                existingPermission
        );
    }

    // Mark request as accepted
    request.setStatus(
            ShareRequestStatus.ACCEPTED
    );

    shareRequestRepository.saveAndFlush(request);

    return "Share request accepted";
}


    // ==========================================
    // DECLINE SHARE REQUEST
    // ==========================================

    public String declineShareRequest(
            Long requestId,
            String email
    ) {

        User receiver =
                getUser(email);

        DocumentShareRequest request =
                shareRequestRepository
                        .findByIdAndReceiverId(
                                requestId,
                                receiver.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Share request not found"
                                )
                        );


        if (request.getStatus()
                != ShareRequestStatus.PENDING) {

            throw new IllegalArgumentException(
                    "Share request is no longer pending"
            );
        }


        request.setStatus(
                ShareRequestStatus.DECLINED
        );

        shareRequestRepository.save(request);

        return "Share request declined";
    }


    // ==========================================
    // GET MEMBERS
    // ==========================================

    public List<DocumentPermission> getMembers(
            Long documentId,
            String email
    ) {

        Document document =
                getDocumentEntity(documentId);


        if (!document.getOwner()
                .getEmail()
                .equals(email)) {

            throw new AccessDeniedException(
                    "Only the owner can view document members"
            );
        }


        return permissionRepository
                .findByDocumentId(documentId);
    }


    // ==========================================
    // CHECK VIEW ACCESS
    // ==========================================

    public boolean canViewDocument(
            Long documentId,
            String email
    ) {

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElse(null);

        if (document == null) {
            return false;
        }


        // Owner always has access
        if (document.getOwner()
                .getEmail()
                .equals(email)) {

            return true;
        }


        User user =
                userRepository
                        .findByEmail(email)
                        .orElse(null);

        if (user == null) {
            return false;
        }


        return permissionRepository
                .findByDocumentIdAndUserId(
                        documentId,
                        user.getId()
                )
                .isPresent();
    }


    // ==========================================
    // CHECK EDIT ACCESS
    // ==========================================

    public boolean canEditDocument(
            Long documentId,
            String email
    ) {

        Document document =
                documentRepository
                        .findById(documentId)
                        .orElse(null);

        if (document == null) {
            return false;
        }


        // Owner always has edit access
        if (document.getOwner()
                .getEmail()
                .equals(email)) {

            return true;
        }


        User user =
                userRepository
                        .findByEmail(email)
                        .orElse(null);

        if (user == null) {
            return false;
        }


        DocumentPermission permission =
                permissionRepository
                        .findByDocumentIdAndUserId(
                                documentId,
                                user.getId()
                        )
                        .orElse(null);

        if (permission == null) {
            return false;
        }


        return permission.getPermission()
                == Permission.EDITOR;
    }


    // ==========================================
    // REMOVE MEMBER
    // ==========================================

    @Transactional
    public void removeMember(
            Long documentId,
            Long userId,
            String email
    ) {

        Document document =
                getDocumentEntity(documentId);


        if (!document.getOwner()
                .getEmail()
                .equals(email)) {

            throw new AccessDeniedException(
                    "Only the owner can remove members"
            );
        }


        DocumentPermission permission =
                permissionRepository
                        .findByDocumentIdAndUserId(
                                documentId,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User does not have access"
                                )
                        );


        permissionRepository.delete(permission);
    }


    // ==========================================
    // CHANGE MEMBER PERMISSION
    // ==========================================

    @Transactional
    public void updateMemberPermission(
            Long documentId,
            Long userId,
            Permission newPermission,
            String email
    ) {

        Document document =
                getDocumentEntity(documentId);


        if (!document.getOwner()
                .getEmail()
                .equals(email)) {

            throw new AccessDeniedException(
                    "Only the owner can change permissions"
            );
        }


        DocumentPermission permission =
                permissionRepository
                        .findByDocumentIdAndUserId(
                                documentId,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User does not have access"
                                )
                        );


        permission.setPermission(
                newPermission
        );

        permissionRepository.save(permission);
    }


    // ==========================================
    // GET DOCUMENT ENTITY
    // ==========================================

    public Document getDocumentEntity(
            Long documentId
    ) {

        return documentRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new DocumentNotFoundException(
                                "Document not found"
                        )
                );
    }


    // ==========================================
    // GET USER
    // ==========================================

    private User getUser(
            String email
    ) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );
    }
}