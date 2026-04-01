package com.claudebox.domain.model;

public class Session {
    private String id;
    private String name;
    private long createdAt;
    private long lastActiveAt;

    public Session() {}

    public Session(String id, String name, long createdAt, long lastActiveAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
