package com.setu.cloudbilling;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile; // YEH NAYA IMPORT HAI

@RestController
public class TestController {

    @Autowired
    private FileMetadataRepository repository;

    @GetMapping(value = "/", produces = "text/html")
    public String showUploadForm() {
        return "<html><body style='font-family: sans-serif;'>" +
               "<h2>Zoho SETU: Cloud Engine</h2>" +
               "<form method='POST' action='/upload' enctype='multipart/form-data'>" +
               "<input type='file' name='file' /><br/><br/>" +
               "<input type='submit' value='Upload & Save to Database' />" +
               "</form>" +
               "<br><br><a href='/invoice' target='_blank'><button>Generate Monthly Bill</button></a>" +
               "</body></html>";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String folderPath = "D:/CloudStorage/"; 
            String filePath = folderPath + file.getOriginalFilename();
            file.transferTo(new File(filePath));
            
            long bytes = file.getSize(); 
            double megabytes = (double) bytes / (1024 * 1024); 
            
            FileMetadata metaData = new FileMetadata();
            metaData.setFileName(file.getOriginalFilename());
            metaData.setFileSizeMB(megabytes);
            repository.save(metaData); 
            
            return "<h3>SUCCESS! File Uploaded & Logged.</h3>" +
                   "<a href='/'>Go Back</a> | <a href='/invoice'>See Bill</a>";
            
        } catch (IOException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // ==========================================
    // THE FINAL BOSS: BILLING ENGINE
    // ==========================================
    @GetMapping(value = "/invoice", produces = "text/html")
    public String generateInvoice() {
        // Magic Line: Database se saari entry utha kar le aao!
        List<FileMetadata> allFiles = repository.findAll(); 

        double totalSizeMB = 0;
        StringBuilder fileDetails = new StringBuilder();

        // Har file ko loop mein ghumao aur total size calculate karo
        for (FileMetadata file : allFiles) {
            totalSizeMB += file.getFileSizeMB();
            fileDetails.append("<li>").append(file.getFileName())
                       .append(" : <b>").append(String.format("%.2f", file.getFileSizeMB())).append(" MB</b></li>");
        }

        // Pricing Logic (₹ 15 per MB)
        double ratePerMB = 15.0; 
        double totalBill = totalSizeMB * ratePerMB;

        // Invoice ka HTML Design
        return "<html><body style='font-family: monospace; background-color: #f4f4f4; padding: 20px;'>" +
               "<div style='background: white; padding: 20px; border: 1px dashed black; width: 400px;'>" +
               "<h2>🧾 ZOHO SETU - TAX INVOICE</h2>" +
               "<hr/>" +
               "<b>Customer:</b> System Admin<br/>" +
               "<b>Billing Cycle:</b> March 2026<br/>" +
               "<hr/>" +
               "<h3>Usage Details:</h3>" +
               "<ul>" + fileDetails.toString() + "</ul>" +
               "<hr/>" +
               "<p><b>Total Storage Used:</b> " + String.format("%.2f", totalSizeMB) + " MB</p>" +
               "<p><b>Rate:</b> ₹ 15.00 / MB</p>" +
               "<h2 style='color: green;'>TOTAL AMOUNT DUE: ₹ " + String.format("%.2f", totalBill) + "</h2>" +
               "</div></body></html>";
    }
}