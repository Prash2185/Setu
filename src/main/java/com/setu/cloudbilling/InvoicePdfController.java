package com.setu.cloudbilling;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@RestController
public class InvoicePdfController {

    @Autowired private InvoiceRepository invoiceRepo;
    
    // 🧠 FIX: Database se usage nikalne ke liye yeh nayi line add ki
    @Autowired private UsageEventRepository usageRepo;

    // Rates & Limits
    private final double RATE_PER_MB_STORAGE = 0.005; 
    private final double RATE_PER_MB_EGRESS = 0.015;  
    private final double FREE_STORAGE_MB = 50.0; 
    private final double FREE_EGRESS_MB = 50.0;  

    @GetMapping("/download-real-invoice")
    public ResponseEntity<byte[]> downloadRealInvoice() {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // ==========================================
        // 🚀 STEP 1: ON-THE-FLY BILL CALCULATION
        // ==========================================
        LocalDateTime endOfMonth = LocalDateTime.now();
        LocalDateTime startOfMonth = endOfMonth.minusDays(30);

        Double egressMB = usageRepo.calculateTotalEgress(currentUser, startOfMonth, endOfMonth);
        Double ingressMB = usageRepo.calculateTotalIngress(currentUser, startOfMonth, endOfMonth);

        if (egressMB == null) egressMB = 0.0;
        if (ingressMB == null) ingressMB = 0.0;

        double billableStorage = Math.max(0, ingressMB - FREE_STORAGE_MB);
        double billableEgress = Math.max(0, egressMB - FREE_EGRESS_MB);

        double storageCost = billableStorage * RATE_PER_MB_STORAGE;
        double egressCost = billableEgress * RATE_PER_MB_EGRESS;
        double totalBill = storageCost + egressCost;

        String invNumber = "INV-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        
        // Database mein hamesha NAYA bill save karo
        Invoice latestInvoice = new Invoice(currentUser, invNumber, storageCost, egressCost, totalBill, "PAID (FREE TIER)");
        invoiceRepo.save(latestInvoice);


        // ==========================================
        // 🚀 STEP 2: NAYE BILL KA PDF BANAO
        // ==========================================
        try {
            Document document = new Document();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("ZOHO SETU - ENTERPRISE TAX INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.BLUE)));
            document.add(new Paragraph(" "));
            
            document.add(new Paragraph("Invoice No:  " + latestInvoice.getInvoiceNumber()));
            document.add(new Paragraph("Customer ID: " + currentUser));
            document.add(new Paragraph("Date:        " + latestInvoice.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))));
            document.add(new Paragraph("Status:      " + latestInvoice.getStatus()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("Description", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
            table.addCell(new PdfPCell(new Phrase("Amount (INR)", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));

            table.addCell("Storage Cost (First 50MB Free)");
            table.addCell(String.format("₹ %.4f", latestInvoice.getStorageCost()));

            table.addCell("Egress Cost (First 50MB Free)");
            table.addCell(String.format("₹ %.4f", latestInvoice.getEgressCost()));

            table.addCell(new PdfPCell(new Phrase("Total Amount Payable", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
            
            BaseColor amountColor = (latestInvoice.getTotalAmount() == 0) ? BaseColor.GREEN : BaseColor.RED;
            table.addCell(new PdfPCell(new Phrase(String.format("₹ %.4f", latestInvoice.getTotalAmount()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, amountColor))));

            document.add(table);
            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", latestInvoice.getInvoiceNumber() + ".pdf");
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0L);

            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}