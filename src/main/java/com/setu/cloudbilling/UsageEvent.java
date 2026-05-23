package com.setu.cloudbilling;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usage_events")
public class UsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    
    // "UPLOAD", "DOWNLOAD", or "DELETE"
    private String eventType; 
    
    private String fileName;
    
    // Tracking in MBs for billing
    private Double bytesTransferred; 
    
    private LocalDateTime timestamp;

    // Default Constructor (Required by JPA/Hibernate)
    public UsageEvent() {}

    // Master Constructor
    public UsageEvent(String username, String eventType, String fileName, Double bytesTransferred, LocalDateTime timestamp) {
        this.username = username;
        this.eventType = eventType;
        this.fileName = fileName;
        this.bytesTransferred = bytesTransferred;
        this.timestamp = timestamp;
    }

    // --- GETTERS & SETTERS ---
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Double getBytesTransferred() { return bytesTransferred; }
    public void setBytesTransferred(Double bytesTransferred) { this.bytesTransferred = bytesTransferred; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}