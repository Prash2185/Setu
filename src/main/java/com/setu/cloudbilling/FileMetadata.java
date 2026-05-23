package com.setu.cloudbilling;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id; // 🚀 NAYA IMPORT TIME KE LIYE

@Entity
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private double fileSizeMB;
    private String owner;
    private String shareId;
    
    // NAYA COLUMN: SMART TAGGING KE LIYE 🤖
    private String fileTag;

    // 🚀 NAYA COLUMN: EXPIRING LINKS KE LIYE (AWS S3 Style)
    private LocalDateTime shareExpiryTime;

    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public double getFileSizeMB() { return fileSizeMB; }
    public void setFileSizeMB(double fileSizeMB) { this.fileSizeMB = fileSizeMB; }
    
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    
    public String getShareId() { return shareId; }
    public void setShareId(String shareId) { this.shareId = shareId; }
    
    public String getFileTag() { return fileTag; }
    public void setFileTag(String fileTag) { this.fileTag = fileTag; }

    // ⏳ Timer wale naye Getters / Setters
    public LocalDateTime getShareExpiryTime() { return shareExpiryTime; }
    public void setShareExpiryTime(LocalDateTime shareExpiryTime) { this.shareExpiryTime = shareExpiryTime; }
}