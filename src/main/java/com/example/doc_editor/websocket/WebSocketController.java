package com.example.doc_editor.websocket;

import com.example.doc_editor.document.Document;
import com.example.doc_editor.document.DocumentRepository;
import com.example.doc_editor.document.DocumentService;
import com.example.doc_editor.document.history.DocumentVersionService;
import com.example.doc_editor.user.UserRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final PresenceService presenceService;
    private final DocumentVersionService versionService;
    private final UserRepository userRepository;

    public WebSocketController(
            SimpMessagingTemplate messagingTemplate,
            DocumentRepository documentRepository,
            DocumentService documentService,
            PresenceService presenceService,
            DocumentVersionService versionService,
            UserRepository userRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
        this.presenceService = presenceService;
        this.versionService = versionService;
        this.userRepository = userRepository;
    }

    // =========================
    // JOIN DOCUMENT
    // =========================

    @MessageMapping("/join")
    public void join(DocumentMessage message, Principal principal) {

        String email = getEmail(message, principal);

        if (email == null) {
            return;
        }

        System.out.println("JOIN USER: " + email);
        System.out.println("JOIN DOCUMENT: " + message.getDocumentId());

        if (!documentService.canViewDocument(
                message.getDocumentId(),
                email
        )) {
            return;
        }

        presenceService.join(
                message.getDocumentId(),
                email
        );

        broadcastPresence(
                message.getDocumentId()
        );
    }

    // =========================
    // LEAVE DOCUMENT
    // =========================

    @MessageMapping("/leave")
    public void leave(DocumentMessage message, Principal principal) {

        String email = getEmail(message, principal);

        if (email == null) {
            return;
        }

        presenceService.leave(
                message.getDocumentId(),
                email
        );

        broadcastPresence(
                message.getDocumentId()
        );
    }

    // =========================
    // USER STATUS
    // =========================

    @MessageMapping("/status")
    public void status(DocumentMessage message, Principal principal) {

        String email = getEmail(message, principal);

        if (email == null) {
            return;
        }

        if (!documentService.canViewDocument(
                message.getDocumentId(),
                email
        )) {
            return;
        }

        presenceService.updateStatus(
                message.getDocumentId(),
                email,
                message.getStatus()
        );

        broadcastPresence(
                message.getDocumentId()
        );
    }

    // =========================
    // EDIT DOCUMENT
    // =========================

    @MessageMapping("/edit")
    public void edit(DocumentMessage message, Principal principal) {

        String email = getEmail(message, principal);

        if (email == null) {
            return;
        }

        // Check whether user has EDITOR permission
        if (!documentService.canEditDocument(
                message.getDocumentId(),
                email
        )) {
            return;
        }

        // Get current document
        Document document = documentRepository
                .findById(message.getDocumentId())
                .orElse(null);

        if (document == null) {
            return;
        }

        Long currentVersion = document.getVersion();

        if (currentVersion == null) {
            currentVersion = 0L;
        }

        // =========================
        // VERSION CHECK
        // =========================

        if (message.getVersion() == null
                || !currentVersion.equals(message.getVersion())) {

            sendConflict(
                    message,
                    document,
                    email,
                    currentVersion
            );

            return;
        }

        // =========================
        // ATOMIC UPDATE
        // =========================

        int updatedRows =
                documentRepository.updateContentIfVersionMatches(
                        message.getDocumentId(),
                        message.getContent(),
                        currentVersion
                );

        // Someone else modified the document
        if (updatedRows == 0) {

            Document latestDocument =
                    documentRepository
                            .findById(message.getDocumentId())
                            .orElse(null);

            if (latestDocument != null) {

                sendConflict(
                        message,
                        latestDocument,
                        email,
                        latestDocument.getVersion()
                );
            }

            return;
        }

        // Get updated document
        Document updatedDocument =
                documentRepository
                        .findById(message.getDocumentId())
                        .orElse(null);

        if (updatedDocument == null) {
            return;
        }

        /*
         * IMPORTANT:
         *
         * We DO NOT create a DocumentVersion here.
         *
         * Otherwise every small edit / keystroke would
         * create a new history version.
         *
         * Document.version is still incremented internally
         * because it is used for optimistic concurrency control.
         */

        // Update user's status
        presenceService.updateStatus(
                message.getDocumentId(),
                email,
                "EDITING"
        );

        // Prepare message
        message.setUserEmail(email);
        message.setVersion(updatedDocument.getVersion());
        message.setType(MessageType.EDIT);

        // Broadcast edit to everyone
        messagingTemplate.convertAndSend(
                "/topic/document/" + message.getDocumentId(),
                message
        );

        // Update active users
        broadcastPresence(
                message.getDocumentId()
        );
    }

    // =========================
    // CURSOR MOVEMENT
    // =========================

    @MessageMapping("/cursor")
    public void cursor(
            DocumentMessage message,
            Principal principal
    ) {

        String email = getEmail(message, principal);

        if (email == null) {
            return;
        }

        if (!documentService.canViewDocument(
                message.getDocumentId(),
                email
        )) {
            return;
        }

        message.setUserEmail(email);
        message.setType(MessageType.CURSOR);

        messagingTemplate.convertAndSend(
                "/topic/document/" + message.getDocumentId(),
                message
        );
    }

    // =========================
    // CONFLICT
    // =========================

    private void sendConflict(
            DocumentMessage message,
            Document document,
            String email,
            Long version
    ) {

        message.setType(MessageType.CONFLICT);
        message.setUserEmail(email);
        message.setContent(document.getContent());
        message.setVersion(version);

        messagingTemplate.convertAndSend(
                "/topic/document/" + message.getDocumentId(),
                message
        );
    }

    // =========================
    // PRESENCE
    // =========================

    private void broadcastPresence(Long documentId) {

        messagingTemplate.convertAndSend(
                "/topic/document/" + documentId + "/presence",
                presenceService.getUsers(documentId)
        );
    }

    // =========================
    // GET USER EMAIL
    // =========================

    private String getEmail(
            DocumentMessage message,
            Principal principal
    ) {

        if (principal != null) {
            return principal.getName();
        }

        return message.getUserEmail();
    }
}