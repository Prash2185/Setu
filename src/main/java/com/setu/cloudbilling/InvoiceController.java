package com.setu.cloudbilling;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

@Controller
public class InvoiceController {

    @Autowired private FileMetadataRepository fileRepo;
    @Autowired private UserPlanRepository planRepo;

    @GetMapping("/download-invoice")
    public ResponseEntity<byte[]> downloadInvoice() {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        List<FileMetadata> myFiles = fileRepo.findByOwner(currentUser);
        UserPlan myPlan = planRepo.findByUsername(currentUser);

        double totalMB = myFiles.stream().mapToDouble(FileMetadata::getFileSizeMB).sum();
        
        if (myPlan == null) {
            myPlan = new UserPlan();
            myPlan.setPlanName("Free Tier (50 MB)");
            myPlan.setRatePerMB(0.0);
        }
        
        double totalBill = totalMB * myPlan.getRatePerMB();

        try {
            Document document = new Document(PageSize.A4, 40, 40, 50, 50);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            BaseColor themeColor = new BaseColor(13, 110, 253); 
            BaseColor lightGray = new BaseColor(230, 230, 230);
            BaseColor successGreen = new BaseColor(40, 167, 69);
            
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, themeColor);
            Font invoiceFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
            Font subTextFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.BLACK);
            Font whiteBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.WHITE);

            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new int[]{1, 1});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(new Paragraph("ZOHO SETU CLOUD", headerFont));
            leftCell.addElement(new Paragraph("Enterprise Storage Engine", subTextFont));
            leftCell.addElement(new Paragraph("Pune, Maharashtra, India", subTextFont));
            headerTable.addCell(leftCell);

            String invoiceNo = "INV-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            
            Paragraph invoiceText = new Paragraph("TAX INVOICE", invoiceFont);
            invoiceText.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(invoiceText);
            
            Paragraph invNoText = new Paragraph("Invoice #: " + invoiceNo, boldFont);
            invNoText.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(invNoText);
            
            Paragraph dateText = new Paragraph("Date: " + new SimpleDateFormat("dd MMM yyyy").format(new Date()), normalFont);
            dateText.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(dateText);
            
            Paragraph statusText = new Paragraph("Status: PAID", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, successGreen));
            statusText.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(statusText);

            headerTable.addCell(rightCell);
            document.add(headerTable);

            document.add(new Paragraph("\n"));
            LineSeparator ls = new LineSeparator();
            ls.setLineColor(themeColor);
            ls.setLineWidth(2);
            document.add(new Chunk(ls));
            document.add(new Paragraph("\n\n"));

            document.add(new Paragraph("BILLED TO:", subTextFont));
            document.add(new Paragraph(currentUser.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK)));
            document.add(new Paragraph("Active Subscription: " + myPlan.getPlanName(), normalFont));
            document.add(new Paragraph("Total Files Hosted: " + myFiles.size(), normalFont));
            document.add(new Paragraph("\n\n"));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.5f, 1.5f, 1.5f, 1.5f}); 

            String[] headers = {"Item Description", "Storage Used", "Rate (per MB)", "Amount"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, whiteBoldFont));
                cell.setBackgroundColor(themeColor);
                cell.setPadding(10);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBorderColor(BaseColor.WHITE);
                table.addCell(cell);
            }

            PdfPCell descCell = new PdfPCell(new Phrase("Cloud Data Storage \n(Auto-Scaling Cluster)", normalFont));
            descCell.setPadding(12); descCell.setBorderColor(lightGray);
            table.addCell(descCell);

            PdfPCell qtyCell = new PdfPCell(new Phrase(String.format("%.2f MB", totalMB), normalFont));
            qtyCell.setPadding(12); qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER); qtyCell.setBorderColor(lightGray);
            table.addCell(qtyCell);

            PdfPCell rateCell = new PdfPCell(new Phrase("₹" + myPlan.getRatePerMB(), normalFont));
            rateCell.setPadding(12); rateCell.setHorizontalAlignment(Element.ALIGN_CENTER); rateCell.setBorderColor(lightGray);
            table.addCell(rateCell);

            PdfPCell amtCell = new PdfPCell(new Phrase(String.format("₹%.4f", totalBill), normalFont));
            amtCell.setPadding(12); amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT); amtCell.setBorderColor(lightGray);
            table.addCell(amtCell);

            document.add(table);

            Paragraph totalLine = new Paragraph("Grand Total: ₹" + String.format("%.4f", totalBill), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, themeColor));
            totalLine.setAlignment(Element.ALIGN_RIGHT);
            totalLine.setSpacingBefore(20);
            document.add(totalLine);

            document.add(new Paragraph("\n\n\n\n"));
            LineSeparator ls2 = new LineSeparator();
            ls2.setLineColor(lightGray);
            document.add(new Chunk(ls2));
            
            Paragraph footer = new Paragraph("Thank you for choosing Zoho SETU.\nThis is a system generated invoice and does not require a physical signature.\nFor support, contact billing@setucloud.com", subTextFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10);
            document.add(footer);

            document.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.setContentDispositionFormData("attachment", "Zoho_SETU_Invoice_" + invoiceNo + ".pdf");

            return new ResponseEntity<>(out.toByteArray(), responseHeaders, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}