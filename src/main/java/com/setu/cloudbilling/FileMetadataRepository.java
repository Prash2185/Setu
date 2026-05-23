package com.setu.cloudbilling;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    
    // 1. User ki saari files dashboard par dikhane ke liye
    List<FileMetadata> findByOwner(String owner);
    
    // 2. File share karte waqt UUID se file dhoondhne ke liye
    FileMetadata findByShareId(String shareId);
    
    // 3. 🔵 NAYA SENSOR: Download karte waqt File ka size dhoondhne ke liye (Bandwidth Tracking)
    FileMetadata findByFileName(String fileName);
    
}