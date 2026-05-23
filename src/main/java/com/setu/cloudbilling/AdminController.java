package com.setu.cloudbilling;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class AdminController {

    // 🚀 THE FIX: Yahan UserRepository ki jagah UserPlanRepository lagana hai
    @Autowired private UserPlanRepository userPlanRepo;
    @Autowired private UsageEventRepository usageRepo;
    @Autowired private FileMetadataRepository fileRepo; 

    // Pricing & Limits
    private final double RATE_PER_MB_STORAGE = 0.005;
    private final double RATE_PER_MB_EGRESS = 0.015;
    private final double FREE_STORAGE_MB = 50.0;
    private final double FREE_EGRESS_MB = 50.0;

    @GetMapping("/admin/usage-report")
    public ModelAndView showAdminDashboard() {
        
        // 🚀 THE FIX: User ki jagah UserPlan ki list uthani hai
        List<UserPlan> allUsers = userPlanRepo.findAll();
        List<Map<String, Object>> reportList = new ArrayList<>();

        LocalDateTime endOfMonth = LocalDateTime.now();
        LocalDateTime startOfMonth = endOfMonth.minusDays(30);

        for (UserPlan u : allUsers) {
            String username = u.getUsername();

            // 1. Storage & Egress Data
            Double egressMB = usageRepo.calculateTotalEgress(username, startOfMonth, endOfMonth);
            Double ingressMB = usageRepo.calculateTotalIngress(username, startOfMonth, endOfMonth);
            if (egressMB == null) egressMB = 0.0;
            if (ingressMB == null) ingressMB = 0.0;

            // 2. Operation Counts
            int uploadCount = usageRepo.countByUsernameAndEventType(username, "UPLOAD");
            int downloadCount = usageRepo.countByUsernameAndEventType(username, "DOWNLOAD");
            int deleteCount = usageRepo.countByUsernameAndEventType(username, "DELETE");
            int totalApiCalls = uploadCount + downloadCount + deleteCount;

            // 3. Current Live Bill Calculation
            double billableStorage = Math.max(0, ingressMB - FREE_STORAGE_MB);
            double billableEgress = Math.max(0, egressMB - FREE_EGRESS_MB);
            double totalBill = (billableStorage * RATE_PER_MB_STORAGE) + (billableEgress * RATE_PER_MB_EGRESS);

            // 4. Map the data
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", username);
            userData.put("ingressMB", String.format("%.2f", ingressMB));
            userData.put("egressMB", String.format("%.2f", egressMB));
            userData.put("apiCalls", totalApiCalls);
            userData.put("totalBill", String.format("%.4f", totalBill));

            reportList.add(userData);
        }

        ModelAndView mav = new ModelAndView("admin-report");
        mav.addObject("reports", reportList);
        return mav;
    }
}