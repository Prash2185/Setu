package com.setu.cloudbilling;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BillingEngineService {

    @Autowired private UsageEventRepository usageRepo;
    @Autowired private InvoiceRepository invoiceRepo;

    private final double RATE_PER_MB_STORAGE = 0.005; 
    private final double RATE_PER_MB_EGRESS = 0.015;  

    // 🎁 50 MB Free Quota
    private final double FREE_STORAGE_MB = 50.0; 
    private final double FREE_EGRESS_MB = 50.0;  

    @GetMapping("/api/billing/run-now")
    public String forceRunBillingNow() {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        
        LocalDateTime endOfMonth = LocalDateTime.now();
        LocalDateTime startOfMonth = endOfMonth.minusDays(30);

        Double egressMB = usageRepo.calculateTotalEgress(currentUser, startOfMonth, endOfMonth);
        Double ingressMB = usageRepo.calculateTotalIngress(currentUser, startOfMonth, endOfMonth);

        if (egressMB == null) egressMB = 0.0;
        if (ingressMB == null) ingressMB = 0.0;

        // 🧠 The Math: 50MB minus karo, agar minus mein gaya toh 0 maano
        double billableStorage = Math.max(0, ingressMB - FREE_STORAGE_MB);
        double billableEgress = Math.max(0, egressMB - FREE_EGRESS_MB);

        double storageCost = billableStorage * RATE_PER_MB_STORAGE;
        double egressCost = billableEgress * RATE_PER_MB_EGRESS;
        double totalBill = storageCost + egressCost;

        String invNumber = "INV-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        
        // Save the Bill!
        Invoice newInvoice = new Invoice(currentUser, invNumber, storageCost, egressCost, totalBill, "PAID (FREE TIER)");
        invoiceRepo.save(newInvoice);

        return "<div style='text-align:center; padding: 50px; font-family: sans-serif;'>" +
               "<h1 style='color:green;'>✅ NEW BILL GENERATED!</h1>" +
               "<h3>Invoice Number: " + invNumber + "</h3>" +
               "<h3>Total Amount: ₹ " + totalBill + "</h3>" +
               "<a href='/dashboard' style='padding: 10px 20px; background: blue; color: white; border-radius: 5px;'>Go to Dashboard & Download PDF</a>" +
               "</div>";
    }
}