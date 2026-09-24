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
            DocumentRepository documentRepository) {

        this.messagingTemplate = messagingTemplate;
        this.documentRepository = documentRepository;
    }

    @MessageMapping("/edit")
    public void edit(DocumentMessage message) {

        // Find document
        Document document =
                documentRepository
                        .findById(message.getDocumentId())
                        .orElse(null);

        if (document == null) {
            return;
        }

        // Update document content
        document.setContent(message.getContent());

        // Save to MySQL
        documentRepository.save(document);


        // Send update to other users
        String destination =
                "/topic/document/" + message.getDocumentId();

        messagingTemplate.convertAndSend(
                destination,
                message
        );
    }
}