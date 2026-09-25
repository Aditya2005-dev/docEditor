package com.example.doc_editor.websocket;

import com.example.doc_editor.document.Document;
import com.example.doc_editor.document.DocumentRepository;
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
    private final PresenceService presenceService;
    private final DocumentVersionService versionService;
    private final UserRepository userRepository;

    public WebSocketController(
            SimpMessagingTemplate messagingTemplate,
            DocumentRepository documentRepository,
            PresenceService presenceService,
            DocumentVersionService versionService,
            UserRepository userRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.documentRepository = documentRepository;
        this.presenceService = presenceService;
        this.versionService = versionService;
        this.userRepository = userRepository;
    }

    // =========================
    // JOIN DOCUMENT
    // =========================

    @MessageMapping("/join")
    public void join(
            DocumentMessage message,
            Principal principal
    ) {

        if (principal == null) {
            return;
        }

        String email = principal.getName();

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

    // =========================
    // USER STATUS
    // =========================

    @MessageMapping("/status")
    public void status(
            DocumentMessage message,
            Principal principal
    ) {

        if (principal == null) {
            return;
        }

        String email = principal.getName();

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
    public void edit(
            DocumentMessage message,
            Principal principal
    ) {

        if (principal == null) {
            return;
        }

        String email = principal.getName();

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
            document.setVersion(0L);
        }

        // =========================
        // VERSION CONFLICT
        // =========================

        if (message.getVersion() == null
                || !currentVersion.equals(message.getVersion())) {

            message.setType(MessageType.CONFLICT);

            message.setUserEmail(email);

            message.setContent(
                    document.getContent()
            );

            message.setVersion(
                    currentVersion
            );

            messagingTemplate.convertAndSend(
                    "/topic/document/"
                            + message.getDocumentId(),
                    message
            );

            return;
        }

        // =========================
        // SAVE EDIT
        // =========================

        document.setContent(
                message.getContent()
        );

        document.setVersion(
                currentVersion + 1
        );

        documentRepository.save(document);

        // =========================
        // SAVE VERSION HISTORY
        // =========================

        User user =
                userRepository
                        .findByEmail(email)
                        .orElse(null);

        if (user != null) {

            versionService.saveVersion(
                    document,
                    user
            );
        }

        // User is currently editing
        presenceService.updateStatus(
                message.getDocumentId(),
                email,
                "EDITING"
        );

        message.setUserEmail(email);

        message.setVersion(
                document.getVersion()
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

    // =========================
    // CURSOR / SELECTION
    // =========================

    @MessageMapping("/cursor")
    public void cursor(
            DocumentMessage message,
            Principal principal
    ) {

        if (principal == null) {
            return;
        }

        String email = principal.getName();

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

    // =========================
    // BROADCAST ACTIVE USERS
    // =========================

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