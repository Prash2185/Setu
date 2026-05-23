package com.setu.cloudbilling;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AnalyticsController {

    @Autowired private FileMetadataRepository fileRepo;
    @Autowired private UserPlanRepository planRepo;

    @GetMapping("/analytics")
    public String showUserAnalytics(Model model) {
        // 1. Current User pakdo (SECURITY LOCK 🔐)
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. SIRF is user ka data uthao (Data Isolation)
        List<FileMetadata> myFiles = fileRepo.findByOwner(currentUser);
        UserPlan myPlan = planRepo.findByUsername(currentUser);

        // 3. Core Math for this user only
        long totalFiles = myFiles.size();
        double totalStorage = myFiles.stream().mapToDouble(FileMetadata::getFileSizeMB).sum();
        String planName = (myPlan != null) ? myPlan.getPlanName() : "Free Tier (50 MB)";

        // 4. Chart 1: User ki AI Tags ki ginti (Count)
        Map<String, Long> tagCounts = myFiles.stream()
            .collect(Collectors.groupingBy(f -> f.getFileTag() != null ? f.getFileTag() : "📄 Document", Collectors.counting()));
        
        // 5. Chart 2: NAYA GRAPH! Kis Tag ne kitni MB jagah li hai? (Size in MB)
        Map<String, Double> storageByTag = myFiles.stream()
            .collect(Collectors.groupingBy(
                f -> f.getFileTag() != null ? f.getFileTag() : "📄 Document", 
                Collectors.summingDouble(FileMetadata::getFileSizeMB)
            ));

        // 6. Frontend ko Data Bhejo
        model.addAttribute("totalFiles", totalFiles);
        model.addAttribute("totalStorage", String.format("%.2f", totalStorage));
        model.addAttribute("myPlan", planName);

        model.addAttribute("tagLabels", tagCounts.keySet());
        model.addAttribute("tagData", tagCounts.values());

        model.addAttribute("storageLabels", storageByTag.keySet());
        model.addAttribute("storageData", storageByTag.values());

        return "analytics"; // Yeh analytics.html kholega
    }
}