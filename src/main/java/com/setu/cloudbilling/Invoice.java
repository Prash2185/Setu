package com.setu.cloudbilling;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String invoiceNumber;
    private Double storageCost;
    private Double egressCost; // Bandwidth (Download) cost
    private Double totalAmount;
    private String status; // "PAID" ya "UNPAID"
    private LocalDateTime generatedAt;

    public Invoice() {}

    public Invoice(String username, String invoiceNumber, Double storageCost, Double egressCost, Double totalAmount, String status) {
        this.username = username;
        this.invoiceNumber = invoiceNumber;
        this.storageCost = storageCost;
        this.egressCost = egressCost;
        this.totalAmount = totalAmount;
        this.status = status;
        this.generatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public Double getStorageCost() { return storageCost; }
    public Double getEgressCost() { return egressCost; }
    public Double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
}