package com.setu.cloudbilling.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access")
public class AccessController {

    // Jab owner kisi peer ka access PENDING se APPROVED karega
    @PostMapping("/approve/{shareId}/{requesterEmail}")
    public ResponseEntity<Map<String, String>> approveAccessAndGenerateContract(
            @PathVariable String shareId,
            @PathVariable String requesterEmail) {

        // 1. Simulating Zoho Sign Document Generation
        String zohoContractId = "ZOHO-SIGN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 2. Ideally, here you update the database status to 'APPROVED'
        // accessRepository.updateStatus(shareId, requesterEmail, "APPROVED");

        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Access approved. Automated Zoho Sign NDA dispatched to " + requesterEmail);
        response.put("zohoContractId", zohoContractId);
        response.put("integration", "Zoho Sign API (v2)");

        return ResponseEntity.ok(response);
    }
}
