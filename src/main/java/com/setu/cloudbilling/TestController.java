package com.setu.cloudbilling;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
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
    @ResponseBody
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

        // 🚀 Apply free 50 MB allowance before billing
        double billableStorage = Math.max(0, totalMB - 50.0);
        double totalBill = billableStorage * userPlan.getRatePerMB();
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
    @ResponseBody
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

            // ==========================================
            // 🗂️ THE FINAL BOSS: SMART OBJECT VERSIONING
            // ==========================================
            String cleanOriginalName = originalFileName.replaceAll("\\s+", "_");
            String baseName = cleanOriginalName;
            String extension = "";

            int dotIndex = cleanOriginalName.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = cleanOriginalName.substring(0, dotIndex);
                extension = cleanOriginalName.substring(dotIndex);
            }

            int versionCount = 0;
            for (FileMetadata f : userFiles) {
                if (f.getFileName().contains(baseName)) {
                    versionCount++;
                }
            }

            String versionedFileName = cleanOriginalName;
            if (versionCount > 0) {
                versionedFileName = baseName + "_v" + (versionCount + 1) + extension;
            }

            String finalFileName = System.currentTimeMillis() + "_" + versionedFileName;
            // ==========================================

            
            // 🛡️ SECURITY FIX 1: Supabase mein folder bana kar upload karo
            String objectPath = dbUser + "/" + finalFileName;
            supabaseStorageService.uploadFile(file, objectPath);
            
            FileMetadata metaData = new FileMetadata();
            metaData.setFileName(finalFileName); // Database mein file ka naam clean rahega
            metaData.setFileSizeMB(newFileMB);
            metaData.setOwner(dbUser);
            metaData.setShareId(UUID.randomUUID().toString());
            metaData.setFileTag(generateSmartTag(originalFileName));
            metaData.setShareExpiryTime(LocalDateTime.now().plusHours(24));


            
            repository.save(metaData); 

            // 🟢 SENSOR 1: Log the UPLOAD Event
            usageEventRepository.save(new UsageEvent(dbUser, "UPLOAD", finalFileName, newFileMB, LocalDateTime.now()));

            return "<script>window.location.href='/dashboard';</script>";
            
        } catch (Exception e) { return "ERROR: " + e.getMessage(); }
    }

    @GetMapping("/download/{fileName}")
    public RedirectView downloadFile(@PathVariable String fileName) { 
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        
        FileMetadata fileInfo = repository.findByFileName(fileName);
        if (fileInfo == null) return new RedirectView("/dashboard"); // Agar file nahi mili toh wapas bhej do
        
        double egressMB = fileInfo.getFileSizeMB();

        // 🔵 SENSOR 2: Log the DOWNLOAD (Egress) Event
        usageEventRepository.save(new UsageEvent(currentUser, "DOWNLOAD", fileName, egressMB, LocalDateTime.now()));

        // 🛡️ SECURITY FIX 2: Supabase se folder ke andar se URL mangwao
        String objectPath = fileInfo.getOwner() + "/" + fileName;
        return new RedirectView(supabaseStorageService.getPublicUrl(objectPath)); 
    }

    @GetMapping("/delete/{id}")
    @ResponseBody
    public String deleteFile(@PathVariable Long id) {
        try {
            Optional<FileMetadata> optionalFile = repository.findById(id);
            if (optionalFile.isPresent()) {
                FileMetadata fileData = optionalFile.get();
                String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
                
                if (fileData.getOwner().equals(currentUser)) {
                    
                    // 🛡️ SECURITY FIX 3: Supabase folder mein ghus kar file delete karo
                    String objectPath = currentUser + "/" + fileData.getFileName();
                    supabaseStorageService.deleteFile(objectPath);
                    
                    repository.delete(fileData);

                    // 🔴 SENSOR 3: Log the DELETE Event
                    usageEventRepository.save(new UsageEvent(currentUser, "DELETE", fileData.getFileName(), fileData.getFileSizeMB(), LocalDateTime.now()));
                }
            }
            return "<script>window.location.href='/dashboard';</script>";
        } catch (Exception e) { return "ERROR: " + e.getMessage(); }
    }

    @PostMapping("/upgrade-plan")
    @ResponseBody
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

    @GetMapping("/share/{id}")
    public String sharePlan(@PathVariable String id, Model model) {
        FileMetadata fileData = repository.findByShareId(id);
        if (fileData == null) {
            model.addAttribute("error", "Invalid Link");
            return "share";
        }
        if (fileData.getShareExpiryTime() != null && LocalDateTime.now().isAfter(fileData.getShareExpiryTime())) {
            model.addAttribute("expired", true);
            return "share";
        }

        // Safely detect authenticated user (may be anonymous)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = null;
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            currentUser = auth.getName();
        }

        // Add file data and metadata to model for the Thymeleaf page to render
        model.addAttribute("fileData", fileData);
        model.addAttribute("isOwner", currentUser != null && currentUser.equals(fileData.getOwner()));

        // Access request status (if user logged in)
        if (currentUser != null) {
            AccessRequest req = accessRequestRepository.findByShareIdAndRequesterUsername(id, currentUser);
            if (req == null) model.addAttribute("accessStatus", "NONE");
            else model.addAttribute("accessStatus", req.getStatus());
        }

        return "share";
    }

    @PostMapping("/request-access/{uuid}")
    @ResponseBody
    public String sendRequest(@PathVariable String uuid) {
        FileMetadata file = repository.findByShareId(uuid);
        AccessRequest req = new AccessRequest(); req.setShareId(uuid); req.setRequesterUsername(SecurityContextHolder.getContext().getAuthentication().getName()); req.setOwnerUsername(file.getOwner());
        accessRequestRepository.save(req); return "<h3>🚀 Request Sent!</h3><a href='/share/" + uuid + "'>Back</a>";
    }

    @GetMapping("/my-requests")
        public ModelAndView viewRequests() {
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // Database se saari pending requests nikaalo
            List<AccessRequest> requests = accessRequestRepository.findByOwnerUsernameAndStatus(currentUser, "PENDING");
        
        // Isko my-requests.html page par bhej do
            ModelAndView mav = new ModelAndView("my-requests");
            mav.addObject("requests", requests);
            return mav;
    }

    @GetMapping("/approve-request/{id}")
    @ResponseBody
    public String approve(@PathVariable Long id) { AccessRequest r = accessRequestRepository.findById(id).get(); r.setStatus("APPROVED"); accessRequestRepository.save(r); return "<h3>✅ Approved!</h3><a href='/my-requests'>Back</a>"; }

    @GetMapping("/reject-request/{id}")
    @ResponseBody
    public String reject(@PathVariable Long id) { AccessRequest r = accessRequestRepository.findById(id).get(); r.setStatus("REJECTED"); accessRequestRepository.save(r); return "<h3>❌ Rejected!</h3><a href='/my-requests'>Back</a>"; }

    @GetMapping("/signup")
    @ResponseBody
    public String signup(@RequestParam String username, @RequestParam String password) { 
        User newUser = new User(); newUser.setUsername(username); newUser.setPassword(passwordEncoder.encode(password)); userRepository.save(newUser); return "<h3>Account Created! <a href='/login'>Login Here</a></h3>"; 
    }
}