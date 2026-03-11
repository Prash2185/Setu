package com.setu.cloudbilling;

import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity 
public class FileMetadata {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    private String fileName;
    private Double fileSizeMB;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Double getFileSizeMB() { return fileSizeMB; }
    public void setFileSizeMB(Double fileSizeMB) { this.fileSizeMB = fileSizeMB; }
    public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    
    
    FileMetadata findByFileName(String fileName); 
}
}