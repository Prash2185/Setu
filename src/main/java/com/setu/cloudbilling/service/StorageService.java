package com.setu.cloudbilling.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.setu.cloudbilling.SupabaseStorageService;

@Service
public class StorageService {

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @Async // <--- YEH JADOO HAI (Thread decoupling)
    public CompletableFuture<String> uploadFileToSupabaseAsync(MultipartFile file, String username) {
        try {
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null) originalFileName = "upload-" + System.currentTimeMillis();
            String objectPath = username + "/" + System.currentTimeMillis() + "_" + originalFileName;

            String publicUrl = supabaseStorageService.uploadFile(file, objectPath);
            return CompletableFuture.completedFuture(publicUrl);
        } catch (Exception e) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }
}
