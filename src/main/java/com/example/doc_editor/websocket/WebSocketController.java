package com.example.doc_editor.websocket;

import com.example.doc_editor.document.Document;
import com.example.doc_editor.document.DocumentRepository;
import com.example.doc_editor.document.DocumentService;
import com.example.doc_editor.document.history.DocumentVersionService;
import com.example.doc_editor.user.User;
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

    // ==========================================
    // JOIN
    // ==========================================

    @MessageMapping("/join")
    public void join(
            DocumentMessage message,
            Principal principal
    ) {

        if (principal == null) {
            return;
        }

        String email = principal.getName();

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

    // ==========================================
    // LEAVE
    // ==========================================

    @MessageMapping("/leave")
    public void leave(
            DocumentMessage message,
            Principal principal
    ) {

        if (principal == null) {
            return;
        }

        String email = principal.getName();

        presenceService.leave(
                message.getDocumentId(),
                email
        );

        broadcastPresence(
                message.getDocumentId()
        );
    }

    // ==========================================
    // STATUS
    // ==========================================

    @MessageMapping("/status")
    public void status(
            DocumentMessage message,
            Principal principal
    ) {

        if (principal == null) {
            return;
        }

        String email = principal.getName();

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

    // ==========================================
    // EDIT
    // ==========================================

    @MessageMapping("/edit")
    public void edit(
            DocumentMessage message,
            Principal principal
    ) {

        if (principal == null) {
            return;
        }

        String email = principal.getName();

        // Only OWNER or EDITOR
        if (!documentService.canEditDocument(
                message.getDocumentId(),
                email
        )) {
            return;
        }

        Document document =
                documentRepository
                        .findById(message.getDocumentId())
                        .orElse(null);

        if (document == null) {
            return;
        }

        Long currentVersion = document.getVersion();

        if (currentVersion == null) {
            currentVersion = 0L;
        }

        // ==========================================
        // BASIC VERSION CHECK
        // ==========================================

        if (message.getVersion() == null
                || !currentVersion.equals(
                        message.getVersion()
                )) {

            sendConflict(
                    message,
                    document,
                    email,
                    currentVersion
            );

            return;
        }

        // ==========================================
        // ATOMIC DATABASE UPDATE
        // ==========================================

        int updatedRows =
                documentRepository.updateContentIfVersionMatches(
                        message.getDocumentId(),
                        message.getContent(),
                        currentVersion
                );

        // Another user updated the document first
        if (updatedRows == 0) {

            Document latestDocument =
                    documentRepository
                            .findById(
                                    message.getDocumentId()
                            )
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

        // ==========================================
        // GET UPDATED DOCUMENT
        // ==========================================

        Document updatedDocument =
                documentRepository
                        .findById(
                                message.getDocumentId()
                        )
                        .orElse(null);

        if (updatedDocument == null) {
            return;
        }

        // ==========================================
        // SAVE VERSION HISTORY
        // ==========================================

        User user =
                userRepository
                        .findByEmail(email)
                        .orElse(null);

        if (user != null) {

            versionService.saveVersion(
                    updatedDocument,
                    user
            );
        }

        // ==========================================
        // UPDATE STATUS
        // ==========================================

        presenceService.updateStatus(
                message.getDocumentId(),
                email,
                "EDITING"
        );

        // ==========================================
        // BROADCAST EDIT
        // ==========================================

        message.setUserEmail(email);

        message.setVersion(
                updatedDocument.getVersion()
        );

        message.setType(
                MessageType.EDIT
        );

        messagingTemplate.convertAndSend(
                "/topic/document/"
                        + message.getDocumentId(),
                message
        );

        broadcastPresence(
                message.getDocumentId()
        );
    }

    // ==========================================
    // CURSOR
    // ==========================================

    @MessageMapping("/cursor")
    public void cursor(
            DocumentMessage message,
            Principal principal
    ) {

        if (principal == null) {
            return;
        }

        String email = principal.getName();

        if (!documentService.canViewDocument(
                message.getDocumentId(),
                email
        )) {
            return;
        }

        message.setUserEmail(email);

        message.setType(
                MessageType.CURSOR
        );

        messagingTemplate.convertAndSend(
                "/topic/document/"
                        + message.getDocumentId(),
                message
        );
    }

    // ==========================================
    // CONFLICT
    // ==========================================

    private void sendConflict(
            DocumentMessage message,
            Document document,
            String email,
            Long version
    ) {

        message.setType(
                MessageType.CONFLICT
        );

        message.setUserEmail(email);

        message.setContent(
                document.getContent()
        );

        message.setVersion(
                version
        );

        messagingTemplate.convertAndSend(
                "/topic/document/"
                        + message.getDocumentId(),
                message
        );
    }

    // ==========================================
    // PRESENCE
    // ==========================================

    private void broadcastPresence(
            Long documentId
    ) {

        messagingTemplate.convertAndSend(
                "/topic/document/"
                        + documentId
                        + "/presence",
                presenceService.getUsers(
                        documentId
                )
        );
    }
}