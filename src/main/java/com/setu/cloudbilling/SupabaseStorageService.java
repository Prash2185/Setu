package com.setu.cloudbilling;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String supabaseBucket;

    // 1. CLOUD UPLOAD ENGINE
    public String uploadFile(MultipartFile file, String fileName) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String endpoint = supabaseUrl + "/storage/v1/object/" + supabaseBucket + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.set("apikey", supabaseKey);
        
        // File ka type set karna (Image, PDF, etc.)
        String contentType = file.getContentType();
        if (contentType == null) contentType = "application/octet-stream";
        headers.setContentType(MediaType.valueOf(contentType));

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);
        ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, requestEntity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Cloud Upload Failed: " + response.getBody());
        }
        return getPublicUrl(fileName);
    }

    // 2. CLOUD TRASH CAN (DELETE ENGINE)
    public void deleteFile(String fileName) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String endpoint = supabaseUrl + "/storage/v1/object/" + supabaseBucket + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.set("apikey", supabaseKey);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        restTemplate.exchange(endpoint, HttpMethod.DELETE, requestEntity, String.class);
    }

    // 3. PUBLIC URL GENERATOR
    public String getPublicUrl(String fileName) {
        return supabaseUrl + "/storage/v1/object/public/" + supabaseBucket + "/" + fileName;
    }
}