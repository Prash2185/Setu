package com.setu.cloudbilling;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {

    // 💸 Query 1: Calculate total Egress (Downloads / Bandwidth Used) for the month
    @Query("SELECT COALESCE(SUM(e.bytesTransferred), 0) FROM UsageEvent e WHERE e.username = :username AND e.eventType = 'DOWNLOAD' AND e.timestamp BETWEEN :startDate AND :endDate")
    Double calculateTotalEgress(@Param("username") String username, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // 📦 Query 2: Calculate total Ingress (Uploads) for the month
    @Query("SELECT COALESCE(SUM(e.bytesTransferred), 0) FROM UsageEvent e WHERE e.username = :username AND e.eventType = 'UPLOAD' AND e.timestamp BETWEEN :startDate AND :endDate")
    Double calculateTotalIngress(@Param("username") String username, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 🚀 NAYA METHOD ADMIN DASHBOARD KE LIYE: Operations count karne ke liye (Upload/Download/Delete)
    int countByUsernameAndEventType(String username, String eventType);
}   