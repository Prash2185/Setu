package com.setu.cloudbilling;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    // 🔑 Google Gemini API Key
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Autowired private FileMetadataRepository fileRepo;

    @PostMapping("/chat")
    public String chatWithCloud(@RequestBody String userMessage) {
        try {
            // 1. Current User aur uski Files nikalo
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            List<FileMetadata> files = fileRepo.findByOwner(currentUser);
            
            // 2. 🧠 THE SMART PROMPT (Ab AI Faltu bakwas nahi karega)
            StringBuilder context = new StringBuilder("You are SETU AI, an advanced cloud storage assistant. ");
            context.append("Current user: ").append(currentUser).append(". ");
            context.append("Total files: ").append(files.size()).append(". ");
            
            if(!files.isEmpty()) {
                context.append("File list: ");
                for(FileMetadata f : files) {
                    context.append("[Name: '").append(f.getFileName()).append("', Tag: '").append(f.getFileTag()).append("'] ");
                }
            }
            
            // 🛑 STRICT RULES TO STOP OVER-ACTING
            context.append(". STRICT RULES FOR YOUR RESPONSE: ");
            context.append("1. DO NOT greet the user (No 'Hi', 'Hello', etc.). Answer the prompt directly. ");
            context.append("2. Be extremely concise and professional. Do not use conversational filler. ");
            context.append("3. If the user sends a short reply like 'ok', 'no', 'yes', or 'thanks', just acknowledge it briefly (e.g., 'Acknowledged.' or 'Ready when you are.'). Do not ask what they mean. ");
            context.append("4. Never use markdown formatting like asterisks or bold text. ");
            
            context.append("User Prompt: ").append(userMessage);

            // 3. 🚀 Google Gemini 2.5 Flash Model
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // JSON Text Formatting (Taaki quotes clash na karein)
            String safeContext = context.toString().replace("\"", "\\\"").replace("\n", " ");
            String requestBody = "{\"contents\": [{\"parts\": [{\"text\": \"" + safeContext + "\"}]}]}";
            
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            String response = restTemplate.postForObject(url, request, String.class);

            // 4. Gemini ka JSON Parse karke Reply Nikalna
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            String aiReply = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");

            return aiReply;

        } catch (Exception e) {
            return "System Error: " + e.getMessage();
        }
    }
}