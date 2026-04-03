package com.claudebox.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "messages",
    foreignKeys = @ForeignKey(
        entity = SessionEntity.class,
        parentColumns = "id",
        childColumns = "sessionId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("sessionId")
)
public class MessageEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String sessionId;
    private String content;      // HTML content (rendered)
    private String rawContent;   // Raw text
    private boolean isFromUser;
    private long timestamp;

    public MessageEntity() {
        this.id = "";
    }

    @Ignore
    public MessageEntity(@NonNull String id, String sessionId, String content, String rawContent,
                         boolean isFromUser, long timestamp) {
        this.id = id;
        this.sessionId = sessionId;
        this.content = content;
        this.rawContent = rawContent;
        this.isFromUser = isFromUser;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
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
