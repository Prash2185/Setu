package com.setu.cloudbilling;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UserPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String planName;
    private double maxStorageMB;
    private double ratePerMB;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public double getMaxStorageMB() { return maxStorageMB; }
    public void setMaxStorageMB(double maxStorageMB) { this.maxStorageMB = maxStorageMB; }
    public double getRatePerMB() { return ratePerMB; }
    public void setRatePerMB(double ratePerMB) { this.ratePerMB = ratePerMB; }
}