package com.claudebox.domain.model;

public class Message {
    private String id;
    private String sessionId;
    private String content;
    private String rawContent;
    private boolean isFromUser;
    private long timestamp;

    public Message() {}

    public Message(String id, String sessionId, String content, String rawContent,
                   boolean isFromUser, long timestamp) {
        this.id = id;
        this.sessionId = sessionId;
        this.content = content;
        this.rawContent = rawContent;
        this.isFromUser = isFromUser;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public boolean isFromUser() {
        return isFromUser;
    }

    public void setFromUser(boolean fromUser) {
        isFromUser = fromUser;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
