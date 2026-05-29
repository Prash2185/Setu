package com.setu.cloudbilling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class TestController {

    @Autowired private FileMetadataRepository repository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AccessRequestRepository accessRequestRepository;
    @Autowired private SupabaseStorageService supabaseStorageService; 
    @Autowired private UserPlanRepository userPlanRepository;
    
    // 📊 NAYA ENGINE: Metering Ledger
    @Autowired private UsageEventRepository usageEventRepository;

    private String getDisplayName(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            if (oauth2User.getAttributes().containsKey("name")) return oauth2User.getAttribute("name").toString();
            if (oauth2User.getAttributes().containsKey("email")) return oauth2User.getAttribute("email").toString();
        }
        return authentication.getName(); 
    }

    private String generateSmartTag(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.contains("resume") || lowerName.contains("cv") || lowerName.contains("portfolio")) return "👔 Resume/CV";
        else if (lowerName.contains("bill") || lowerName.contains("invoice") || lowerName.contains("receipt") || lowerName.contains("tax")) return "💰 Invoice/Bill";
        else if (lowerName.contains("id") || lowerName.contains("aadhar") || lowerName.contains("pan") || lowerName.contains("passport")) return "🪪 ID Card";
        else if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif")) return "🖼️ Image";
        else if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".avi")) return "🎬 Video";
        else if (lowerName.endsWith(".zip") || lowerName.endsWith(".rar")) return "🗜️ Archive";
        else return "📄 Document"; 
    }

    @GetMapping("/login")
    public ModelAndView showLoginPage() { 
        return new ModelAndView("login"); 
    }

    @GetMapping(value = "/", produces = "text/html")
    public String showUploadForm(Authentication authentication) {
        return "<script>window.location.href='/dashboard';</script>";
    }

    @GetMapping("/dashboard")
    public ModelAndView showModernDashboard(Authentication authentication) {
        String dbUser = authentication.getName(); 
        String displayName = getDisplayName(authentication);
        
        UserPlan userPlan = userPlanRepository.findByUsername(dbUser);
        if (userPlan == null) {
            userPlan = new UserPlan();
            userPlan.setUsername(dbUser);
            userPlan.setPlanName("Free Tier (50 MB)");
            userPlan.setMaxStorageMB(50.0);
            userPlan.setRatePerMB(0.0000);
            userPlanRepository.save(userPlan);
        }

        List<FileMetadata> userFiles = repository.findByOwner(dbUser);
        double totalMB = 0;
        for(FileMetadata f : userFiles) totalMB += f.getFileSizeMB();

        double totalBill = totalMB * userPlan.getRatePerMB();
        double storagePercent = (totalMB / userPlan.getMaxStorageMB()) * 100;

        ModelAndView mav = new ModelAndView("dashboard");
        mav.addObject("username", displayName.toUpperCase());
        mav.addObject("files", userFiles);
        mav.addObject("totalMB", String.format("%.2f", totalMB));
        mav.addObject("totalBill", String.format("%.4f", totalBill)); 
        mav.addObject("activePlan", userPlan.getPlanName());
        mav.addObject("ratePerMB", String.valueOf(userPlan.getRatePerMB()));
        mav.addObject("planLimit", String.format("%.0f", userPlan.getMaxStorageMB()));
        mav.addObject("storagePercent", Math.min(storagePercent, 100)); 

        return mav;
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String dbUser = SecurityContextHolder.getContext().getAuthentication().getName();
            UserPlan userPlan = userPlanRepository.findByUsername(dbUser);
            
            List<FileMetadata> userFiles = repository.findByOwner(dbUser);
            double currentTotalMB = 0;
            for(FileMetadata f : userFiles) currentTotalMB += f.getFileSizeMB();
            
            double newFileMB = (double) file.getSize() / (1024 * 1024); 
            
            if (currentTotalMB + newFileMB > userPlan.getMaxStorageMB()) {
                return "<div style='font-family: sans-serif; text-align:center; padding: 50px;'><h1 style='color:red;'>🚨 Storage Limit Exceeded!</h1><h3>Your current <b>" + userPlan.getPlanName() + "</b> only allows up to " + userPlan.getMaxStorageMB() + " MB.</h3><p>You are trying to upload a " + String.format("%.2f", newFileMB) + " MB file, which crosses your limit.</p><a href='/dashboard'><button style='padding:15px; background:blue; color:white; font-size:18px; border:none; border-radius:10px; cursor:pointer;'>Go Back & Upgrade Plan</button></a></div>";
            }

            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.contains("..")) return "Invalid File";

            String finalFileName = System.currentTimeMillis() + "_" + originalFileName.replaceAll("\\s+", "_");
            supabaseStorageService.uploadFile(file, finalFileName);
            
            FileMetadata metaData = new FileMetadata();
            metaData.setFileName(finalFileName); 
            metaData.setFileSizeMB(newFileMB);
            metaData.setOwner(dbUser);
            metaData.setShareId(UUID.randomUUID().toString());
            metaData.setFileTag(generateSmartTag(originalFileName));
            
            repository.save(metaData); 

            // 🟢 SENSOR 1: Log the UPLOAD Event
            usageEventRepository.save(new UsageEvent(dbUser, "UPLOAD", finalFileName, newFileMB, LocalDateTime.now()));

            return "<script>window.location.href='/dashboard';</script>";
            
        } catch (Exception e) { return "ERROR: " + e.getMessage(); }
    }

    @GetMapping("/download/{fileName}")
    public RedirectView downloadFile(@PathVariable String fileName) { 
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Find file to get its size for Bandwidth tracking
        FileMetadata fileInfo = repository.findByFileName(fileName);
        double egressMB = (fileInfo != null) ? fileInfo.getFileSizeMB() : 0.0;

        // 🔵 SENSOR 2: Log the DOWNLOAD (Egress) Event
        usageEventRepository.save(new UsageEvent(currentUser, "DOWNLOAD", fileName, egressMB, LocalDateTime.now()));

        return new RedirectView(supabaseStorageService.getPublicUrl(fileName)); 
    }

    @GetMapping("/delete/{id}")
    public String deleteFile(@PathVariable Long id) {
        try {
            Optional<FileMetadata> optionalFile = repository.findById(id);
            if (optionalFile.isPresent()) {
                FileMetadata fileData = optionalFile.get();
                String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
                
                if (fileData.getOwner().equals(currentUser)) {
                    supabaseStorageService.deleteFile(fileData.getFileName());
                    repository.delete(fileData);

                    // 🔴 SENSOR 3: Log the DELETE Event
                    usageEventRepository.save(new UsageEvent(currentUser, "DELETE", fileData.getFileName(), fileData.getFileSizeMB(), LocalDateTime.now()));
                }
            }
            return "<script>window.location.href='/dashboard';</script>";
        } catch (Exception e) { return "ERROR: " + e.getMessage(); }
    }

    @PostMapping("/upgrade-plan")
    public String upgradePlan(@RequestParam("planType") String planType) {
        String dbUser = SecurityContextHolder.getContext().getAuthentication().getName();
        UserPlan userPlan = userPlanRepository.findByUsername(dbUser);
        switch (planType) {
            case "Lite": userPlan.setPlanName("Lite Plan (30 GB)"); userPlan.setMaxStorageMB(30720); userPlan.setRatePerMB(0.0019); break;
            case "Basic": userPlan.setPlanName("Basic Plan (100 GB)"); userPlan.setMaxStorageMB(102400); userPlan.setRatePerMB(0.0013); break;
            case "Standard": userPlan.setPlanName("Standard Plan (200 GB)"); userPlan.setMaxStorageMB(204800); userPlan.setRatePerMB(0.00105); break;
            case "Premium": userPlan.setPlanName("Premium Plan (2 TB)"); userPlan.setMaxStorageMB(2097152); userPlan.setRatePerMB(0.00032); break;
        }
        userPlanRepository.save(userPlan);
        return "<script>window.location.href='/dashboard';</script>";
    }

    @GetMapping(value = "/share/{uuid}", produces = "text/html")
    public String shareFileSecurely(@PathVariable String uuid) {
        FileMetadata fileData = repository.findByShareId(uuid);
        if (fileData == null) return "<h3>⚠️ Invalid Link!</h3>";
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (currentUser.equals(fileData.getOwner())) return "<script>window.location.href='/download/" + fileData.getFileName() + "';</script>";
        AccessRequest req = accessRequestRepository.findByShareIdAndRequesterUsername(uuid, currentUser);
        if (req == null) return "<h2>🔒 Restricted</h2><form method='POST' action='/request-access/" + uuid + "'><button type='submit'>Request Access</button></form>";
        else if (req.getStatus().equals("PENDING")) return "<h3>⏳ Pending...</h3>";
        else if (req.getStatus().equals("APPROVED")) return "<script>window.location.href='/download/" + fileData.getFileName() + "';</script>";
        else return "<h3 style='color:red;'>❌ Rejected.</h3>"; 
    }

    @PostMapping("/request-access/{uuid}")
    public String sendRequest(@PathVariable String uuid) {
        FileMetadata file = repository.findByShareId(uuid);
        AccessRequest req = new AccessRequest(); req.setShareId(uuid); req.setRequesterUsername(SecurityContextHolder.getContext().getAuthentication().getName()); req.setOwnerUsername(file.getOwner());
        accessRequestRepository.save(req); return "<h3>🚀 Request Sent!</h3><a href='/share/" + uuid + "'>Back</a>";
    }

    @GetMapping(value = "/my-requests", produces = "text/html")
    public String viewRequests() {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        List<AccessRequest> requests = accessRequestRepository.findByOwnerUsernameAndStatus(currentUser, "PENDING");
        StringBuilder html = new StringBuilder("<h2>📩 Pending Requests</h2><ul>");
        for (AccessRequest r : requests) html.append("<li>User ID <b>").append(r.getRequesterUsername()).append("</b> wants access. <a href='/approve-request/").append(r.getId()).append("'>[✅ Yes]</a> <a href='/reject-request/").append(r.getId()).append("' style='color:red;'>[❌ No]</a></li><br>");
        return html.append("</ul><br><a href='/dashboard'>Back</a>").toString();
    }

    @GetMapping("/approve-request/{id}")
    public String approve(@PathVariable Long id) { AccessRequest r = accessRequestRepository.findById(id).get(); r.setStatus("APPROVED"); accessRequestRepository.save(r); return "<h3>✅ Approved!</h3><a href='/my-requests'>Back</a>"; }

    @GetMapping("/reject-request/{id}")
    public String reject(@PathVariable Long id) { AccessRequest r = accessRequestRepository.findById(id).get(); r.setStatus("REJECTED"); accessRequestRepository.save(r); return "<h3>❌ Rejected!</h3><a href='/my-requests'>Back</a>"; }

    @GetMapping("/signup")
    public String signup(@RequestParam String username, @RequestParam String password) { 
        User newUser = new User(); newUser.setName(username); newUser.setPassword(passwordEncoder.encode(password)); userRepository.save(newUser); return "<h3>Account Created! <a href='/login'>Login Here</a></h3>"; 
    }
}