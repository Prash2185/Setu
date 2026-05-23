package com.setu.cloudbilling;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    // Purana method: Agar future mein user ko dashboard par uske saare bills ki history dikhani ho
    List<Invoice> findByUsername(String username);
    
    // 🚀 THE ULTIMATE FIX: PDF generate karne ke liye sabse latest (Naya) bill nikalne wala method
    Invoice findFirstByUsernameOrderByIdDesc(String username);
    
}