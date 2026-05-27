package com.setu.cloudbilling.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "infrastructure_events")
public class InfrastructureEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username; // Jis user ne file download/upload ki
    private String fileId;   // Kaunsi file
    private String eventType; // "UPLOAD" (Ingress) ya "DOWNLOAD" (Egress)
    private long bytesTransferred; // File ka size bytes mein
    private LocalDateTime timestamp;

    public InfrastructureEvent() {}

    public InfrastructureEvent(String username, String fileId, String eventType, long bytesTransferred) {
        this.username = username;
        this.fileId = fileId;
        this.eventType = eventType;
        this.bytesTransferred = bytesTransferred;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getBytesTransferred() {
        return bytesTransferred;
    }

    public void setBytesTransferred(long bytesTransferred) {
        this.bytesTransferred = bytesTransferred;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}