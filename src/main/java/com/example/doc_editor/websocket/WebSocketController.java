package com.example.doc_editor.websocket;

import com.example.doc_editor.document.Document;
import com.example.doc_editor.document.DocumentRepository;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final DocumentRepository documentRepository;

    public WebSocketController(
            SimpMessagingTemplate messagingTemplate,
            DocumentRepository documentRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.documentRepository = documentRepository;
    }

    @MessageMapping("/edit")
    public void edit(DocumentMessage message) {

        System.out.println("EDIT REQUEST RECEIVED");
        System.out.println("Document ID: " + message.getDocumentId());
        System.out.println("User: " + message.getUserEmail());
        System.out.println("Client Version: " + message.getVersion());

        // Find document
        Document document = documentRepository
                .findById(message.getDocumentId())
                .orElse(null);

        if (document == null) {

            System.out.println("DOCUMENT NOT FOUND");

            return;
        }

        // Existing documents may have NULL version
        Long currentVersion = document.getVersion();

        if (currentVersion == null) {
            currentVersion = 0L;
            document.setVersion(0L);
        }

        System.out.println(
                "Database Version: " + currentVersion
        );

        // Check version
        if (message.getVersion() == null
                || !currentVersion.equals(message.getVersion())) {

            System.out.println("CONFLICT DETECTED");

            message.setType(MessageType.CONFLICT);

            message.setContent(
                    document.getContent()
            );

            message.setVersion(
                    currentVersion
            );

            messagingTemplate.convertAndSend(
                    "/topic/document/" + message.getDocumentId(),
                    message
            );

            return;
        }

        // Version matches
        System.out.println("EDIT ACCEPTED");

        // Update content
        document.setContent(
                message.getContent()
        );

        // Increase version
        document.setVersion(
                currentVersion + 1
        );

        // Save
        documentRepository.save(document);

        System.out.println(
                "Saved Version: " + document.getVersion()
        );

        // Update message
        message.setVersion(
                document.getVersion()
        );

        message.setType(
                MessageType.EDIT
        );

        // Broadcast
        messagingTemplate.convertAndSend(
                "/topic/document/" + message.getDocumentId(),
                message
        );
    }
}