package com.setu.cloudbilling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional; // NAYA IMPORT

@RestController
public class TestController {

    @Autowired
    private FileMetadataRepository repository;

    @GetMapping(value = "/", produces = "text/html")
    public String showUploadForm() {
        return "<html><body style='font-family: sans-serif; padding: 20px;'>" +
               "<h2>☁️ Zoho SETU: Cloud Engine</h2>" +
               "<form method='POST' action='/upload' enctype='multipart/form-data'>" +
               "<input type='file' name='file' required /><br/><br/>" +
               "<button type='submit'>Upload to Cloud</button>" +
               "</form>" +
               "<br><a href='/invoice' target='_blank'><button>Generate Monthly Bill</button></a>" +
               "</body></html>";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String folderPath = "D:/CloudStorage/"; 
            String filePath = folderPath + file.getOriginalFilename();
            file.transferTo(new File(filePath));
            
            double megabytes = (double) file.getSize() / (1024 * 1024); 
            
            FileMetadata metaData = new FileMetadata();
            metaData.setFileName(file.getOriginalFilename());
            metaData.setFileSizeMB(megabytes);
            repository.save(metaData); 
            
            return "<h3>✅ SUCCESS! File Uploaded.</h3>" +
                   "<a href='/'>Upload Another</a> | <a href='/invoice'>See Bill</a>";
            
        } catch (IOException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @GetMapping(value = "/invoice", produces = "text/html")
    public String generateInvoice() {
        List<FileMetadata> allFiles = repository.findAll(); 
        double totalSizeMB = 0;
        StringBuilder fileDetails = new StringBuilder();

        for (FileMetadata file : allFiles) {
            totalSizeMB += file.getFileSizeMB();
            // BUG FIX: Ab hum 'fileName' ki jagah file ki 'ID' pass kar rahe hain!
            fileDetails.append("<li style='margin-bottom: 10px;'>")
                       .append(file.getFileName())
                       .append(" : <b>").append(String.format("%.2f", file.getFileSizeMB())).append(" MB</b> ")
                       .append("<a href='/download/").append(file.getFileName()).append("'>[⬇️ Download]</a> ")
                       .append("<a href='/delete/").append(file.getId()).append("' style='color: red;'>[🗑️ Delete]</a>")
                       .append("</li>");
        }

        double totalBill = totalSizeMB * 15.0; 

        return "<html><body style='font-family: monospace; background-color: #f4f4f4; padding: 20px;'>" +
               "<div style='background: white; padding: 20px; border: 1px solid black; width: 550px;'>" +
               "<h2>🧾 ZOHO SETU - TAX INVOICE</h2><hr/>" +
               "<b>Customer:</b> System Admin<br/>" +
               "<b>Billing Cycle:</b> March 2026<br/><hr/>" +
               "<h3>Storage Details:</h3>" +
               "<ul>" + fileDetails.toString() + "</ul><hr/>" +
               "<p><b>Total Usage:</b> " + String.format("%.2f", totalSizeMB) + " MB</p>" +
               "<p><b>Rate:</b> ₹ 15.00 / MB</p>" +
               "<h2 style='color: green;'>TOTAL DUE: ₹ " + String.format("%.2f", totalBill) + "</h2>" +
               "</div></body></html>";
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("D:/CloudStorage/").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==========================================
    // BUG FIX: THE UPGRADED DELETE ENGINE
    // ==========================================
    @GetMapping(value = "/delete/{id}", produces = "text/html")
    public String deleteFile(@PathVariable Long id) {
        try {
            // 1. Database mein 'ID' se dhundo (100% unique)
            Optional<FileMetadata> optionalFile = repository.findById(id);
            
            if (optionalFile.isPresent()) {
                FileMetadata fileData = optionalFile.get();
                
                // 2. Database se entry udao
                repository.delete(fileData);
                
                // 3. Hard Drive se physically delete karo
                File physicalFile = new File("D:/CloudStorage/" + fileData.getFileName());
                if (physicalFile.exists()) {
                    physicalFile.delete();
                }
            }
            
            return "<script>window.location.href='/invoice';</script>";
            
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}