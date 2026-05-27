package com.setu.cloudbilling.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.setu.cloudbilling.entity.InfrastructureEvent;

public interface InfrastructureEventRepository extends JpaRepository<InfrastructureEvent, Long> {
    // End of month billing calculate karne ke liye ye method kaam aayega
    List<InfrastructureEvent> findByUsername(String username);
}
