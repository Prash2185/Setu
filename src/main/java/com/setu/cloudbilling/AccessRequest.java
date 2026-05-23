package com.setu.cloudbilling;

import jakarta.persistence.*;

@Entity
public class AccessRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shareId;
    private String requesterUsername;
    private String ownerUsername;
    private String status = "PENDING"; 

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getShareId() { return shareId; }
    public void setShareId(String shareId) { this.shareId = shareId; }
    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }
    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}