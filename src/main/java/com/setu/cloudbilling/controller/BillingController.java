package com.setu.cloudbilling.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.setu.cloudbilling.entity.InfrastructureEvent;
import com.setu.cloudbilling.repository.InfrastructureEventRepository;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    @Autowired
    private InfrastructureEventRepository eventRepository;

    // Cloud Pricing (Example: ₹0.5 per MB)
    private static final double COST_PER_MB = 0.5;

    @GetMapping("/invoice/current")
    public ResponseEntity<Map<String, Object>> generateUsageInvoice(Principal principal) {
        String username = principal.getName();
        List<InfrastructureEvent> events = eventRepository.findByUsername(username);

        long totalIngressBytes = 0;
        long totalEgressBytes = 0;

        for (InfrastructureEvent event : events) {
            if ("UPLOAD".equals(event.getEventType())) {
                totalIngressBytes += event.getBytesTransferred();
            } else if ("DOWNLOAD".equals(event.getEventType())) {
                totalEgressBytes += event.getBytesTransferred();
            }
        }

        // Convert Bytes to MB
        double totalIngressMB = totalIngressBytes / (1024.0 * 1024.0);
        double totalEgressMB = totalEgressBytes / (1024.0 * 1024.0);
        double totalUsageMB = totalIngressMB + totalEgressMB;

        // Calculate Usage-Based Bill
        double totalBillAmount = totalUsageMB * COST_PER_MB;

        // JSON Response for Dashboard
        Map<String, Object> invoice = new HashMap<>();
        invoice.put("username", username);
        invoice.put("billingCycle", "Current Month");
        invoice.put("totalIngressMB", String.format("%.2f", totalIngressMB));
        invoice.put("totalEgressMB", String.format("%.2f", totalEgressMB));
        invoice.put("totalCostINR", String.format("%.2f", totalBillAmount));
        invoice.put("billingModel", "Dynamic Usage-Based (Pay-As-You-Go)");

        return ResponseEntity.ok(invoice);
    }
}
