package com.setu.cloudbilling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // Yeh Spring Boot ko batata hai ki yeh ek Database Translator hai
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    // Khaali chhod de! Spring Boot baaki ka saara magic (Insert, Update, Delete) khud likh lega.
}