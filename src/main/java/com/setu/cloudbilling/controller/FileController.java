package com.setu.cloudbilling.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.setu.cloudbilling.entity.InfrastructureEvent;
import com.setu.cloudbilling.repository.InfrastructureEventRepository;
import com.setu.cloudbilling.service.StorageService;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private StorageService storageService;

    @Autowired
    private InfrastructureEventRepository eventRepository;

    // 1. ASYNC UPLOAD API (Ingress Logging)
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, Principal principal) {
        String username = principal.getName();
        long fileSize = file.getSize();

        // Background mein file upload chalu kar do (Async)
        storageService.uploadFileToSupabaseAsync(file, username);

        // Metering: Upload (Ingress) event log karo
        eventRepository.save(new InfrastructureEvent(username, file.getOriginalFilename(), "UPLOAD", fileSize));

        // Turant user ko response de do bina wait kiye (Point 4 Fix)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("File upload processing started in background (202 Accepted).");
    }

    // 2. DOWNLOAD API (Egress Logging Fix - Point 6)
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId, Principal principal) {
        String downloaderUsername = principal.getName();

        // File nikalne ka tera purana logic...
        // Resource file = storageService.getFile(fileId);
        long fileSize = 1048576; // Example: Real file ka size bytes mein nikalna (e.g. 1MB)

        // Metering: Asli Billing Egress pe hoti hai! (Point 6 Fix)
        // Chahe owner download kare ya koi aur, bandwidth consume ho rahi hai
        eventRepository.save(new InfrastructureEvent(downloaderUsername, fileId, "DOWNLOAD", fileSize));

        // File user ko return kar do
        return ResponseEntity.ok()
            // .headers(...)
            // .body(file);
            .build(); // Yahan tu apna purana return laga lena
    }
}
