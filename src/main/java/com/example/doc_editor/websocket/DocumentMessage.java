package com.example.doc_editor.websocket;

public class DocumentMessage {

    private Long documentId;

    private String userEmail;

    private String content;

    private Long version;

    private MessageType type;

    // VIEWING / EDITING
    private String status;

    // Cursor position
    private Integer cursorPosition;

    // Text selection
    private Integer selectionStart;

    private Integer selectionEnd;

    public DocumentMessage() {
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(Integer cursorPosition) {
        this.cursorPosition = cursorPosition;
    }

    public Integer getSelectionStart() {
        return selectionStart;
    }

    public void setSelectionStart(Integer selectionStart) {
        this.selectionStart = selectionStart;
    }

    public Integer getSelectionEnd() {
        return selectionEnd;
    }

    public void setSelectionEnd(Integer selectionEnd) {
        this.selectionEnd = selectionEnd;
    }
}