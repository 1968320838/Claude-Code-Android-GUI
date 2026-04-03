package com.claudebox.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "sessions")
public class SessionEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private long createdAt;
    private long lastActiveAt;

    public SessionEntity() {
        this.id = "";
    }

    @Ignore
    public SessionEntity(@NonNull String id, String name, long createdAt, long lastActiveAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(long lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
